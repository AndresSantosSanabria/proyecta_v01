package com.proyecta.api_gestion.dto.risk;

import jakarta.validation.constraints.NotBlank;

public record RiesgoTratamientoRequest(
        @NotBlank(message = "El comentario del tratamiento es obligatorio.")
        String comentario
) {}
