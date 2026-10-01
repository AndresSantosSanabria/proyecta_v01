package com.proyecta.api_gestion.dto.closure;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":15,"texto":"¿Se entregaron todos los productos contratados?","tipoRespuesta":"SI_NO","opciones":["Sí","No"],"activo":true,"orden":1,"createdAt":"2026-07-15T10:30:00","updatedAt":"2026-07-15T10:30:00","createdBy":"admin@proyecta.gov.co","updatedBy":"admin@proyecta.gov.co"}
    """)
public record ClosureQuestionDTO(
    Long id,
    String texto,
    String tipoRespuesta,
    Object opciones,
    Boolean activo,
    Integer orden,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}
