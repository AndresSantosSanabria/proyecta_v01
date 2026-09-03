package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RiesgoCompletitudDTO(
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "La probabilidad es obligatoria")
        Probabilidad probabilidad,

        @NotNull(message = "El impacto es obligatorio")
        Impacto impacto,

        String tratamiento,

        @NotBlank(message = "El responsable es obligatorio")
        String entidadResponsable,

        String accionesMitigacion,

        LocalDate fechaAccion
) {}
