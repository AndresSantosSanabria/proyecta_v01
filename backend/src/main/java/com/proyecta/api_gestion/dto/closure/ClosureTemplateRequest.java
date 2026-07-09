package com.proyecta.api_gestion.dto.closure;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClosureTemplateRequest(
        @NotBlank @JsonProperty("codigo_proceso") String codigoProceso,
        @NotBlank @JsonProperty("nombre_documento") String nombreDocumento,
        @NotNull @JsonProperty("template_json") Object templateJson
) {
}
