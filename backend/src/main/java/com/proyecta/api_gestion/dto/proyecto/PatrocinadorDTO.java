package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;

public record PatrocinadorDTO(
    @NotBlank String nombre,
    @NotBlank String entidad,
    @NotBlank String cargo,
    String procesoSigc,
    String procedimientoSigc
) {}
