package com.proyecta.api_gestion.dto.closure;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ProjectClosureRecordDTO(
        Long id,
        @JsonProperty("proyecto_id") String proyectoId,
        @JsonProperty("template_id") Long templateId,
        @JsonProperty("template_snapshot") Object templateSnapshot,
        @JsonProperty("form_data") Object formData,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("created_by") String createdBy
) {
}
