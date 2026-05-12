package com.gobernacion.proyecta.proyectos.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProyectoResponseDTO(
    String id,
    String nombre,
    String dependencia,
    String director,
    String estado,
    BigDecimal avanceTotal,
    LocalDate fechaInicio
) {}
