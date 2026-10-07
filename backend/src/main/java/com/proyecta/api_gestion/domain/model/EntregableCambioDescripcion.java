package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "entregable_cambio_descripcion")
public class EntregableCambioDescripcion extends EntregableCambioBase {

    @Column(name = "descripcion_anterior", columnDefinition = "TEXT")
    private String descripcionAnterior;

    @Column(name = "descripcion_nueva", columnDefinition = "TEXT", nullable = false)
    private String descripcionNueva;

    public String getDescripcionAnterior() { return descripcionAnterior; }
    public void setDescripcionAnterior(String descripcionAnterior) { this.descripcionAnterior = descripcionAnterior; }

    public String getDescripcionNueva() { return descripcionNueva; }
    public void setDescripcionNueva(String descripcionNueva) { this.descripcionNueva = descripcionNueva; }
}
