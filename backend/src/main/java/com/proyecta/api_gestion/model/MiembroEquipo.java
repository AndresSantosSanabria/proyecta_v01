package com.proyecta.api_gestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MiembroEquipo {

    @Column(name = "miembro_nombre", length = 120)
    private String nombre;

    @Column(name = "miembro_cargo", length = 100)
    private String cargo;

    @Column(name = "miembro_rol", length = 100)
    private String rol;

    @Column(name = "miembro_dependencia", length = 150)
    private String dependencia;

    @Column(name = "miembro_telefono", length = 30)
    private String telefono;

    @Column(name = "miembro_correo", length = 150)
    private String correo;

    public MiembroEquipo() {
    }

    public MiembroEquipo(String nombre, String cargo, String rol, String dependencia, String telefono, String correo) {
        this.nombre = nombre;
        this.cargo = cargo;
        this.rol = rol;
        this.dependencia = dependencia;
        this.telefono = telefono;
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getDependencia() {
        return dependencia;
    }

    public void setDependencia(String dependencia) {
        this.dependencia = dependencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
