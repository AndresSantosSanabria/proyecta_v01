package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;

public record ViabilidadDevolverDTO(
        @NotBlank(message = "Las observaciones son obligatorias al devolver la viabilidad.")
        String observaciones
) {
}
