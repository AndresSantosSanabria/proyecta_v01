package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;

@Entity
@Table(name = "matriz_riesgo", schema = "proyecta_db")
public class MatrizRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matriz_riesgo_id")
    private Long id;

    @Column(nullable = false, length = 10)
    private String probabilidad;

    @Column(nullable = false, length = 10)
    private String impacto;

    @Column(name = "nivel_resultante", nullable = false, length = 20)
    private String nivelResultante;

    public MatrizRiesgo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProbabilidad() { return probabilidad; }
    public void setProbabilidad(String probabilidad) { this.probabilidad = probabilidad; }

    public String getImpacto() { return impacto; }
    public void setImpacto(String impacto) { this.impacto = impacto; }

    public String getNivelResultante() { return nivelResultante; }
    public void setNivelResultante(String nivelResultante) { this.nivelResultante = nivelResultante; }
}
