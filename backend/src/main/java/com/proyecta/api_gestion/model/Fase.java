package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fase")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Fase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fase_id")
    private Integer id;

    @Column(nullable = false)
    private Short numero;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    @Column(name = "avance_calculado", precision = 5, scale = 2)
    private BigDecimal avanceCalculado = BigDecimal.ZERO;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    public Fase() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    // Manual Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Short getNumero() {
        return numero;
    }

    public void setNumero(Short numero) {
        this.numero = numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(BigDecimal ponderacion) {
        this.ponderacion = ponderacion;
    }

    public BigDecimal getAvanceCalculado() {
        return avanceCalculado;
    }

    public void setAvanceCalculado(BigDecimal avanceCalculado) {
        this.avanceCalculado = avanceCalculado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }
}
