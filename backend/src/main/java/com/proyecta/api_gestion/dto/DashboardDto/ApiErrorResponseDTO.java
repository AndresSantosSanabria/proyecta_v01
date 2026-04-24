package com.proyecta.api_gestion.dto.DashboardDto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "DTO para respuestas de error estandarizadas")
public class ApiErrorResponseDTO {

    @Schema(description = "Código de estado HTTP")
    private int status;

    @Schema(description = "Nombre del error HTTP")
    private String error;

    @Schema(description = "Mensaje detallado del error")
    private String message;

    @Schema(description = "Ruta del endpoint que generó el error")
    private String path;

    @Schema(description = "Marca de tiempo del error")
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
