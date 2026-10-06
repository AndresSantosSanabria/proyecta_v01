package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.domain.model.enums.Impacto;
import com.proyecta.api_gestion.domain.model.enums.Probabilidad;
import com.proyecta.api_gestion.domain.model.enums.TipoRiesgo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(example = """
    {"descripcion":"Retraso en la entrega de la infraestructura de red por parte del proveedor","probabilidad":"TRES","impacto":"CUATRO","tipoRiesgo":"GENERAL","tratamiento":"Mitigar con plan de contingencia","entidadResponsable":"Subdirección de Infraestructura","accionesMitigacion":"Reunión semanal con el proveedor","fechaAccion":"2026-08-01"}
    """)
public record RiesgoCompletitudDTO(
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "La probabilidad es obligatoria")
        Probabilidad probabilidad,

        @NotNull(message = "El impacto es obligatorio")
        Impacto impacto,

        @NotNull(message = "El tipo de riesgo es obligatorio")
        TipoRiesgo tipoRiesgo,

        String tratamiento,

        @NotBlank(message = "El responsable es obligatorio")
        String entidadResponsable,

        String accionesMitigacion,

        LocalDate fechaAccion
) {}
