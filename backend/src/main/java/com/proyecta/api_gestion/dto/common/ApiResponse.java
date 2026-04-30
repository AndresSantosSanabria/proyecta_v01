package com.proyecta.api_gestion.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Envoltorio unificado para todas las respuestas exitosas de la API.
 * Proporciona una estructura consistente que facilita la integración con el frontend.
 *
 * @param <T> El tipo de dato que contiene la respuesta.
 */
@Schema(description = "Respuesta genérica de la API")
public class ApiResponse<T> {

    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;

    @Schema(description = "Mensaje informativo sobre el resultado de la operación", example = "Operación realizada con éxito")
    private String message;

    @Schema(description = "Marca de tiempo de la respuesta", example = "2026-04-29T12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Datos de la respuesta")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Crea una respuesta de éxito estándar (200 OK).
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Crea una respuesta de éxito para creación de recursos (201 Created).
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Crea una respuesta de éxito sin datos (ej. 204 No Content en concepto, aunque se envíe 200).
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public T getData() { return data; }
}
