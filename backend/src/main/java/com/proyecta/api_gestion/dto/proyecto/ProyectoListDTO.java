package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.dto.config.FuragPreguntaRespuestaDTO;

import java.math.BigDecimal;
import java.util.List;

public record ProyectoListDTO(
    String id,
    String codigo,
    String nombre,
    String dependencia,
    String director,
    Boolean peti,
    BigDecimal avanceTotal,
    String estado,
    Integer entregablesTotal,
    Integer entregablesConformes,
    Integer entregablesAtrasados,
    List<FuragPreguntaRespuestaDTO> furagDetalle
) {}
