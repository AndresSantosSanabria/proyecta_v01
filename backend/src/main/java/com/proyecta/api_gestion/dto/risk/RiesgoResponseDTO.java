package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.Probabilidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RiesgoResponseDTO(
        Integer id,
        String codigo,
        String categoriaRiesgo,
        String descripcion,
        String causa,
        String consecuencia,
        Probabilidad probabilidad,
        Impacto impacto,
        Integer calificacionInherente,
        NivelRiesgo nivel,
        String controlesExistentes,
        String tipoControl,
        String valoracionControl,
        Probabilidad probabilidadResidual,
        Impacto impactoResidual,
        NivelRiesgo nivelResidual,
        String tratamiento,
        String accionesMitigacion,
        String entidadResponsable,
        String rolResponsable,
        LocalDate fechaAccion,
        String evidenciaIndicador,
        EstadoRiesgo estado,
        LocalDateTime fechaActualizacion,
        List<RiesgoSolucionAdjuntoDTO> soluciones
) {}
