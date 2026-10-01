package com.proyecta.api_gestion.dto.closure;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":301,"proyectoId":"PROY-CUN-2026-008","questionId":15,"questionTexto":"¿Se entregaron todos los productos contratados?","respuesta":"Sí","createdAt":"2026-07-15T10:30:00","updatedAt":"2026-07-15T10:30:00"}
    """)
public record ClosureAnswerDTO(
    Long id,
    String proyectoId,
    Long questionId,
    String questionTexto,
    String respuesta,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
