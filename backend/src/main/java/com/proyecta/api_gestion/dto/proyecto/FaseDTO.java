package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FaseDTO(
    @NotBlank String nombre,
    String descripcion,
    @NotNull @Min(1) @Max(100) Integer ponderacion,
    List<HitoDTO> hitos
) {}
