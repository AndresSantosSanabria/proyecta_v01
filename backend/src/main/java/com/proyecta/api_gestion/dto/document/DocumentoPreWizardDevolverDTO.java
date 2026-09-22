package com.proyecta.api_gestion.dto.document;

import jakarta.validation.constraints.NotBlank;

public record DocumentoPreWizardDevolverDTO(
        @NotBlank(message = "Las observaciones son obligatorias al devolver el documento.")
        String observaciones
) {
}
