package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(example = """
    {"nombre":"Carlos Ramírez","cargo":"Director de TIC","procesoSigc":"PR-TIC-01","procedimientoSigc":"P-TIC-07"}
    """)
public record PatrocinadorDTO(
    @NotBlank String nombre,
    @NotBlank String cargo,
    String procesoSigc,
    String procedimientoSigc
) {}
