package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
    {
      "proyectoId": "PROY-CUN-2026-008",
      "codigo": "PROY-CUN-2026-008",
      "nombre": "Modernización de Redes LAN",
      "progresoProgramado": 60.00,
      "progresoEjecutado": 55.00,
      "diferencia": -5.00,
      "eficacia": 91.67,
      "eficiencia": 91.67,
      "estado": "ACTIVO",
      "avanceTotal": 55.00,
      "entregablesConformes": 11,
      "entregablesTotal": 20,
      "entregablesAtrasados": 2,
      "proximosAVencer": 3,
      "entregablesProgramadosAlCorte": 12,
      "entregablesEntregadosAlCorte": 11,
      "entregablesEntregadosATiempo": 10,
      "corte": "2026-07-15",
      "fases": []
    }
    """)
public record ProyectoAvanceResponseDTO(
    String proyectoId,
    String codigo,
    String nombre,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    BigDecimal eficiencia,
    String estado,
    BigDecimal avanceTotal,
    Long entregablesConformes,
    Long entregablesTotal,
    Long entregablesAtrasados,
    Long proximosAVencer,
    Long entregablesProgramadosAlCorte,
    Long entregablesEntregadosAlCorte,
    Long entregablesEntregadosATiempo,
    LocalDate corte,
    List<FaseAvanceDTO> fases
) {}
