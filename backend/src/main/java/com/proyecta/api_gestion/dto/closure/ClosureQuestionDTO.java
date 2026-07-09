package com.proyecta.api_gestion.dto.closure;

import java.time.LocalDateTime;

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
