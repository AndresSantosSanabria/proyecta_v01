package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CierreForzosoDTO(
        @NotBlank(message = "El comentario es obligatorio para el cierre forzoso.")
        @Size(min = 10, max = 2000, message = "El comentario debe tener entre 10 y 2000 caracteres.")
        String comentario
) {
}
