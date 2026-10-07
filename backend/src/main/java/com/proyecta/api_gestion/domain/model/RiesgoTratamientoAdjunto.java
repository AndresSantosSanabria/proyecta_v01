package com.proyecta.api_gestion.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "riesgo_tratamiento_adjunto")
public class RiesgoTratamientoAdjunto extends RiesgoAdjuntoCargaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_tratamiento_adjunto_id")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "riesgo_tratamiento_id", nullable = false)
    private RiesgoTratamiento tratamiento;

    public RiesgoTratamientoAdjunto() {
        // Constructor vacío intencional: requerido por JPA para instanciar la entidad.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RiesgoTratamiento getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(RiesgoTratamiento tratamiento) {
        this.tratamiento = tratamiento;
    }
}
