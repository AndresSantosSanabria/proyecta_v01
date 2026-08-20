package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.RespuestaFurag;

import java.util.Map;

public record FuragDTO(
    Map<String, RespuestaFurag> respuestas
) {}
