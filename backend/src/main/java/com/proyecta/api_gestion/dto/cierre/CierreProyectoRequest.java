package com.proyecta.api_gestion.dto.cierre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CierreProyectoRequest(
    @NotBlank(message = "El resumen ejecutivo es obligatorio.")
    @Size(min = 100, message = "El resumen ejecutivo debe tener al menos 100 caracteres.")
    String resumenEjecutivo
) {}
