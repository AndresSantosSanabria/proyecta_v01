package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class PlanificacionHierarchyDTO extends HierarchyItemDTO {
    private String descripcion;
    private BigDecimal ponderacion;
    private BigDecimal avanceCalculado;

    protected PlanificacionHierarchyDTO() {}

    protected PlanificacionHierarchyDTO(Integer id, Short numero, String nombre, String descripcion, BigDecimal ponderacion,
                                        BigDecimal avanceCalculado) {
        super(id, numero, nombre);
        this.descripcion = descripcion;
        this.ponderacion = ponderacion;
        this.avanceCalculado = avanceCalculado;
    }

    // Getters and Setters
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }
}
