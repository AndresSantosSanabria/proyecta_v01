package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CambioFechaRequest(
    @NotNull LocalDate nuevaFecha,
    @NotBlank String justificacion
) {}
