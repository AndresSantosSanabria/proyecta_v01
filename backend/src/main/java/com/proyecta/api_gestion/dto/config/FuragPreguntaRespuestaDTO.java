package com.proyecta.api_gestion.dto.config;

import com.proyecta.api_gestion.model.enums.RespuestaFurag;

public record FuragPreguntaRespuestaDTO(
        String key,
        String label,
        RespuestaFurag response
) {
}
