package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO para la creación de un nuevo riesgo")
public class RiesgoCreateDTO {

    @NotBlank(message = "El ID del proyecto es obligatorio para asociar el riesgo")
    @Schema(description = "ID del proyecto asociado", example = "IS-PROY-2024-001")
    private String proyectoId;

    @NotBlank(message = "La descripción es obligatoria")
    @Schema(description = "Descripción detallada del riesgo", example = "Retraso en servidores")
    private String descripcion;

    @NotNull(message = "La probabilidad es obligatoria")
    @Min(1) @Max(5)
    private Integer probabilidad;

    @NotNull(message = "El impacto es obligatorio")
    @Min(1) @Max(5)
    private Integer impacto;

    // Getters and Setters
    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getProbabilidad() { return probabilidad; }
    public void setProbabilidad(Integer probabilidad) { this.probabilidad = probabilidad; }
    public Integer getImpacto() { return impacto; }
    public void setImpacto(Integer impacto) { this.impacto = impacto; }
}
