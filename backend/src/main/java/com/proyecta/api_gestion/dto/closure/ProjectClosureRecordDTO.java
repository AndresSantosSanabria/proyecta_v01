package com.proyecta.api_gestion.dto.closure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":4201,"proyecto_id":"PROY-CUN-2026-008","template_id":3,"template_snapshot":{"codigo_proceso":"CIERRE_PROYECTO","version_num":2},"form_data":{"15":"Sí","16":"Cumplió con los plazos pactados"},"created_at":"2026-07-15T10:30:00","created_by":"maria.gomez@proyecta.gov.co"}
    """)
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
