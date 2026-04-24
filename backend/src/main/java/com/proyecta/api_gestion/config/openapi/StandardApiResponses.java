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
                summary = "Parámetros inválidos",
                value = "{\n  \"status\": 400,\n  \"error\": \"Bad Request\",\n  \"message\": \"El avance mínimo debe ser un valor entre 0 y 100\",\n  \"path\": \"/api/proyectos/activos-con-avance\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
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
                summary = "Token ausente o inválido",
                value = "{\n  \"status\": 401,\n  \"error\": \"Unauthorized\",\n  \"message\": \"Token de autenticación ausente o inválido\",\n  \"path\": \"/api/dashboard/summary\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
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
                summary = "Sin permisos",
                value = "{\n  \"status\": 403,\n  \"error\": \"Forbidden\",\n  \"message\": \"No tiene permisos para acceder a este recurso\",\n  \"path\": \"/api/dashboard/summary\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
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
                value = "{\n  \"status\": 404,\n  \"error\": \"Not Found\",\n  \"message\": \"Proyecto no encontrado: IS-PROY-001\",\n  \"path\": \"/api/proyectos/IS-PROY-001\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
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
                summary = "Error inesperado del servidor",
                value = "{\n  \"status\": 500,\n  \"error\": \"Internal Server Error\",\n  \"message\": \"Ha ocurrido un error inesperado al procesar la solicitud\",\n  \"path\": \"/api/dashboard/summary\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
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
                summary = "Base de datos no disponible",
                value = "{\n  \"status\": 503,\n  \"error\": \"Service Unavailable\",\n  \"message\": \"Servicio temporalmente no disponible (Base de datos o IO)\",\n  \"path\": \"/api/dashboard/summary\",\n  \"timestamp\": \"2026-04-24T12:00:00\"\n}"
            )
        )
    )
})
public @interface StandardApiResponses {
}
