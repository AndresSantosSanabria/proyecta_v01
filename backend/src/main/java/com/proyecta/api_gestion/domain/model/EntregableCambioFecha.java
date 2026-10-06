package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entregable_cambio_fecha")
public class EntregableCambioFecha extends EntregableCambioBase {

    @Column(name = "fecha_anterior", nullable = false)
    private LocalDate fechaAnterior;

    @Column(name = "fecha_nueva", nullable = false)
    private LocalDate fechaNueva;

    public LocalDate getFechaAnterior() { return fechaAnterior; }
    public void setFechaAnterior(LocalDate fechaAnterior) { this.fechaAnterior = fechaAnterior; }

    public LocalDate getFechaNueva() { return fechaNueva; }
    public void setFechaNueva(LocalDate fechaNueva) { this.fechaNueva = fechaNueva; }
}
