package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;

@MappedSuperclass
public abstract class DocumentoCargaBase extends RiesgoAdjuntoBase {

    @Column(name = "proyecto_id", length = 30, nullable = false)
    private String proyectoId;

    @Column(name = "url_descarga", length = 500)
    private String urlDescarga;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCarga == null) {
            this.fechaCarga = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now(ZoneId.systemDefault());
    }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }
    public String getUrlDescarga() { return urlDescarga; }
    public void setUrlDescarga(String urlDescarga) { this.urlDescarga = urlDescarga; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
