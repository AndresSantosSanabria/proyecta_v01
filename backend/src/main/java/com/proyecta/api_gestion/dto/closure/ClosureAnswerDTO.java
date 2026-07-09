package com.proyecta.api_gestion.dto.closure;

import java.time.LocalDateTime;

public record ClosureAnswerDTO(
    Long id,
    String proyectoId,
    Long questionId,
    String questionTexto,
    String respuesta,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
