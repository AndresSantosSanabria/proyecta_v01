package com.proyecta.api_gestion.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Esquema documental del cuerpo de error Problem Details (RFC 9457) que devuelve
 * {@code GlobalExceptionHandler} para los códigos 400, 403, 404, 413, 422 y 500.
 *
 * <p>No es un DTO de negocio: existe para que el esquema registrado en Swagger
 * coincida exactamente con la respuesta real de la API
 * ({@code ProblemDetail} serializado con Jackson, sin campos nulos).</p>
 */
@Schema(
    name = "ProblemDetail",
    description = "Cuerpo de error Problem Details (RFC 9457) devuelto por la API",
    example = """
        {"type":"/errors/not-found","title":"Recurso no encontrado","status":404,"detail":"Proyecto no encontrado: PROY-CUN-2026-999"}
        """
)
public class ProblemDetailDTO {

    @Schema(
        description = "URI que identifica el tipo de problema",
        example = "/errors/not-found"
    )
    private String type;

    @Schema(
        description = "Título humano del problema (no usar para lógica; el campo canónico es status)",
        example = "Recurso no encontrado"
    )
    private String title;

    @Schema(
        description = "Código HTTP de estado",
        example = "404"
    )
    private int status;

    @Schema(
        description = "Explicación específica y segura del problema ocurrido",
        example = "Proyecto no encontrado: PROY-CUN-2026-999"
    )
    private String detail;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
