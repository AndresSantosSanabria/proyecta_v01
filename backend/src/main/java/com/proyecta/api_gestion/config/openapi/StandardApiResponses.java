package com.proyecta.api_gestion.config.openapi;

import com.proyecta.api_gestion.dto.common.ApiErrorResponseDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.*;

/**
 * Anotación compuesta para reutilizar las respuestas estándar de error en Swagger.
 * Cada código HTTP tiene su propio ejemplo realista y coherente.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = DashboardSwaggerConstants.RESPONSE_400_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "400 Bad Request",
                summary = "Error de validación",
                value = DashboardSwaggerConstants.EXAMPLE_400
            )
        )
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = DashboardSwaggerConstants.RESPONSE_401_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "401 Unauthorized",
                summary = "Error de autenticación",
                value = DashboardSwaggerConstants.EXAMPLE_401
            )
        )
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = DashboardSwaggerConstants.RESPONSE_403_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "403 Forbidden",
                summary = "Error de autorización",
                value = DashboardSwaggerConstants.EXAMPLE_403
            )
        )
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = DashboardSwaggerConstants.RESPONSE_404_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "404 Not Found",
                summary = "Recurso no encontrado",
                value = DashboardSwaggerConstants.EXAMPLE_404
            )
        )
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = DashboardSwaggerConstants.RESPONSE_500_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "500 Internal Server Error",
                summary = "Error interno",
                value = DashboardSwaggerConstants.EXAMPLE_500
            )
        )
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = DashboardSwaggerConstants.RESPONSE_503_DESC,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseDTO.class),
            examples = @ExampleObject(
                name = "503 Service Unavailable",
                summary = "Servicio no disponible",
                value = DashboardSwaggerConstants.EXAMPLE_503
            )
        )
    )
})
public @interface StandardApiResponses {
}
