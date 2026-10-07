package com.proyecta.api_gestion.domain.model.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class CatalogoDenominacionBase extends CatalogoCodigoBase {

    @Column(nullable = false)
    private String nombre;

    @Column
    private String descripcion;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
