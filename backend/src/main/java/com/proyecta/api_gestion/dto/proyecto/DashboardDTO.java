package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;

public record DashboardDTO(
    Long totalProyectos,
    Long activos,
    Long cerrados,
    BigDecimal avancePromedio,
    Long entregablesAtrasados,
    Long proximosAVencer,
    Integer diasUmbralProximo
) {}
