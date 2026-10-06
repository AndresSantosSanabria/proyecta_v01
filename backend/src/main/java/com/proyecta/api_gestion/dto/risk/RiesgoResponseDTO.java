package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.domain.model.enums.Impacto;
import com.proyecta.api_gestion.domain.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.domain.model.enums.Probabilidad;
import com.proyecta.api_gestion.domain.model.enums.TipoRiesgo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(example = """
    {
      "id": 34,
      "codigo": "RIES-PROY-CUN-2026-008-001",
      "descripcion": "Retraso en la entrega de la infraestructura de red por parte del proveedor",
      "probabilidad": "TRES",
      "impacto": "CUATRO",
      "calificacionInherente": 12,
      "nivel": "ALTO",
      "tipoRiesgo": "GENERAL",
      "tratamiento": "Mitigar con plan de contingencia y seguimiento semanal",
      "entidadResponsable": "Subdirección de Infraestructura",
      "accionesMitigacion": "Reunión semanal con el proveedor y hitos de verificación",
      "fechaAccion": "2026-08-01",
      "evidenciaIndicador": "Acta de seguimiento semanal",
      "estado": "PENDIENTE",
      "fechaActualizacion": "2026-07-15T10:30:00",
      "soluciones": [],
      "tratamientos": [],
      "createdBy": "juan.perez@proyecta.gov.co"
    }
    """)
public record RiesgoResponseDTO(
        Integer id,
        String codigo,
        String descripcion,
        Probabilidad probabilidad,
        Impacto impacto,
        Integer calificacionInherente,
        NivelRiesgo nivel,
        TipoRiesgo tipoRiesgo,
        String tratamiento,
        String entidadResponsable,
        String accionesMitigacion,
        LocalDate fechaAccion,
        String evidenciaIndicador,
        EstadoRiesgo estado,
        LocalDateTime fechaActualizacion,
        List<RiesgoSolucionAdjuntoDTO> soluciones,
        List<RiesgoTratamientoDTO> tratamientos,
        String createdBy
) {}
