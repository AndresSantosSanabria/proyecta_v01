package com.proyecta.api_gestion.dto.proyecto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        FuragDTO furag
) {
}
