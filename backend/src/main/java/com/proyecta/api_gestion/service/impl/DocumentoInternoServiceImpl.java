package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.DocumentoInternoDTO;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDownload;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.InternalErrorException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnauthorizedException;
import com.proyecta.api_gestion.model.DocumentoInterno;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.DocumentoInternoRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoInternoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final DocumentoInternoRepository documentoInternoRepository;
    private final IStorageProvider storageProvider;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final KeycloakIdentityExtractor identityExtractor;

    public DocumentoInternoServiceImpl(
            DocumentoInternoRepository documentoInternoRepository,
            IStorageProvider storageProvider,
            LocalUserAuthorizationService localUserAuthorizationService,
            KeycloakIdentityExtractor identityExtractor) {
        this.documentoInternoRepository = documentoInternoRepository;
        this.storageProvider = storageProvider;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.identityExtractor = identityExtractor;
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

        String actor = resolveActor(authentication);
        String nombreLimpio = nombre.trim();
        String descripcionLimpia = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();
        LocalDate fecha = (fechaCreacion == null) ? LocalDate.now() : fechaCreacion;

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

    DocumentoInterno guardarConCodigoUnico(
            String nombreLimpio,
            String descripcionLimpia,
            LocalDate fecha,
            String nombreOriginal,
            String nombreAlmacenado,
            String mimeType,
            long tamanoBytes,
            String actor) {

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
                doc.setCreadoPor(actor);
                return documentoInternoRepository.saveAndFlush(doc);
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
        long total = documentoInternoRepository.contarConFiltros(nombrePattern, anio, descripcionPattern);
        PageRequest pageable = PageRequest.of(pageIndex, pageSize,
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("creadoEn")));
        List<DocumentoInternoDTO> documentos = documentoInternoRepository
                .buscarConFiltros(nombrePattern, anio, descripcionPattern, pageable)
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
        String filename = doc.getNombreOriginal() != null && !doc.getNombreOriginal().isBlank()
                ? doc.getNombreOriginal()
                : (resource.getFilename() != null ? resource.getFilename() : "documento");
        return new DocumentoInternoDownload(resource, filename, doc.getMimeType());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInternoDownload ver(Long id) {
        return descargar(id);
    }

    /**
     * Genera DOC-ANIO-MES-DIA-HH:MM con lock fair (FIFO).
     * Si el slot de minuto actual ya esta ocupado, el siguiente hilo espera la liberacion
     * del lock y asigna el siguiente minuto libre.
     */
    String asignarCodigoUnico() {
        codigoLock.lock();
        try {
            LocalDateTime slot = LocalDateTime.now();
            for (int i = 0; i < MAX_SLOT_RETRIES; i++) {
                String codigo = formatCodigo(slot);
                if (!documentoInternoRepository.existsByCodigo(codigo)) {
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
        return documentoInternoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento interno no encontrado: " + id));
    }

    private String resolveActor(Authentication authentication) {
        SeguridadUsuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
        String username = firstNonBlank(usuario.getNombre(), usuario.getCorreo(), identityExtractor.resolveUsername(authentication));
        if (username == null) {
            throw new UnauthorizedException("No fue posible identificar el usuario que carga el documento.");
        }
        return username;
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
        try (var in = file.getInputStream()) {
            byte[] header = in.readNBytes(5);
            String signature = new String(header, java.nio.charset.StandardCharsets.ISO_8859_1);
            return signature.startsWith("%PDF-");
        } catch (java.io.IOException ex) {
            return false;
        }
    }

    private String resolverMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
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
