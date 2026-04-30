package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaseHierarchyDTO {
    private Integer id;
    private Short numero;
    private String descripcion;
    private BigDecimal ponderacion;
    private BigDecimal avanceCalculado;
    private List<HitoHierarchyDTO> hitos;

    public FaseHierarchyDTO() {}

    public FaseHierarchyDTO(Integer id, Short numero, String descripcion, BigDecimal ponderacion,
                            BigDecimal avanceCalculado, List<HitoHierarchyDTO> hitos) {
        this.id = id;
        this.numero = numero;
        this.descripcion = descripcion;
        this.ponderacion = ponderacion;
        this.avanceCalculado = avanceCalculado;
        this.hitos = hitos;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Short getNumero() { return numero; }
    public void setNumero(Short numero) { this.numero = numero; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }
    public List<HitoHierarchyDTO> getHitos() { return hitos; }
    public void setHitos(List<HitoHierarchyDTO> hitos) { this.hitos = hitos; }
}
