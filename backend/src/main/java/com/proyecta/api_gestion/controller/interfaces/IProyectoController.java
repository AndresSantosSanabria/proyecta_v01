package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoUpdateDTO;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Gestión de Proyectos", description = "Endpoints para el ciclo de vida completo de los proyectos")
public interface IProyectoController {

    @Operation(
        summary = "Listar proyectos activos con avance mínimo",
        description = "Retorna una lista de proyectos cuyo estado es 'activo' y superan un umbral de avance."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "No se encontraron proyectos activos",
            content = @Content
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<List<Proyecto>>> getProyectosActivos(
            @Parameter(description = "Porcentaje de avance mínimo (0-100)", example = "10.5")
            @RequestParam BigDecimal minimo);

    @Operation(
        summary = "Obtener proyecto por ID",
        description = "Busca un proyecto por su identificador único (ej. IS-PROY-001)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Proyecto encontrado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<Proyecto>> getProyecto(
            @Parameter(description = "ID del proyecto", example = "IS-PROY-001")
            @PathVariable String id);

    @Operation(
        summary = "Crear nuevo proyecto",
        description = "Registra un nuevo proyecto en el sistema. Valida integridad de gestores y patrocinadores."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Proyecto creado exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<Proyecto>> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto);

    @Operation(
        summary = "Actualizar proyecto existente",
        description = "Actualiza los campos de un proyecto. Solo se modifican los campos enviados en el JSON."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Proyecto actualizado correctamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<Proyecto>> actualizarProyecto(
            @Parameter(description = "ID del proyecto a actualizar") @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto);

    @Operation(
        summary = "Eliminar proyecto",
        description = "Elimina físicamente un proyecto del sistema."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Proyecto eliminado con éxito",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<Void>> eliminarProyecto(@PathVariable String id);
}
