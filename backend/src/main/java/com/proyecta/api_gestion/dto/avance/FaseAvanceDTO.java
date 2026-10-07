package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {
      "id": 12,
      "nombre": "Planificación",
      "descripcion": "Definición del alcance y cronograma",
      "ponderacion": 20.00,
      "progresoProgramado": 100.00,
      "progresoEjecutado": 95.00,
      "diferencia": -5.00,
      "eficacia": 95.00,
      "estado": "COMPLETADO",
      "avance": 95.00,
      "hitos": []
    }
    """)
public record FaseAvanceDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    String estado,
    BigDecimal avance,
    List<HitoAvanceDTO> hitos
) {}
