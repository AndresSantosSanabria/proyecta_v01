package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HitoHierarchyDTO {
    private Integer id;
    private Short numero;
    private String nombre;
    private String descripcion;
    private BigDecimal ponderacion;
    private BigDecimal avanceCalculado;
    private List<EntregableHierarchyDTO> entregables;

    public HitoHierarchyDTO() {}

    public HitoHierarchyDTO(Integer id, Short numero, String nombre, String descripcion, BigDecimal ponderacion,
                            BigDecimal avanceCalculado, List<EntregableHierarchyDTO> entregables) {
        this.id = id;
        this.numero = numero;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ponderacion = ponderacion;
        this.avanceCalculado = avanceCalculado;
        this.entregables = entregables;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Short getNumero() { return numero; }
    public void setNumero(Short numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }
    public List<EntregableHierarchyDTO> getEntregables() { return entregables; }
    public void setEntregables(List<EntregableHierarchyDTO> entregables) { this.entregables = entregables; }
}
