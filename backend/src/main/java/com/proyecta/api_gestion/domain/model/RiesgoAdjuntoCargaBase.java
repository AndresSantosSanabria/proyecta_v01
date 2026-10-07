package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.time.ZoneId;

@MappedSuperclass
public abstract class RiesgoAdjuntoCargaBase extends RiesgoAdjuntoBase {

    @Column(name = "fecha_carga", updatable = false)
    private LocalDateTime fechaCarga;

    @PrePersist
    protected void onCreate() {
        if (fechaCarga == null) {
            fechaCarga = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }
}
