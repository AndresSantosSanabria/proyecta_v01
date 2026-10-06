package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class DocumentoSubidaBase {

    @Column(name = "subido_por", length = 200)
    private String subidoPor;

    @Column(name = "subido_rol", length = 80)
    private String subidoRol;

    public String getSubidoPor() { return subidoPor; }
    public void setSubidoPor(String subidoPor) { this.subidoPor = subidoPor; }
    public String getSubidoRol() { return subidoRol; }
    public void setSubidoRol(String subidoRol) { this.subidoRol = subidoRol; }
}
