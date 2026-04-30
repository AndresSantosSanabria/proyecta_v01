package com.proyecta.api_gestion.dto.timeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO que representa un elemento del cronograma (Fase o Hito).
 * El campo {@code children} solo se serializa cuando contiene elementos,
 * previniendo recursión infinita en serialización JSON.
 */
@Schema(description = "Representa un elemento del cronograma (Fase o Hito)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimelineItemDTO {

    @Schema(description = "ID del elemento")
    private Integer id;

    @Schema(description = "Nombre o descripción del elemento")
    private String nombre;

    @Schema(description = "Tipo de elemento: Fase o Hito", example = "Fase")
    private String tipo;

    @Schema(description = "Fecha de inicio calculada")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha de fin calculada")
    private LocalDate fechaFin;

    @Schema(description = "Progreso o avance calculado (0-100)")
    private BigDecimal progreso;

    @Schema(description = "Hitos anidados (solo presente en elementos de tipo Fase)")
    private List<TimelineItemDTO> children;

    public TimelineItemDTO() {}

    public TimelineItemDTO(Integer id, String nombre, String tipo,
                           LocalDate fechaInicio, LocalDate fechaFin, BigDecimal progreso) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.progreso = progreso;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getProgreso() {
        return progreso;
    }

    public void setProgreso(BigDecimal progreso) {
        this.progreso = progreso;
    }

    public List<TimelineItemDTO> getChildren() {
        return children;
    }

    public void setChildren(List<TimelineItemDTO> children) {
        this.children = children;
    }
}
