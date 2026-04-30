package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO para la actualización de un riesgo existente")
public class RiesgoUpdateDTO {

    @NotNull(message = "El ID del riesgo es obligatorio para actualizar")
    @Schema(description = "ID único del riesgo", example = "1")
    private Integer id;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "La probabilidad es obligatoria")
    @Min(1) @Max(5)
    private Integer probabilidad;

    @NotNull(message = "El impacto es obligatorio")
    @Min(1) @Max(5)
    private Integer impacto;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getProbabilidad() { return probabilidad; }
    public void setProbabilidad(Integer probabilidad) { this.probabilidad = probabilidad; }
    public Integer getImpacto() { return impacto; }
    public void setImpacto(Integer impacto) { this.impacto = impacto; }
}
