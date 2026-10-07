package com.proyecta.api_gestion.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "riesgo_solucion_adjunto")
public class RiesgoSolucionAdjunto extends RiesgoAdjuntoCargaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_solucion_adjunto_id")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "riesgo_id", nullable = false)
    private Riesgo riesgo;

    public RiesgoSolucionAdjunto() {
        // Constructor vacío intencional: lo requiere JPA para instanciar la entidad.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Riesgo getRiesgo() {
        return riesgo;
    }

    public void setRiesgo(Riesgo riesgo) {
        this.riesgo = riesgo;
    }
}
