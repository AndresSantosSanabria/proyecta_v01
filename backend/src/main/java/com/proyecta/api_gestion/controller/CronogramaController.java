package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.ICronogramaController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.cronograma.CronogramaResponseDTO;
import com.proyecta.api_gestion.dto.cronograma.CronogramaUploadResponseDTO;
import com.proyecta.api_gestion.service.interfaces.CronogramaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class CronogramaController implements ICronogramaController {

    private final CronogramaService cronogramaService;

    public CronogramaController(CronogramaService cronogramaService) {
        this.cronogramaService = cronogramaService;
    }

    @Override
    @GetMapping("/{proyectoId}/cronograma")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<CronogramaResponseDTO>> obtenerCronograma(@PathVariable String proyectoId) {
        CronogramaResponseDTO response = cronogramaService.obtenerCronograma(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cronograma obtenido exitosamente"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/cronograma", consumes = "multipart/form-data")
    @PreAuthorize("@proyectoSecurity.canAccess('CRONOGRAMA:CARGAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<CronogramaUploadResponseDTO>> cargarCronograma(
            @PathVariable String proyectoId, 
            @RequestPart("archivo") MultipartFile archivo) {
        CronogramaUploadResponseDTO response = cronogramaService.cargarCronograma(proyectoId, archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Archivo PDF subido y asociado correctamente"));
    }

    @Override
    @GetMapping(value = "/{proyectoId}/cronograma/descargar", produces = "application/pdf")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarCronograma(@PathVariable String proyectoId) {
        Resource resource = cronogramaService.descargarCronograma(proyectoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
