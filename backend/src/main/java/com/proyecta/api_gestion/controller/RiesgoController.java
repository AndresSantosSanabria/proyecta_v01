package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IRiesgoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoSolucionAdjuntoDTO;
import com.proyecta.api_gestion.service.IRiesgoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class RiesgoController implements IRiesgoController {

    private final IRiesgoService riesgoService;

    public RiesgoController(IRiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @Override
    @GetMapping("/{proyectoId}/riesgos")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoListResponseDTO>> listarRiesgos(@PathVariable String proyectoId) {
        RiesgoListResponseDTO riesgos = riesgoService.getRisksByProject(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Matriz de riesgos recuperada con exito"));
    }

    @GetMapping("/riesgos/matriz")
    @PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
    public ResponseEntity<ApiResponse<java.util.List<MatrizRiesgoDTO>>> listarMatrizRiesgos() {
        return ResponseEntity.ok(ApiResponse.success(riesgoService.getRiskMatrix(), "Matriz de riesgos configurada con exito"));
    }

    @GetMapping("/{proyectoId}/riesgos/descargar-excel")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarMatrizExcel(@PathVariable String proyectoId) {
        Resource resource = riesgoService.descargarMatrizExcel(proyectoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Matriz de Riesgos " + proyectoId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @Override
    @PostMapping("/{proyectoId}/riesgos")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoCreatedResponseDTO>> crearRiesgo(
            @PathVariable String proyectoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoCreatedResponseDTO response = riesgoService.createRisk(proyectoId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, response.mensaje()));
    }

    @Override
    @PutMapping("/{proyectoId}/riesgos/{riesgoId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> actualizarRiesgo(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoResponseDTO response = riesgoService.updateRisk(proyectoId, riesgoId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "Riesgo actualizado con exito"));
    }

    @Override
    @DeleteMapping("/{proyectoId}/riesgos/{riesgoId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<Void> eliminarRiesgo(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId) {
        riesgoService.deleteRisk(proyectoId, riesgoId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{proyectoId}/riesgos/{riesgoId}/tratamiento")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<Void>> verificarTratamiento(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @RequestBody String verificacion) {
        riesgoService.verificarTratamiento(proyectoId, riesgoId, verificacion);
        return ResponseEntity.ok(ApiResponse.success(null, "Tratamiento verificado y riesgo actualizado"));
    }

    @Override
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/soluciones")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<java.util.List<RiesgoSolucionAdjuntoDTO>>> listarSoluciones(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId) {
        return ResponseEntity.ok(ApiResponse.success(riesgoService.listarSoluciones(proyectoId, riesgoId), "Soluciones del riesgo obtenidas con exito"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/riesgos/{riesgoId}/soluciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<java.util.List<RiesgoSolucionAdjuntoDTO>>> agregarSoluciones(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @RequestPart("archivos") MultipartFile[] archivos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(riesgoService.agregarSoluciones(proyectoId, riesgoId, archivos), "Soluciones cargadas con exito"));
    }

    @Override
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/soluciones/{solucionId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarSolucion(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @PathVariable Long solucionId) {
        Resource resource = riesgoService.descargarSolucion(proyectoId, riesgoId, solucionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"solucion-riesgo-" + riesgoId + "-" + solucionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
