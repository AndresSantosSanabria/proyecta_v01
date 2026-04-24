package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Gestión de Proyectos", description = "Endpoints para consultar y gestionar el ciclo de vida de los proyectos")
public interface IProyectoController {

    @Operation(
        summary = "Listar proyectos activos con avance mínimo",
        description = "Retorna una lista de proyectos cuyo estado es 'activo'. Si no hay resultados, retorna 204 No Content."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "No se encontraron proyectos para los criterios especificados",
            content = @Content
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<List<Proyecto>>> getProyectosActivos(
            @Parameter(description = "Porcentaje de avance mínimo a filtrar (0 a 100)", example = "50.5")
            @RequestParam BigDecimal minimo);

    @Operation(
        summary = "Obtener proyecto por ID",
        description = "Busca un proyecto específico basado en su identificador manual (ej. IS-PROY-CUN-001). Retorna 404 si no existe."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Proyecto encontrado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "El proyecto solicitado no existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<Proyecto>> getProyecto(
            @Parameter(description = "ID único del proyecto", example = "IS-PROY-CUN-001")
            @PathVariable String id);
}
