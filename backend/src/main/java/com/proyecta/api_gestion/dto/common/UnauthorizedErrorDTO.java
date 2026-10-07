package com.proyecta.api_gestion.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Esquema documental de la respuesta 401 que emite {@code JwtAuthenticationEntryPoint}
 * cuando el token está ausente, es inválido o venció.
 *
 * <p>Este es el único 401 real de la API; no usa el formato Problem Details.</p>
 */
@Schema(
    name = "UnauthorizedError",
    description = "Respuesta 401 emitida por el punto de entrada de autenticación JWT",
    example = """
        {"status":401,"error":"Unauthorized","message":"Token de acceso ausente, inválido o vencido. Renueve el token o inicie sesión nuevamente.","path":"/api/v1/proyectos/PROY-CUN-2026-008","timestamp":"2026-04-29T12:00:00","action":"REFRESH_OR_LOGIN"}
        """
)
public class UnauthorizedErrorDTO {

    @Schema(description = "Código de estado HTTP", example = "401")
    private int status;

    @Schema(description = "Nombre del error HTTP", example = "Unauthorized")
    private String error;

    @Schema(
        description = "Mensaje orientado al usuario sobre la causa de la autenticación fallida",
        example = "Token de acceso ausente, inválido o vencido. Renueve el token o inicie sesión nuevamente."
    )
    private String message;

    @Schema(
        description = "Ruta del endpoint que rechazó la petición",
        example = "/api/v1/proyectos/PROY-CUN-2026-008"
    )
    private String path;

    @Schema(
        description = "Marca de tiempo del rechazo (ISO-8601, sin zona horaria)",
        example = "2026-04-29T12:00:00"
    )
    private String timestamp;

    @Schema(
        description = "Acción sugerida al cliente",
        example = "REFRESH_OR_LOGIN"
    )
    private String action;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
