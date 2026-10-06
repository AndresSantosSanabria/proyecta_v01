package com.proyecta.api_gestion.domain.model.security;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_permiso", schema = "proyecta_db")
public class SeguridadRolPermiso extends SeguridadRegistroBase {

    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private SeguridadRol rol;

    @ManyToOne(optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private SeguridadPermiso permiso;

    public SeguridadRol getRol() {
        return rol;
    }

    public void setRol(SeguridadRol rol) {
        this.rol = rol;
    }

    public SeguridadPermiso getPermiso() {
        return permiso;
    }

    public void setPermiso(SeguridadPermiso permiso) {
        this.permiso = permiso;
    }
}
