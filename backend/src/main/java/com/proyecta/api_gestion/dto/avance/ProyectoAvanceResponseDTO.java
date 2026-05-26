package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyectoAvanceResponseDTO(
    String proyectoId,
    String codigo,
    String nombre,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    String estado,
    BigDecimal avanceTotal,
    Long entregablesConformes,
    Long entregablesTotal,
    Long entregablesAtrasados,
    Long proximosAVencer,
    LocalDate corte,
    List<FaseAvanceDTO> fases
) {}
