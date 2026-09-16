package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.model.enums.TipoRiesgo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
