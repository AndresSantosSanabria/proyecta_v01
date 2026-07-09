package com.proyecta.api_gestion.dto.closure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClosureQuestionRequest(
    @NotBlank(message = "El texto de la pregunta es obligatorio.")
    @Size(max = 500, message = "El texto no puede exceder 500 caracteres.")
    String texto,

    String tipoRespuesta,

    Object opciones,

    Boolean activo,

    Integer orden
) {}
