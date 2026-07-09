package com.proyecta.api_gestion.dto.closure;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ClosureTemplateDTO(
        Long id,
        @JsonProperty("codigo_proceso") String codigoProceso,
        @JsonProperty("version_num") Integer versionNum,
        @JsonProperty("nombre_documento") String nombreDocumento,
        Boolean activo,
        @JsonProperty("template_json") Object templateJson,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("created_by") String createdBy,
        @JsonProperty("updated_by") String updatedBy
) {
}
