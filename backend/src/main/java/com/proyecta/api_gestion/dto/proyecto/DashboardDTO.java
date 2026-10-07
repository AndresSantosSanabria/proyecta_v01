package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(example = """
    {"totalProyectos":25,"activos":18,"cerrados":7,"avancePromedio":65.40,"entregablesAtrasados":4,"proximosAVencer":3,"diasUmbralProximo":7}
    """)
public record DashboardDTO(
    Long totalProyectos,
    Long activos,
    Long cerrados,
    BigDecimal avancePromedio,
    Long entregablesAtrasados,
    Long proximosAVencer,
    Integer diasUmbralProximo
) {}
