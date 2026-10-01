package com.proyecta.api_gestion.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO para respuestas de error estandarizadas.
 *
 * @deprecated La API NO devuelve este formato: los errores 4xx/5xx usan Problem Details
 * (RFC 9457) — ver {@link ProblemDetailDTO} — salvo el 401, que devuelve
 * {@link UnauthorizedErrorDTO}. Se conserva por compatibilidad; no usar en documentación.
 */
@Deprecated(forRemoval = false)
@Schema(description = "Respuesta de error de la API", example = """
    {"status":404,"error":"Not Found","message":"Proyecto no encontrado: PROY-CUN-2026-999","path":"/api/v1/proyectos/PROY-CUN-2026-999","timestamp":"2026-07-15T10:30:00"}
    """)
public class ApiErrorResponseDTO {

    @Schema(description = "Código de estado HTTP", example = "404")
    private int status;

    @Schema(description = "Nombre del error HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Mensaje detallado del error", example = "Proyecto no encontrado: PROY-001")
    private String message;

    @Schema(description = "Ruta del endpoint que generó el error", example = "/api/proyectos/PROY-001")
    private String path;

    @Schema(description = "Marca de tiempo del error", example = "2026-04-24T11:51:00")
    private LocalDateTime timestamp;

    public ApiErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponseDTO(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Getters and Setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
