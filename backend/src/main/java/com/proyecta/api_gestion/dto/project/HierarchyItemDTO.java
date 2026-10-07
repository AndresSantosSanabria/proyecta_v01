package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class HierarchyItemDTO {
    private Integer id;
    private Short numero;
    private String nombre;

    protected HierarchyItemDTO() {}

    protected HierarchyItemDTO(Integer id, Short numero, String nombre) {
        this.id = id;
        this.numero = numero;
        this.nombre = nombre;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Short getNumero() { return numero; }
    public void setNumero(Short numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
