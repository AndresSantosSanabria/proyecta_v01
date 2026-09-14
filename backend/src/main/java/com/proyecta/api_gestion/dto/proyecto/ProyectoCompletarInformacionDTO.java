package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyectoCompletarInformacionDTO(
        @NotBlank String dependencia,
        @NotNull LocalDate fechaInicio,
        String alcanceDetallado,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal presupuestoEstimado,
        List<String> objetivosEspecificos,
        PatrocinadorDTO patrocinador,
        List<EquipoTrabajoDTO> equipoTrabajo,
        List<StakeholderDTO> stakeholders,
        List<FaseDTO> fases,
        @NotNull Boolean peti,
        String vigenciaPeti,
        String estrategiaPeti,
        @NotNull Boolean tienePlanComunicaciones,
        FuragDTO furag,
        @Size(min = 2, message = "Debe registrar al menos 2 riesgos en la matriz de riesgos")
        List<RiesgoCompletitudDTO> riesgosIniciales
) {
    public ProyectoCompletarInformacionDTO {
        if (riesgosIniciales != null) {
            boolean tieneGeneral = riesgosIniciales.stream().anyMatch(r -> r.tipoRiesgo() == com.proyecta.api_gestion.model.enums.TipoRiesgo.GENERAL);
            boolean tieneSeguridad = riesgosIniciales.stream().anyMatch(r -> r.tipoRiesgo() == com.proyecta.api_gestion.model.enums.TipoRiesgo.SEGURIDAD);
            if (!tieneGeneral) {
                throw new IllegalArgumentException("Debe registrar al menos 1 riesgo de tipo GENERAL");
            }
            if (!tieneSeguridad) {
                throw new IllegalArgumentException("Debe registrar al menos 1 riesgo de tipo SEGURIDAD");
            }
        }
    }
}
