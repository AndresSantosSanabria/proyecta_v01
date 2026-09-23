package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoRequest;
import com.proyecta.api_gestion.service.IRiesgoTratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Tratamientos de Riesgo", description = "Endpoints para el historial iterativo de tratamientos de riesgos")
public class RiesgoTratamientoController {

    private final IRiesgoTratamientoService tratamientoService;

    public RiesgoTratamientoController(IRiesgoTratamientoService tratamientoService) {
        this.tratamientoService = tratamientoService;
    }

    @Operation(
        summary = "Listar historial de tratamientos de un riesgo",
        description = "Retorna todos los tratamientos registrados para un riesgo, ordenados del más reciente al más antiguo."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de tratamientos obtenida exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Riesgo no encontrado")
    })
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/tratamientos")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<RiesgoTratamientoDTO>>> listarTratamientos(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId) {
        List<RiesgoTratamientoDTO> tratamientos = tratamientoService.listarTratamientos(proyectoId, riesgoId);
        return ResponseEntity.ok(ApiResponse.success(tratamientos, "Tratamientos obtenidos exitosamente"));
    }

    @Operation(
        summary = "Crear un nuevo tratamiento para un riesgo",
        description = """
            Registra una nueva iteración de tratamiento para un riesgo.
            Cada tratamiento es independiente y contiene su comentario y archivos adjuntos (1-10 PDFs).
            Se asigna automáticamente el número de iteración siguiente.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tratamiento creado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o exceso de archivos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Riesgo no encontrado")
    })
    @PostMapping("/{proyectoId}/riesgos/{riesgoId}/tratamientos")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoTratamientoDTO>> crearTratamiento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @RequestPart("comentario") String comentario,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos,
            Authentication authentication) {

        RiesgoTratamientoRequest request = new RiesgoTratamientoRequest(comentario);
        MultipartFile[] archivosArray = archivos != null ? archivos.toArray(new MultipartFile[0]) : new MultipartFile[0];
        RiesgoTratamientoDTO resultado = tratamientoService.crearTratamiento(proyectoId, riesgoId, request, archivosArray);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Tratamiento registrado exitosamente"));
    }

    @Operation(
        summary = "Descargar un adjunto de un tratamiento específico",
        description = "Descarga un archivo PDF adjunto a un tratamiento específico de un riesgo."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Archivo descargado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Adjunto no encontrado")
    })
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/tratamientos/{tratamientoId}/adjuntos/{adjuntoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarAdjunto(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @Parameter(description = "ID del tratamiento") @PathVariable Long tratamientoId,
            @Parameter(description = "ID del adjunto") @PathVariable Long adjuntoId) {

        Resource resource = tratamientoService.descargarAdjunto(proyectoId, riesgoId, tratamientoId, adjuntoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tratamiento_" + tratamientoId + "_adjunto_" + adjuntoId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
