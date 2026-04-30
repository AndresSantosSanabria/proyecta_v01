package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public class RiesgoResponseDTO {

    @Schema(description = "ID único del riesgo", example = "1")
    private Integer id;

    @Schema(description = "Código interno del riesgo", example = "R-001")
    private String codigo;

    @Schema(description = "Descripción del riesgo", example = "Retraso en servidores")
    private String descripcion;

    @Schema(description = "Probabilidad (1-5)", example = "4")
    private Integer probabilidad;

    @Schema(description = "Impacto (1-5)", example = "5")
    private Integer impacto;

    @Schema(description = "Nivel de riesgo calculado", example = "Crítico")
    private String nivel;

    @Schema(description = "Color para el badge en frontend", example = "#FF0000")
    private String colorTag;

    @Schema(description = "Plan de acción o tratamiento", example = "Alquiler temporal de capacidad en la nube")
    private String tratamiento;

    @Schema(description = "Estado actual del riesgo", example = "Pendiente")
    private String estado;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime fechaActualizacion;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Integer probabilidad) {
        this.probabilidad = probabilidad;
    }

    public Integer getImpacto() {
        return impacto;
    }

    public void setImpacto(Integer impacto) {
        this.impacto = impacto;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
