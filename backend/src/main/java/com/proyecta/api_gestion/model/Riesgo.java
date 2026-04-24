package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.enums.Probabilidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "riesgos")
@Getter
@Setter
@NoArgsConstructor
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_id")
    private Integer id;

    @Column(length = 10)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Probabilidad probabilidad;

    @Column(length = 20)
    private String impacto;

    @Column(length = 15)
    private String nivel;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    private Boolean tratado = false;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
