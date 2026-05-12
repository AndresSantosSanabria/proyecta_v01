package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Cierre de Proyectos", description = "Endpoints para la ejecución formal del cierre de proyecto con generación de Acta")
public interface IProjectClosureController {

    @Operation(
        summary = "Ejecutar cierre formal del proyecto",
        description = """
            Cierra formalmente un proyecto generando un Acta de Cierre. Aplica las siguientes reglas de negocio:
            - Todos los hitos deben tener avance_calculado == 100%.
            - Todos los hitos deben tener estado_revision == 'APROBADO'.
            - El resumen_ejecutivo debe tener mínimo 100 caracteres.
            - El proyecto no debe estar ya cerrado.
            Persiste el registro en 'actas_cierre' y actualiza el campo 'cerrado' del proyecto.
            **Roles requeridos:** ADMINISTRADOR o GESTOR_TIC.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Proyecto cerrado exitosamente",
            content = @Content(schema = @Schema(implementation = CierreProyectoResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validación fallida: hitos incompletos o sin aprobación del gestor"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Acceso denegado: se requiere rol ADMINISTRADOR o GESTOR_TIC"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Proyecto no encontrado"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Datos de entrada inválidos (resumen ejecutivo muy corto)"
        )
    })
    @StandardApiResponses
    @PostMapping("/{id}/cierre")
    ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @Parameter(description = "ID del proyecto a cerrar", required = true) @PathVariable String id,
            @RequestBody CierreProyectoRequest request);
}
