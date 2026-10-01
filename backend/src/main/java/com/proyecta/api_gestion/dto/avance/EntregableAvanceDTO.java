package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(example = """
    {
      "id": 101,
      "nombre": "Documento de alcance aprobado",
      "descripcion": "Alcance funcional y técnico del proyecto",
      "ponderacion": 5.00,
      "fechaInicio": "2026-07-01",
      "fechaLimite": "2026-07-20",
      "progresoProgramado": 100.00,
      "progresoEjecutado": 100.00,
      "diferencia": 0.00,
      "eficacia": 100.00,
      "estado": "Conforme",
      "diasAtraso": 0,
      "fechaEntrega": "2026-07-18",
      "evidenciaPdf": "alcance_v2.pdf",
      "evidenciaUrl": "/api/v1/evidencias/101/descarga",
      "avance": 100.00,
      "atraso": 0,
      "estadoCodigo": "APROBADO",
      "observacionRevision": null,
      "diasCumplimiento": 2
    }
    """)
public record EntregableAvanceDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    LocalDate fechaInicio,
    LocalDate fechaLimite,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    String estado,
    Long diasAtraso,
    LocalDate fechaEntrega,
    String evidenciaPdf,
    String evidenciaUrl,
    BigDecimal avance,
    Long atraso,
    String estadoCodigo,
    String observacionRevision,
    Long diasCumplimiento
) {}
