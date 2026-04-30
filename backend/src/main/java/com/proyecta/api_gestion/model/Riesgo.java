package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_id")
    private Integer id;

    @Column(length = 10)
    private String codigo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Integer probabilidad; // 1-5

    @Column(nullable = false)
    private Integer impacto; // 1-5

    @Column(length = 20)
    private String nivel; // Calculado: Crítico, Alto, Moderado, Bajo

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(nullable = false)
    private String estado = "Pendiente"; // Pendiente, Tratado

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    public Riesgo() {
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Integer probabilidad) {
        this.probabilidad = probabilidad;
    }

    public Integer getImpacto() {
        return impacto;
    }

    public void setImpacto(Integer impacto) {
        this.impacto = impacto;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }
}
