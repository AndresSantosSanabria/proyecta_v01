package com.proyecta.api_gestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MiembroEquipo {

    @Column(name = "miembro_nombre", length = 120)
    private String nombre;

    @Column(name = "miembro_rol", length = 100)
    private String rol;

    public MiembroEquipo() {
    }

    public MiembroEquipo(String nombre, String rol) {
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
