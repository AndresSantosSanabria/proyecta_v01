package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hito")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AttributeOverride(name = "id", column = @Column(name = "hito_id"))
public class Hito extends ProyectoNodoBase {

    @Column(name = "avance_calculado", precision = 5, scale = 2)
    private BigDecimal avanceCalculado = BigDecimal.ZERO;

    @Column(name = "estado_revision", length = 30)
    private String estadoRevision; // 'PENDIENTE', 'APROBADO', 'RECHAZADO'

    @OneToMany(mappedBy = "hito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entregable> entregables = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fase_id")
    private Fase fase;

    public Hito() {
        // Constructor vacío intencional: lo requiere JPA para instanciar la entidad.
    }

    // Getters and Setters
    public BigDecimal getAvanceCalculado() { return avanceCalculado; }
    public void setAvanceCalculado(BigDecimal avanceCalculado) { this.avanceCalculado = avanceCalculado; }

    public String getEstadoRevision() { return estadoRevision; }
    public void setEstadoRevision(String estadoRevision) { this.estadoRevision = estadoRevision; }

    public List<Entregable> getEntregables() { return entregables; }
    public void setEntregables(List<Entregable> entregables) { this.entregables = entregables; }

    public Fase getFase() { return fase; }
    public void setFase(Fase fase) { this.fase = fase; }
}
