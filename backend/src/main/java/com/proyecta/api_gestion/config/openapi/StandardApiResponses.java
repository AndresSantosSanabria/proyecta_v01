package com.proyecta.api_gestion.config.openapi;

import com.proyecta.api_gestion.dto.common.ProblemDetailDTO;
import com.proyecta.api_gestion.dto.common.UnauthorizedErrorDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.*;

/**
 * Anotación compuesta para reutilizar las respuestas estándar de error en Swagger.
 *
 * <p>Los esquemas y ejemplos coinciden con lo que la API devuelve realmente:</p>
 * <ul>
 *   <li>400/403/404/422/500 → {@link ProblemDetailDTO} (RFC 9457, serializado por
 *       {@code GlobalExceptionHandler});</li>
 *   <li>401 → {@link UnauthorizedErrorDTO} (formato de {@code JwtAuthenticationEntryPoint}).</li>
 * </ul>
 *
 * <p>Los errores por endpoint también se agregan automáticamente con
 * {@link ErrorResponseOperationCustomizer} (solo los códigos que aplican y sin pisar
 * los ya declarados), por lo que esta anotación es opcional pero explícita.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "400",
    description = DashboardSwaggerConstants.RESPONSE_400_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProblemDetailDTO.class),
        examples = @ExampleObject(
            name = "400 Bad Request",
            summary = "Error de validación",
            value = DashboardSwaggerConstants.EXAMPLE_400
        )
    )
)
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "401",
    description = DashboardSwaggerConstants.RESPONSE_401_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = UnauthorizedErrorDTO.class),
        examples = @ExampleObject(
            name = "401 Unauthorized",
            summary = "Token ausente, inválido o vencido",
            value = DashboardSwaggerConstants.EXAMPLE_401
        )
    )
)
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "403",
    description = DashboardSwaggerConstants.RESPONSE_403_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProblemDetailDTO.class),
        examples = @ExampleObject(
            name = "403 Forbidden",
            summary = "Sin privilegios para la acción",
            value = DashboardSwaggerConstants.EXAMPLE_403
        )
    )
)
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "404",
    description = DashboardSwaggerConstants.RESPONSE_404_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProblemDetailDTO.class),
        examples = @ExampleObject(
            name = "404 Not Found",
            summary = "Recurso no encontrado",
            value = DashboardSwaggerConstants.EXAMPLE_404
        )
    )
)
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "422",
    description = DashboardSwaggerConstants.RESPONSE_422_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProblemDetailDTO.class),
        examples = @ExampleObject(
            name = "422 Unprocessable Entity",
            summary = "Regla de negocio violada",
            value = DashboardSwaggerConstants.EXAMPLE_422
        )
    )
)
@io.swagger.v3.oas.annotations.responses.ApiResponse(
    responseCode = "500",
    description = DashboardSwaggerConstants.RESPONSE_500_DESC,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ProblemDetailDTO.class),
        examples = @ExampleObject(
            name = "500 Internal Server Error",
            summary = "Error interno",
            value = DashboardSwaggerConstants.EXAMPLE_500
        )
    )
)
public @interface StandardApiResponses {
}
