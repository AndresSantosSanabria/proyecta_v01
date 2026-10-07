package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
    {
      "id": "PROY-CUN-2026-008",
      "codigo": "PROY-CUN-2026-008",
      "nombre": "Modernización de Redes LAN",
      "dependencia": "Secretaría de Transformación Digital",
      "director": "María Gómez",
      "directorUsuarioId": "usr-7c2f9a11-4f3e-4a2b-9d5b-111111111111",
      "correoDirector": "maria.gomez@proyecta.gov.co",
      "objetivoGeneral": "Modernizar la infraestructura de red de las sedes institucionales",
      "objetivosEspecificos": ["Actualizar switches core", "Implementar segmentación de red"],
      "fechaInicio": "2026-03-02",
      "estado": "ACTIVO",
      "avanceTotal": 55.00,
      "presupuestoEstimado": 1250000000.00,
      "alcanceDetallado": "Actualización de equipos de red en 8 sedes",
      "peti": true,
      "vigenciaPeti": "2026",
      "estrategiaPeti": "TECNOLOGIAS_INFORMACION",
      "tienePlanComunicaciones": true,
      "patrocinador": {"nombre": "Carlos Ramírez", "cargo": "Director de TIC", "procesoSigc": "PR-TIC-01", "procedimientoSigc": "P-TIC-07"},
      "equipoTrabajo": [],
      "stakeholders": [],
      "furag": null,
      "fases": []
    }
    """)
public record ProyectoResponseDTO(
    String id,
    String codigo,
    String nombre,
    String dependencia,
    String director,
    String directorUsuarioId,
    String correoDirector,
    String objetivoGeneral,
    List<String> objetivosEspecificos,
    LocalDate fechaInicio,
    String estado,
    BigDecimal avanceTotal,
    BigDecimal presupuestoEstimado,
    String alcanceDetallado,
    Boolean peti,
    String vigenciaPeti,
    String estrategiaPeti,
    Boolean tienePlanComunicaciones,
    PatrocinadorDTO patrocinador,
    List<EquipoTrabajoDTO> equipoTrabajo,
    List<StakeholderDTO> stakeholders,
    FuragDTO furag,
    List<FaseResponseDTO> fases
) {}
