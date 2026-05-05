package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import java.math.BigDecimal;

public record ProyectoListDTO(
    String id,
    String codigo,
    String nombre,
    String dependencia,
    String director,
    Boolean peti,
    BigDecimal avanceTotal,
    EstadoProyecto estado,
    Integer entregablesTotal,
    Integer entregablesConformes,
    Integer entregablesAtrasados
) {}
