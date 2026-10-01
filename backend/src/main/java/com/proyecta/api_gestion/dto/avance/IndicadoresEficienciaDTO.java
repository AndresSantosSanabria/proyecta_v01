package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombreProyecto":"Modernización de Redes LAN","fechaCorte":"2026-07-15","programadosAlCorte":12,"entregadosAlCorte":11,"entregadosATiempo":10,"eficacia":91.67,"eficiencia":83.33,"totalEntregables":20}
    """)
public record IndicadoresEficienciaDTO(
    String proyectoId,
    String nombreProyecto,
    LocalDate fechaCorte,
    long programadosAlCorte,
    long entregadosAlCorte,
    long entregadosATiempo,
    BigDecimal eficacia,
    BigDecimal eficiencia,
    long totalEntregables
) {}
