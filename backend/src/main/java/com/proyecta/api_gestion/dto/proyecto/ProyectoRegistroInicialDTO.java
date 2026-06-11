package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProyectoRegistroInicialDTO(
        @Size(max = 30) String codigoProyecto,
        @NotBlank @Size(max = 300) String nombre,
        @NotBlank String objetivoGeneral,
        @NotNull Long directorUsuarioId
) {
}
