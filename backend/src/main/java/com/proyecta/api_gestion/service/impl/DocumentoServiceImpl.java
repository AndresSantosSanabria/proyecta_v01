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
import com.proyecta.api_gestion.repository.DocumentoRepository;
import com.proyecta.api_gestion.repository.DocumentoDinamicoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.config.TipoDocumentoConfigRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
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
    private final IStorageProvider storageProvider;

    public DocumentoServiceImpl(
            DocumentoRepository documentoRepository,
            ProyectoRepository proyectoRepository,
            DocumentoDinamicoRepository documentoDinamicoRepository,
            TipoDocumentoConfigRepository tipoDocumentoConfigRepository,
            IStorageProvider storageProvider) {
        this.documentoRepository = documentoRepository;
        this.proyectoRepository = proyectoRepository;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.tipoDocumentoConfigRepository = tipoDocumentoConfigRepository;
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
    public DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo) {
        validarExistenciaProyecto(proyectoId);
        storageProvider.validateFile(archivo, MAX_FILE_SIZE, ALLOWED_MIME_TYPES);

        Documento documentoExistente = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElse(null);

        if (documentoExistente != null) {
            storageProvider.deleteFile(STORAGE_SUBDIR, documentoExistente.getNombreAlmacenado());
            documentoRepository.delete(documentoExistente);
        }

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = generarNombreUnico(tipoDocumento, extension);
        String mimeType = resolverMimeType(archivo);

        String nombreAlmacenado = storageProvider.storeFile(archivo, STORAGE_SUBDIR, nombreUnico);

        Documento documento = new Documento();
        documento.setProyectoId(proyectoId);
        documento.setTipoDocumentoConfig(tipoDocumentoConfigRepository.findByCodigo(tipoDocumento).orElse(null));
        documento.setNombreOriginal(nombreOriginal);
        documento.setNombreAlmacenado(nombreAlmacenado);
        documento.setRutaAlmacenamiento(STORAGE_SUBDIR);
        documento.setMimeType(mimeType);
        documento.setTamanoBytes(archivo.getSize());
        documento.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));

        Documento guardado = documentoRepository.save(documento);

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
    public void eliminarDocumento(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        Documento documento = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado: " + tipoDocumento + " para proyecto " + proyectoId));

        storageProvider.deleteFile(documento.getRutaAlmacenamiento(), documento.getNombreAlmacenado());
        documentoRepository.delete(documento);
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
}
