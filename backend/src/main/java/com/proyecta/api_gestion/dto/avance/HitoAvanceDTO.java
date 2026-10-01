package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {
      "id": 45,
      "nombre": "Levantamiento de información",
      "descripcion": "Recopilación de requerimientos con las dependencias",
      "ponderacion": 15.00,
      "progresoProgramado": 80.00,
      "progresoEjecutado": 75.00,
      "diferencia": -5.00,
      "eficacia": 93.75,
      "estado": "EN_PROCESO",
      "avance": 75.00,
      "entregables": []
    }
    """)
public record HitoAvanceDTO(
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
    List<EntregableAvanceDTO> entregables
) {}
