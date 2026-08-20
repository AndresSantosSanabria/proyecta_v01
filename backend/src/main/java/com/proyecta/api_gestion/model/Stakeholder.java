package com.proyecta.api_gestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Stakeholder {

    @Column(name = "stakeholder_rol", length = 150)
    private String rol;

    @Column(name = "stakeholder_descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "stakeholder_interes", columnDefinition = "TEXT")
    private String interes;

    @Column(name = "stakeholder_impacto", columnDefinition = "TEXT")
    private String impacto;

    public Stakeholder() {
    }

    public Stakeholder(String rol, String descripcion, String interes, String impacto) {
        this.rol = rol;
        this.descripcion = descripcion;
        this.interes = interes;
        this.impacto = impacto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getInteres() {
        return interes;
    }

    public void setInteres(String interes) {
        this.interes = interes;
    }

    public String getImpacto() {
        return impacto;
    }

    public void setImpacto(String impacto) {
        this.impacto = impacto;
    }
}
