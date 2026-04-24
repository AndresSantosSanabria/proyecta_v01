package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.service.ProyectoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
@Tag(name = "Gestión de Proyectos", description = "Endpoints para consultar y gestionar el ciclo de vida de los proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @Operation(
        summary = "Listar proyectos activos con avance mínimo",
        description = "Retorna una lista de proyectos cuyo estado es 'activo'. Si no hay resultados, retorna una lista vacía con código 200 OK."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos obtenida exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Proyecto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetro de avance mínimo inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = com.proyecta.api_gestion.exception.ErrorResponse.class))
        )
    })
    @GetMapping("/activos-con-avance")
    public ResponseEntity<List<Proyecto>> getProyectosActivos(
            @Parameter(description = "Porcentaje de avance mínimo a filtrar (0 a 100)", example = "50.5")
            @RequestParam(defaultValue = "0") BigDecimal minimo) {
        List<Proyecto> resultados = proyectoService.obtenerProyectosActivosConAvance(minimo);
        return ResponseEntity.ok(resultados);
    }

    @Operation(
        summary = "Obtener proyecto por ID",
        description = "Busca un proyecto específico basado en su identificador manual (ej. IS-PROY-CUN-001). Retorna 404 si no existe."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Proyecto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "El proyecto solicitado no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = com.proyecta.api_gestion.exception.ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Proyecto> getProyecto(
            @Parameter(description = "ID único del proyecto", example = "IS-PROY-CUN-001")
            @PathVariable String id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }
}