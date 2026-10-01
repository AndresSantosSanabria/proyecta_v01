package com.proyecta.api_gestion.dto.proyecto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para el resumen ejecutivo del proyecto previo al cierre.
 * Uso de 'record' siguiendo las recomendaciones de Java 25.
 */
@Schema(example = """
    {
      "id": "PROY-CUN-2026-008",
      "nombre": "Modernización de Redes LAN",
      "dependencia": "Secretaría de Transformación Digital",
      "director": "María Gómez",
      "directorCargo": "Jefe de Oficina",
      "directorEntidad": "Secretaría de Transformación Digital",
      "patrocinadorNombre": "Carlos Ramírez",
      "patrocinadorCargo": "Director de TIC",
      "patrocinadorEntidad": "Vicerrectoría Administrativa",
      "fechaInicio": "2026-03-02",
      "objetivoGeneral": "Modernizar la infraestructura de red de las sedes institucionales",
      "objetivosEspecificos": ["Actualizar switches core", "Implementar segmentación de red"],
      "avanceTotal": 96.50,
      "estado": "ACTIVO",
      "totalFases": 4,
      "totalHitos": 12,
      "entregablesConformes": 19,
      "totalEntregables": 20,
      "puede_cerrar": true,
      "entregables": ["Documento de alcance aprobado", "Plan de pruebas"],
      "cierre_solicitado": false,
      "cierre_estado": null,
      "cierre_observaciones": null,
      "cierre_borrador_json": null
    }
    """)
public record ProyectoResumenDTO(
    String id,
    String nombre,
    String dependencia,
    String director,
    String directorCargo,
    String directorEntidad,
    String patrocinadorNombre,
    String patrocinadorCargo,
    String patrocinadorEntidad,
    LocalDate fechaInicio,
    String objetivoGeneral,
    List<String> objetivosEspecificos,
    BigDecimal avanceTotal,
    String estado,
    long totalFases,
    long totalHitos,
    long entregablesConformes,
    long totalEntregables,
    @JsonProperty("puede_cerrar") boolean puedeCerrar,
    List<String> entregables,
    @JsonProperty("cierre_solicitado") boolean cierreSolicitado,
    @JsonProperty("cierre_estado") String cierreEstado,
    @JsonProperty("cierre_observaciones") String cierreObservaciones,
    @JsonProperty("cierre_borrador_json") String cierreBorradorJson
) {}
