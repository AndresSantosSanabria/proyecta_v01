package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RiesgoRequestDTO(
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        String categoriaRiesgo,
        String causa,
        String consecuencia,

        @NotNull(message = "La probabilidad es obligatoria")
        Probabilidad probabilidad,

        @NotNull(message = "El impacto es obligatorio")
        Impacto impacto,

        Probabilidad probabilidadResidual,
        Impacto impactoResidual,
        String controlesExistentes,
        String tipoControl,
        String valoracionControl,

        @NotBlank(message = "El plan de tratamiento es obligatorio")
        String tratamiento,

        String accionesMitigacion,
        String entidadResponsable,
        String rolResponsable,
        LocalDate fechaAccion,
        String evidenciaIndicador,

        @NotNull(message = "El estado es obligatorio")
        EstadoRiesgo estado
) {}
