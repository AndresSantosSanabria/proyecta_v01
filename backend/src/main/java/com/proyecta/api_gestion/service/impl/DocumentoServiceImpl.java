package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.*;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Documento;
import com.proyecta.api_gestion.model.DocumentoProyectoVersion;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.config.TipoDocumentoConfigRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentoServiceImpl implements IDocumentoService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final String STORAGE_SUBDIR = "documentos";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DocumentoRepository documentoRepository;
    private final DocumentoProyectoVersionRepository versionRepository;
    private final ProyectoRepository proyectoRepository;
    private final DocumentoDinamicoRepository documentoDinamicoRepository;
    private final TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final IStorageProvider storageProvider;

    public DocumentoServiceImpl(
            DocumentoRepository documentoRepository,
            DocumentoProyectoVersionRepository versionRepository,
            ProyectoRepository proyectoRepository,
            DocumentoDinamicoRepository documentoDinamicoRepository,
            TipoDocumentoConfigRepository tipoDocumentoConfigRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
            NotificationEventPublisherPort notificationPublisher,
            KeycloakIdentityExtractor identityExtractor,
            LocalUserAuthorizationService localUserAuthorizationService,
            IStorageProvider storageProvider) {
        this.documentoRepository = documentoRepository;
        this.versionRepository = versionRepository;
        this.proyectoRepository = proyectoRepository;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.tipoDocumentoConfigRepository = tipoDocumentoConfigRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.storageProvider = storageProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoListadoResponseDTO listarDocumentos(String proyectoId) {
        validarExistenciaProyecto(proyectoId);

        List<Documento> documentos = documentoRepository.findByProyectoIdOrderByFechaCargaDesc(proyectoId);
        List<DocumentoDetailDTO> detalles = documentos.stream()
                .map(this::toDetailDTO)
                .toList();

        return new DocumentoListadoResponseDTO(proyectoId, detalles);
    }

    @Override
    @Transactional
    public DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo, String observacion, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio.");
        }
        tipoDocumento = tipoDocumento.trim().toUpperCase();
        storageProvider.validateFile(archivo, MAX_FILE_SIZE, ALLOWED_MIME_TYPES);
        ActorContext actor = actorContext(authentication);

        DocumentoProyectoVersion versionActual = versionRepository
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipoDocumento, DocumentoProyectoVersionEstado.ACTUAL)
                .orElse(null);
        boolean esReemplazo = versionActual != null;

        if (esReemplazo) {
            if (observacion == null || observacion.isBlank()) {
                throw new BadRequestException("La observacion es obligatoria al reemplazar un documento.");
            }
            observacion = observacion.trim();
            versionActual.setEstado(DocumentoProyectoVersionEstado.HISTORICA);
            versionRepository.save(versionActual);
        }

        Integer maxVersion = versionRepository.findMaxNumeroVersion(proyectoId, tipoDocumento);
        Integer nuevaVersion = maxVersion + 1;

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = generarNombreUnico(tipoDocumento, extension);
        String mimeType = resolverMimeType(archivo);

        String nombreAlmacenado = storageProvider.storeFile(archivo, STORAGE_SUBDIR, nombreUnico);

        DocumentoProyectoVersion version = new DocumentoProyectoVersion();
        version.setProyectoId(proyectoId);
        version.setTipoDocumento(tipoDocumento);
        version.setNumeroVersion(nuevaVersion);
        version.setNombreArchivoOriginal(nombreOriginal);
        version.setNombreAlmacenado(nombreAlmacenado);
        version.setRutaAlmacenamiento(STORAGE_SUBDIR);
        version.setMimeType(mimeType);
        version.setTamanoBytes(archivo.getSize());
        version.setObservacion(esReemplazo ? observacion : null);
        version.setEstado(DocumentoProyectoVersionEstado.ACTUAL);
        version.setSubidoPor(actor.username());
        version.setSubidoRol(actor.role());

        DocumentoProyectoVersion guardada = versionRepository.save(version);

        sincronizarDocumentoPrincipal(proyectoId, tipoDocumento, guardada);

        notificarCambioDocumento(proyectoId, actor.username(), NotificationEventType.PROJECT_DOCUMENT_UPLOADED, tipoDocumento, nombreOriginal, esReemplazo);

        return toUploadResultDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarDocumento(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        DocumentoProyectoVersion version = versionRepository
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipoDocumento, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado: " + tipoDocumento + " para proyecto " + proyectoId));

        return storageProvider.loadFileAsResource(version.getRutaAlmacenamiento(), version.getNombreAlmacenado());
    }

    @Override
    @Transactional
    public void eliminarDocumento(String proyectoId, String tipoDocumento, Authentication authentication) {
        throw new UnsupportedOperationException(
                "La eliminacion de documentos no esta habilitada. Use el reemplazo con observacion para crear nuevas versiones.");
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoVersionHistorialResponseDTO listarVersiones(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        List<DocumentoProyectoVersion> versiones = versionRepository
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyectoId, tipoDocumento);

        List<DocumentoProyectoVersionDTO> dtos = versiones.stream()
                .map(this::toVersionDTO)
                .toList();

        return new DocumentoVersionHistorialResponseDTO(proyectoId, tipoDocumento, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarVersion(String proyectoId, String tipoDocumento, Integer numeroVersion) {
        validarExistenciaProyecto(proyectoId);

        DocumentoProyectoVersion version = versionRepository
                .findByProyectoIdAndTipoDocumentoAndNumeroVersion(proyectoId, tipoDocumento, numeroVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + numeroVersion + " del documento " + tipoDocumento + " no encontrada."));

        return storageProvider.loadFileAsResource(version.getRutaAlmacenamiento(), version.getNombreAlmacenado());
    }

    private void sincronizarDocumentoPrincipal(String proyectoId, String tipoDocumento, DocumentoProyectoVersion version) {
        TipoDocumento tipoDocumentoEnum = resolverTipoDocumento(tipoDocumento);
        TipoDocumentoConfig tipoDocumentoConfig = tipoDocumentoConfigRepository.findByCodigo(tipoDocumento)
                .orElse(null);

        Documento documentoExistente = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElse(null);

        if (documentoExistente != null) {
            // ELIMINADO: storageProvider.deleteFile(STORAGE_SUBDIR, documentoExistente.getNombreAlmacenado());
            // No podemos borrar el archivo físico porque la version anterior (histórica) en `DocumentoProyectoVersion`
            // lo sigue referenciando y es necesario para que funcione el visor de historial.
            documentoExistente.setNombreOriginal(version.getNombreArchivoOriginal());
            documentoExistente.setNombreAlmacenado(version.getNombreAlmacenado());
            documentoExistente.setRutaAlmacenamiento(version.getRutaAlmacenamiento());
            documentoExistente.setMimeType(version.getMimeType());
            documentoExistente.setTamanoBytes(version.getTamanoBytes());
            documentoExistente.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));
            documentoRepository.save(documentoExistente);
        } else {
            Documento documento = new Documento();
            documento.setProyectoId(proyectoId);
            documento.setTipoDocumento(tipoDocumentoEnum);
            documento.setTipoDocumentoConfig(tipoDocumentoConfig);
            documento.setNombreOriginal(version.getNombreArchivoOriginal());
            documento.setNombreAlmacenado(version.getNombreAlmacenado());
            documento.setRutaAlmacenamiento(version.getRutaAlmacenamiento());
            documento.setMimeType(version.getMimeType());
            documento.setTamanoBytes(version.getTamanoBytes());
            documento.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));
            documentoRepository.save(documento);
        }
    }

    private void validarExistenciaProyecto(String proyectoId) {
        proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));
    }

    private String extraerExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex).toLowerCase() : "";
    }

    private String generarNombreUnico(String tipoDocumento, String extension) {
        return tipoDocumento.toLowerCase() + "_" + UUID.randomUUID().toString();
    }

    private String resolverMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            return contentType.toLowerCase();
        }
        String ext = extraerExtension(file.getOriginalFilename()).toLowerCase();
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private String construirUrlDescarga(String proyectoId, String tipoDocumento) {
        return "/api/v1/proyectos/" + proyectoId + "/documentos/" + tipoDocumento + "/descargar";
    }

    private TipoDocumento resolverTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio.");
        }
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de documento no válido: " + tipoDocumento);
        }
    }

    private String formatearTamano(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private DocumentoDetailDTO toDetailDTO(Documento doc) {
        return new DocumentoDetailDTO(
                doc.getId(),
                doc.getTipoDocumentoCodigo(),
                doc.getNombreOriginal(),
                doc.getMimeType(),
                doc.getTamanoBytes(),
                formatearTamano(doc.getTamanoBytes()),
                doc.getUrlDescarga(),
                doc.getFechaCarga().format(DATE_FORMATTER)
        );
    }

    private DocumentoUploadResultDTO toUploadResultDTO(DocumentoProyectoVersion version) {
        String urlDescarga = construirUrlDescarga(version.getProyectoId(), version.getTipoDocumento());
        return new DocumentoUploadResultDTO(
                version.getId(),
                version.getTipoDocumento(),
                version.getNombreArchivoOriginal(),
                version.getNombreAlmacenado(),
                version.getMimeType(),
                version.getTamanoBytes(),
                formatearTamano(version.getTamanoBytes()),
                urlDescarga,
                version.getSubidoEn().format(DATE_FORMATTER)
        );
    }

    private DocumentoProyectoVersionDTO toVersionDTO(DocumentoProyectoVersion v) {
        String urlDescarga = construirUrlDescarga(v.getProyectoId(), v.getTipoDocumento()) + "?version=" + v.getNumeroVersion();
        return new DocumentoProyectoVersionDTO(
                v.getId(),
                v.getTipoDocumento(),
                v.getNumeroVersion(),
                v.getNombreArchivoOriginal(),
                v.getMimeType(),
                v.getTamanoBytes(),
                formatearTamano(v.getTamanoBytes()),
                v.getObservacion(),
                v.getEstado().name(),
                v.getSubidoPor(),
                v.getSubidoRol(),
                v.getSubidoEn().format(DATE_FORMATTER),
                v.getEstado() == DocumentoProyectoVersionEstado.ACTUAL
        );
    }

    private void notificarCambioDocumento(String proyectoId, String actorUsername, NotificationEventType eventType, String tipoDocumento, String nombreDocumento, boolean reemplazo) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }

        var recipients = usuarioProyectoRepository.findActivasByProyectoId(proyectoId).stream()
                .map(item -> item.getUsuario())
                .filter(usuario -> usuario != null)
                .map(usuario -> usuario.getUsername())
                .filter(username -> username != null && !username.isBlank())
                .filter(username -> actorUsername == null || !username.equalsIgnoreCase(actorUsername))
                .distinct()
                .toList();

        if (recipients.isEmpty()) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                eventType,
                proyectoId,
                actorUsername,
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "documentType", tipoDocumento,
                        "documentName", nombreDocumento,
                        "isReplacement", reemplazo,
                        "recipients", recipients
                )));
    }

    private ActorContext actorContext(Authentication authentication) {
        try {
            Usuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
            String username = firstNonBlank(usuario.getCorreo(), usuario.getNombre(), identityExtractor.resolveUsername(authentication), "sistema");
            String role = firstNonBlank(SecurityRoleCatalog.normalize(usuario.getRolCodigo()), "sin_rol");
            return new ActorContext(username, role);
        } catch (RuntimeException ex) {
            return new ActorContext(firstNonBlank(identityExtractor.resolveUsername(authentication), "sistema"), "sin_rol");
        }
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

    private record ActorContext(String username, String role) {}
}
