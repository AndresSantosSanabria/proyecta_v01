package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas_cierre")
public class ActaCierre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acta_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @Column(name = "resumen_ejecutivo", columnDefinition = "TEXT", nullable = false)
    private String resumenEjecutivo;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "avance_final", precision = 5, scale = 2, nullable = false)
    private BigDecimal avanceFinal;

    public ActaCierre() {
    }

    public ActaCierre(Proyecto proyecto, String resumenEjecutivo, LocalDateTime fechaCierre, BigDecimal avanceFinal) {
        this.proyecto = proyecto;
        this.resumenEjecutivo = resumenEjecutivo;
        this.fechaCierre = fechaCierre;
        this.avanceFinal = avanceFinal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public String getResumenEjecutivo() {
        return resumenEjecutivo;
    }

    public void setResumenEjecutivo(String resumenEjecutivo) {
        this.resumenEjecutivo = resumenEjecutivo;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getAvanceFinal() {
        return avanceFinal;
    }

    public void setAvanceFinal(BigDecimal avanceFinal) {
        this.avanceFinal = avanceFinal;
    }
}
