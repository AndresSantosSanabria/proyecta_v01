package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Probabilidad probabilidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Impacto impacto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelRiesgo nivel;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado = EstadoRiesgo.PENDIENTE;

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

    public Probabilidad getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Probabilidad probabilidad) {
        this.probabilidad = probabilidad;
    }

    public Impacto getImpacto() {
        return impacto;
    }

    public void setImpacto(Impacto impacto) {
        this.impacto = impacto;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }

    public void setEstado(EstadoRiesgo estado) {
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
