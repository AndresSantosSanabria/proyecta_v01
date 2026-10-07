package com.proyecta.api_gestion.dto.closure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":3,"codigo_proceso":"CIERRE_PROYECTO","version_num":2,"nombre_documento":"Acta de cierre de proyecto","activo":true,"template_json":{"secciones":[{"titulo":"Información general","preguntas":["¿Se entregaron todos los productos contratados?"]}]},"created_at":"2026-07-15T10:30:00","updated_at":"2026-07-15T10:30:00","created_by":"admin@proyecta.gov.co","updated_by":"admin@proyecta.gov.co"}
    """)
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
