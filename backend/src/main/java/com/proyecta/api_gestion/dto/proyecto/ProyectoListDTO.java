package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.dto.config.FuragPreguntaRespuestaDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(example = """
    {
      "id": "PROY-CUN-2026-008",
      "codigo": "PROY-CUN-2026-008",
      "nombre": "Modernización de Redes LAN",
      "dependencia": "Secretaría de Transformación Digital",
      "director": "María Gómez",
      "peti": true,
      "avanceTotal": 55.00,
      "estado": "ACTIVO",
      "viabilidadEstado": "APROBADO",
      "documentosCargados": true,
      "entregablesTotal": 20,
      "entregablesConformes": 11,
      "entregablesAtrasados": 2,
      "furagDetalle": [],
      "cierreForzoso": false,
      "cierreObservaciones": null,
      "cierreForzosoPor": null,
      "cierreForzosoEn": null
    }
    """)
public record ProyectoListDTO(
    String id,
    String codigo,
    String nombre,
    String dependencia,
    String director,
    Boolean peti,
    BigDecimal avanceTotal,
    String estado,
    String viabilidadEstado,
    Boolean documentosCargados,
    Integer entregablesTotal,
    Integer entregablesConformes,
    Integer entregablesAtrasados,
    List<FuragPreguntaRespuestaDTO> furagDetalle,
    Boolean cierreForzoso,
    String cierreObservaciones,
    String cierreForzosoPor,
    LocalDateTime cierreForzosoEn
) {}
