package com.proyecta.api_gestion.dto.risk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiesgoTratamientoRequest(
        @NotBlank(message = "El comentario del tratamiento es obligatorio.")
        String comentario
) {}
