package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.DocumentoInternoDTO;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDownload;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.InternalErrorException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.SortOrder;
import com.proyecta.api_gestion.domain.exception.UnauthorizedException;
import com.proyecta.api_gestion.domain.model.DocumentoInterno;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.DocumentoInternoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.IDocumentoInternoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DocumentoInternoServiceImpl implements IDocumentoInternoService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentoInternoServiceImpl.class);

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("application/pdf");
    private static final String STORAGE_SUBDIR = "documentos-internos";
    static final DateTimeFormatter CODIGO_SLOT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm");
    private static final int MAX_SLOT_RETRIES = 60;
    private static final int MAX_SAVE_RETRIES = 3;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    /** Lock fair: el segundo hilo espera al primero (FIFO) para asignar el siguiente slot de codigo. */
    private final ReentrantLock codigoLock = new ReentrantLock(true);

    private final DocumentoInternoRepositoryPort documentoInternoRepositoryPort;
    private final IStorageProvider storageProvider;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final KeycloakIdentityExtractor identityExtractor;

    /** Proxy transaccional de esta misma bean; null en tests unitarios sin contexto Spring. */
    private final DocumentoInternoServiceImpl self;

    private DocumentoInternoServiceImpl selfProxy() {
        return self != null ? self : this;
    }

    public DocumentoInternoServiceImpl(
            DocumentoInternoRepositoryPort documentoInternoRepositoryPort,
            IStorageProvider storageProvider,
            LocalUserAuthorizationService localUserAuthorizationService,
            KeycloakIdentityExtractor identityExtractor,
            @Lazy DocumentoInternoServiceImpl self) {
        this.documentoInternoRepositoryPort = documentoInternoRepositoryPort;
        this.storageProvider = storageProvider;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.identityExtractor = identityExtractor;
        this.self = self;
    }

    @Override
    @Transactional
    public DocumentoInternoDTO cargar(
            String nombre,
            String descripcion,
            LocalDate fechaCreacion,
            MultipartFile archivo,
            Authentication authentication) {

        if (nombre == null || nombre.isBlank()) {
            throw new BadRequestException("El nombre del documento es obligatorio.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio.");
        }
        storageProvider.validateFile(archivo, MAX_FILE_SIZE, ALLOWED_MIME_TYPES);
        if (!esPdfValido(archivo)) {
            throw new BadRequestException("El archivo cargado no es un PDF valido.");
        }

        Actor actor = resolveActor(authentication);
        String nombreLimpio = nombre.trim();
        String descripcionLimpia = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();
        LocalDate fecha = (fechaCreacion == null) ? LocalDate.now(ZoneId.systemDefault()) : fechaCreacion;

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = "doc_int_" + UUID.randomUUID().toString().replace("-", "") + extension;
        String mimeType = resolverMimeType(archivo);
        String nombreAlmacenado = storageProvider.storeFile(archivo, STORAGE_SUBDIR, nombreUnico);

        try {
            DocumentoInterno guardado = guardarConCodigoUnico(nombreLimpio, descripcionLimpia, fecha,
                    nombreOriginal, nombreAlmacenado, mimeType, archivo.getSize(), actor);
            return toDTO(guardado);
        } catch (Exception ex) {
            eliminarArchivoSilencioso(nombreAlmacenado);
            if (ex instanceof DataIntegrityViolationException) {
                throw new InternalErrorException("No se pudo asignar un codigo unico. Intente nuevamente.");
            }
            throw ex;
        }
    }

    @SuppressWarnings("java:S107")
    DocumentoInterno guardarConCodigoUnico(
            String nombreLimpio,
            String descripcionLimpia,
            LocalDate fecha,
            String nombreOriginal,
            String nombreAlmacenado,
            String mimeType,
            long tamanoBytes,
            Actor actor) {

        DataIntegrityViolationException lastViolation = null;
        for (int attempt = 0; attempt < MAX_SAVE_RETRIES; attempt++) {
            String codigo = asignarCodigoUnico();
            try {
                DocumentoInterno doc = new DocumentoInterno();
                doc.setCodigo(codigo);
                doc.setNombre(nombreLimpio);
                doc.setDescripcion(descripcionLimpia);
                doc.setFechaCreacion(fecha);
                doc.setNombreOriginal(nombreOriginal);
                doc.setNombreAlmacenado(nombreAlmacenado);
                doc.setRutaAlmacenamiento(STORAGE_SUBDIR);
                doc.setMimeType(mimeType);
                doc.setTamanoBytes(tamanoBytes);
                doc.setCreadoPor(actor.id());
                doc.setCreadoPorNombre(actor.nombre());
                return documentoInternoRepositoryPort.saveAndFlush(doc);
            } catch (DataIntegrityViolationException ex) {
                lastViolation = ex;
                logger.warn("Colision de codigo {} en intento {}", codigo, attempt + 1);
            }
        }
        throw lastViolation != null ? lastViolation
                : new InternalErrorException("No se pudo asignar un codigo unico. Intente nuevamente.");
    }

    private void eliminarArchivoSilencioso(String nombreAlmacenado) {
        try {
            storageProvider.deleteFile(STORAGE_SUBDIR, nombreAlmacenado);
        } catch (Exception ex) {
            logger.warn("No se pudo eliminar archivo huerfano {}: {}", nombreAlmacenado, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInternoDTO.Listado listar(String nombre, Integer anio, String descripcion, Integer page, Integer size) {
        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String nombrePattern = buildLikePattern(nombre);
        String descripcionPattern = buildLikePattern(descripcion);
        long total = documentoInternoRepositoryPort.contarConFiltros(nombrePattern, anio, descripcionPattern);
        PageQuery query = new PageQuery(pageIndex, pageSize, List.of(
                new SortOrder("fechaCreacion", false),
                new SortOrder("creadoEn", false)));
        List<DocumentoInternoDTO> documentos = documentoInternoRepositoryPort
                .buscarConFiltros(nombrePattern, anio, descripcionPattern, query)
                .stream()
                .map(this::toDTO)
                .toList();
        return new DocumentoInternoDTO.Listado(documentos, total, pageIndex, pageSize);
    }

    static String buildLikePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInternoDownload descargar(Long id) {
        DocumentoInterno doc = obtener(id);
        Resource resource = storageProvider.loadFileAsResource(doc.getRutaAlmacenamiento(), doc.getNombreAlmacenado());
        if (resource == null || !resource.exists()) {
            throw new ResourceNotFoundException("Archivo no encontrado para el documento interno " + id);
        }
        String fallbackFilename = resource.getFilename() != null ? resource.getFilename() : "documento";
        String filename = doc.getNombreOriginal() != null && !doc.getNombreOriginal().isBlank()
                ? doc.getNombreOriginal()
                : fallbackFilename;
        return new DocumentoInternoDownload(resource, filename, doc.getMimeType());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInternoDownload ver(Long id) {
        return selfProxy().descargar(id);
    }

    /**
     * Genera DOC-ANIO-MES-DIA-HH:MM con lock fair (FIFO).
     * Si el slot de minuto actual ya esta ocupado, el siguiente hilo espera la liberacion
     * del lock y asigna el siguiente minuto libre.
     */
    String asignarCodigoUnico() {
        codigoLock.lock();
        try {
            LocalDateTime slot = LocalDateTime.now(ZoneId.systemDefault());
            for (int i = 0; i < MAX_SLOT_RETRIES; i++) {
                String codigo = formatCodigo(slot);
                if (!documentoInternoRepositoryPort.existsByCodigo(codigo)) {
                    return codigo;
                }
                slot = slot.plusMinutes(1);
            }
            throw new InternalErrorException("No hay slots de codigo disponibles. Intente mas tarde.");
        } finally {
            codigoLock.unlock();
        }
    }

    static String formatCodigo(LocalDateTime slot) {
        return "DOC-" + slot.format(CODIGO_SLOT);
    }

    private DocumentoInterno obtener(Long id) {
        return documentoInternoRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento interno no encontrado: " + id));
    }

    /**
     * Autor del documento: {@code id} para la FK {@code documento_interno.creado_por}
     * (usuarios.id) y {@code nombre} visible para {@code creado_por_nombre}.
     */
    record Actor(Long id, String nombre) {
    }

    private Actor resolveActor(Authentication authentication) {
        SeguridadUsuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
        String nombreVisible = firstNonBlank(usuario.getNombre(), usuario.getCorreo(), identityExtractor.resolveUsername(authentication));
        if (nombreVisible == null) {
            throw new UnauthorizedException("No fue posible identificar el usuario que carga el documento.");
        }
        return new Actor(usuario.getId(), nombreVisible);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static String extraerExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx) : "";
    }

    private boolean esPdfValido(MultipartFile file) {
        return com.proyecta.api_gestion.infrastructure.UploadMimeSanitizer.esPdfValido(file);
    }

    private String resolverMimeType(MultipartFile file) {
        // CWE-434: el Content-Type lo declara el cliente; se deriva de la extension.
        return com.proyecta.api_gestion.infrastructure.UploadMimeSanitizer.resolverMimeType(file);
    }

    private DocumentoInternoDTO toDTO(DocumentoInterno doc) {
        return new DocumentoInternoDTO(
                doc.getId(),
                doc.getCodigo(),
                doc.getNombre(),
                doc.getDescripcion(),
                doc.getFechaCreacion() != null ? doc.getFechaCreacion().toString() : null,
                doc.getNombreOriginal(),
                doc.getMimeType(),
                doc.getTamanoBytes(),
                formatearTamano(doc.getTamanoBytes()),
                doc.getCreadoPorNombre(),
                doc.getCreadoPor(),
                doc.getCreadoEn() != null ? doc.getCreadoEn().toString() : null
        );
    }

    private String formatearTamano(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
