package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fase")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Fase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fase_id")
    private Integer id;

    @Column(length = 150)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    @Column(name = "avance_calculado", precision = 5, scale = 2)
    private BigDecimal avanceCalculado = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hito> hitos = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    public Fase() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }

    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }

    public List<Hito> getHitos() { return hitos; }
    public void setHitos(List<Hito> hitos) { this.hitos = hitos; }

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
