package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RiesgoTratamientoDTO {

    @NotNull(message = "El ID del riesgo es obligatorio")
    @Schema(description = "ID del riesgo a tratar", example = "1")
    private Integer id;

    @NotBlank(message = "El plan de tratamiento no puede estar vacío")
    @Schema(description = "Descripción del plan de acción o mitigación", example = "Se procedió a contratar un servicio de backup externo")
    private String tratamiento;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }
}
