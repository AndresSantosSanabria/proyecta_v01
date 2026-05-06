package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.DocumentoItemDTO;
import com.proyecta.api_gestion.dto.document.DocumentoListResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResponseDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentoServiceImpl implements IDocumentoService {

    private final ProyectoRepository proyectoRepository;
    private final String UPLOAD_DIR = "uploads/proyectos/";

    public DocumentoServiceImpl(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @Override
    public DocumentoListResponseDTO listarDocumentos(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));

        List<DocumentoItemDTO> documentos = new ArrayList<>();
        
        documentos.add(buildDocumentoItem(proyecto, TipoDocumento.VIABILIZACION));
        documentos.add(buildDocumentoItem(proyecto, TipoDocumento.ACTA_CONSTITUCION));
        documentos.add(buildDocumentoItem(proyecto, TipoDocumento.CRONOGRAMA));
        if (Boolean.TRUE.equals(proyecto.getTienePlanComunicaciones())) {
            documentos.add(buildDocumentoItem(proyecto, TipoDocumento.PLAN_COMUNICACIONES));
        }

        return new DocumentoListResponseDTO(proyectoId, documentos);
    }

    private DocumentoItemDTO buildDocumentoItem(Proyecto proyecto, TipoDocumento tipo) {
        String fileName = getFileNameByTipo(proyecto, tipo);
        boolean cargado = fileName != null;
        LocalDate fechaCarga = cargado ? getFechaCarga(proyecto.getId(), fileName) : null;
        boolean requerido = isRequerido(tipo, proyecto);
        
        LocalDate fechaLimiteActa = null;
        Integer diasRestantes = null;
        
        if (tipo == TipoDocumento.ACTA_CONSTITUCION) {
            String viabFile = proyecto.getViabilizacionPdf();
            if (viabFile != null) {
                LocalDate viabFecha = getFechaCarga(proyecto.getId(), viabFile);
                if (viabFecha != null) {
                    fechaLimiteActa = viabFecha.plusMonths(6);
                    diasRestantes = (int) ChronoUnit.DAYS.between(LocalDate.now(), fechaLimiteActa);
                }
            }
        }

        String descargaUrl = cargado ? "/api/v1/proyectos/" + proyecto.getId() + "/documentos/" + tipo.name() + "/descargar" : null;

        return new DocumentoItemDTO(
                tipo,
                requerido,
                cargado,
                fileName,
                fechaCarga,
                fechaLimiteActa,
                diasRestantes,
                descargaUrl
        );
    }

    private boolean isRequerido(TipoDocumento tipo, Proyecto proyecto) {
        if (tipo == TipoDocumento.VIABILIZACION || tipo == TipoDocumento.ACTA_CONSTITUCION || tipo == TipoDocumento.CRONOGRAMA) {
            return true;
        }
        if (tipo == TipoDocumento.PLAN_COMUNICACIONES) {
            return Boolean.TRUE.equals(proyecto.getTienePlanComunicaciones());
        }
        return false;
    }

    private String getFileNameByTipo(Proyecto proyecto, TipoDocumento tipo) {
        return switch (tipo) {
            case VIABILIZACION -> proyecto.getViabilizacionPdf();
            case ACTA_CONSTITUCION -> proyecto.getActaConstitucionPdf();
            case CRONOGRAMA -> proyecto.getCronogramaPdf();
            case PLAN_COMUNICACIONES -> proyecto.getPlanComunicacionesPdf();
        };
    }

    private LocalDate getFechaCarga(String proyectoId, String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, proyectoId, fileName);
            if (Files.exists(filePath)) {
                return LocalDate.ofInstant(Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault());
            }
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    @Transactional
    public DocumentoUploadResponseDTO cargarDocumento(String proyectoId, TipoDocumento tipoDocumento, MultipartFile archivo) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));

        if (tipoDocumento == TipoDocumento.PLAN_COMUNICACIONES && !Boolean.TRUE.equals(proyecto.getTienePlanComunicaciones())) {
            throw new BadRequestException("El proyecto no requiere Plan de Comunicaciones.");
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacío");
        }
        
        if (!"application/pdf".equals(archivo.getContentType())) {
            throw new BadRequestException("Solo se permiten archivos PDF");
        }
        
        if (archivo.getSize() > 20 * 1024 * 1024) {
            throw new BadRequestException("El archivo excede el límite de 20MB");
        }

        try {
            Path projectDir = Paths.get(UPLOAD_DIR, proyectoId);
            if (!Files.exists(projectDir)) {
                Files.createDirectories(projectDir);
            }

            String oldFile = getFileNameByTipo(proyecto, tipoDocumento);
            if (oldFile != null) {
                Path oldPath = projectDir.resolve(oldFile);
                Files.deleteIfExists(oldPath);
            }

            String originalName = archivo.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".pdf";
            String newFileName = tipoDocumento.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            Path targetPath = projectDir.resolve(newFileName);
            Files.copy(archivo.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            switch (tipoDocumento) {
                case VIABILIZACION -> proyecto.setViabilizacionPdf(newFileName);
                case ACTA_CONSTITUCION -> proyecto.setActaConstitucionPdf(newFileName);
                case CRONOGRAMA -> proyecto.setCronogramaPdf(newFileName);
                case PLAN_COMUNICACIONES -> proyecto.setPlanComunicacionesPdf(newFileName);
            }
            proyectoRepository.save(proyecto);

            String descargaUrl = "/api/v1/proyectos/" + proyecto.getId() + "/documentos/" + tipoDocumento.name() + "/descargar";
            return new DocumentoUploadResponseDTO(
                    tipoDocumento,
                    newFileName,
                    LocalDate.now(),
                    descargaUrl
            );

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo", e);
        }
    }

    @Override
    public Resource descargarDocumento(String proyectoId, TipoDocumento tipoDocumento) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));

        String fileName = getFileNameByTipo(proyecto, tipoDocumento);
        if (fileName == null) {
            throw new ResourceNotFoundException("El documento no ha sido cargado");
        }

        try {
            Path filePath = Paths.get(UPLOAD_DIR, proyectoId, fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("El archivo no se encuentra o no es legible");
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error al leer el archivo");
        }
    }
}
