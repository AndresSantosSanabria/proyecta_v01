package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fase")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AttributeOverride(name = "id", column = @Column(name = "fase_id"))
public class Fase extends ProyectoNodoBase {

    @Column(name = "avance_calculado", precision = 5, scale = 2)
    private BigDecimal avanceCalculado = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hito> hitos = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    public Fase() {
        // Constructor por defecto requerido por JPA, sin lógica que ejecutar.
    }

    // Getters and Setters
    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }

    public List<Hito> getHitos() { return hitos; }
    public void setHitos(List<Hito> hitos) { this.hitos = hitos; }

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
}
