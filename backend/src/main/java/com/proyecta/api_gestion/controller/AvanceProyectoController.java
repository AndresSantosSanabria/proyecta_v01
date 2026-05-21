package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IAvanceProyectoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import java.time.LocalDate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
public class AvanceProyectoController implements IAvanceProyectoController {

    private final ProyectoAvanceService proyectoAvanceService;
    private final EntregableRepository entregableRepository;
    private final IStorageProvider storageProvider;

    public AvanceProyectoController(ProyectoAvanceService proyectoAvanceService,
                                    EntregableRepository entregableRepository,
                                    IStorageProvider storageProvider) {
        this.proyectoAvanceService = proyectoAvanceService;
        this.entregableRepository = entregableRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    @GetMapping("/{proyectoId}/avance")
    public ResponseEntity<ApiResponse<ProyectoAvanceResponseDTO>> getAvanceProyecto(
            @PathVariable String proyectoId) {
        ProyectoAvanceResponseDTO detalle = proyectoAvanceService.obtenerAvanceDetallado(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(detalle, "Avance del proyecto obtenido con éxito"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/avance/entregables/{entregableId}/completar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> marcarCompletado(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrega,
            @RequestPart("evidencia") MultipartFile evidencia) {

        EntregableConformidadResponseDTO result = proyectoAvanceService.actualizarConformidad(proyectoId, entregableId, true, fechaEntrega, evidencia);
        return ResponseEntity.ok(ApiResponse.success(result, "Entregable completado exitosamente. Documento registrado."));
    }

    @Override
    @PatchMapping(value = "/{proyectoId}/avance/entregables/{entregableId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> marcarConformidad(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam Boolean conformidad,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrega,
            @RequestPart MultipartFile evidencia) {

        EntregableConformidadResponseDTO result = proyectoAvanceService.actualizarConformidad(proyectoId, entregableId, conformidad, fechaEntrega, evidencia);
        return ResponseEntity.ok(ApiResponse.success(result, "Entregable marcado a conformidad exitosamente"));
    }

    @GetMapping("/{proyectoId}/avance/entregables/{entregableId}/evidencia")
    public ResponseEntity<Resource> descargarEvidencia(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        if (entregable.getArchivoPdf() == null) {
            throw new ResourceNotFoundException("El entregable no tiene documento de evidencia cargado");
        }

        Resource resource = storageProvider.loadFileAsResource("evidencias", entregable.getArchivoPdf());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
