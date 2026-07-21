package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.DocumentoDetailDTO;
import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Documento;
import com.proyecta.api_gestion.model.DocumentoDinamico;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import com.proyecta.api_gestion.repository.DocumentoRepository;
import com.proyecta.api_gestion.repository.DocumentoDinamicoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.config.TipoDocumentoConfigRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
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
    private final ProyectoRepository proyectoRepository;
    private final DocumentoDinamicoRepository documentoDinamicoRepository;
    private final TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final IStorageProvider storageProvider;

    public DocumentoServiceImpl(
            DocumentoRepository documentoRepository,
            ProyectoRepository proyectoRepository,
            DocumentoDinamicoRepository documentoDinamicoRepository,
            TipoDocumentoConfigRepository tipoDocumentoConfigRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
            NotificationEventPublisherPort notificationPublisher,
            KeycloakIdentityExtractor identityExtractor,
            IStorageProvider storageProvider) {
        this.documentoRepository = documentoRepository;
        this.proyectoRepository = proyectoRepository;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.tipoDocumentoConfigRepository = tipoDocumentoConfigRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
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
    public DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        storageProvider.validateFile(archivo, MAX_FILE_SIZE, ALLOWED_MIME_TYPES);
        String actorUsername = identityExtractor.resolveUsername(authentication);

        Documento documentoExistente = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElse(null);
        boolean reemplazo = documentoExistente != null;

        if (documentoExistente != null) {
            storageProvider.deleteFile(STORAGE_SUBDIR, documentoExistente.getNombreAlmacenado());
            documentoRepository.delete(documentoExistente);
        }

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = generarNombreUnico(tipoDocumento, extension);
        String mimeType = resolverMimeType(archivo);

        String nombreAlmacenado = storageProvider.storeFile(archivo, STORAGE_SUBDIR, nombreUnico);
        TipoDocumento tipoDocumentoEnum = resolverTipoDocumento(tipoDocumento);
        TipoDocumentoConfig tipoDocumentoConfig = tipoDocumentoConfigRepository.findByCodigo(tipoDocumento)
                .orElseThrow(() -> new BadRequestException("Tipo de documento no configurado: " + tipoDocumento));

        Documento documento = new Documento();
        documento.setProyectoId(proyectoId);
        documento.setTipoDocumento(tipoDocumentoEnum);
        documento.setTipoDocumentoConfig(tipoDocumentoConfig);
        documento.setNombreOriginal(nombreOriginal);
        documento.setNombreAlmacenado(nombreAlmacenado);
        documento.setRutaAlmacenamiento(STORAGE_SUBDIR);
        documento.setMimeType(mimeType);
        documento.setTamanoBytes(archivo.getSize());
        documento.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));

        Documento guardado = documentoRepository.save(documento);
        notificarCambioDocumento(proyectoId, actorUsername, NotificationEventType.PROJECT_DOCUMENT_UPLOADED, guardado, reemplazo);

        return toUploadResultDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarDocumento(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        Documento documento = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado: " + tipoDocumento + " para proyecto " + proyectoId));

        return storageProvider.loadFileAsResource(documento.getRutaAlmacenamiento(), documento.getNombreAlmacenado());
    }

    @Override
    @Transactional
    public void eliminarDocumento(String proyectoId, String tipoDocumento, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        String actorUsername = identityExtractor.resolveUsername(authentication);

        Documento documento = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado: " + tipoDocumento + " para proyecto " + proyectoId));

        storageProvider.deleteFile(documento.getRutaAlmacenamiento(), documento.getNombreAlmacenado());
        documentoRepository.delete(documento);
        notificarCambioDocumento(proyectoId, actorUsername, NotificationEventType.PROJECT_DOCUMENT_DELETED, documento, false);
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

    private DocumentoUploadResultDTO toUploadResultDTO(Documento doc) {
        return new DocumentoUploadResultDTO(
                doc.getId(),
                doc.getTipoDocumentoCodigo(),
                doc.getNombreOriginal(),
                doc.getNombreAlmacenado(),
                doc.getMimeType(),
                doc.getTamanoBytes(),
                formatearTamano(doc.getTamanoBytes()),
                doc.getUrlDescarga(),
                doc.getFechaCarga().format(DATE_FORMATTER)
        );
    }

    private void notificarCambioDocumento(String proyectoId, String actorUsername, NotificationEventType eventType, Documento documento, boolean reemplazo) {
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
                        "documentType", documento.getTipoDocumentoCodigo(),
                        "documentName", documento.getNombreOriginal(),
                        "isReplacement", reemplazo,
                        "recipients", recipients
                )));
    }
}
