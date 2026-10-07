package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;

public record CambioDescripcionRequest(
    @NotBlank String nuevaDescripcion,
    @NotBlank String justificacion
) {}