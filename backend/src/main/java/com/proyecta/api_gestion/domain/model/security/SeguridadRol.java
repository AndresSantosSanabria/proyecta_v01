package com.proyecta.api_gestion.domain.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles", schema = "proyecta_db")
public class SeguridadRol extends SeguridadCatalogoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transversal", nullable = false)
    private Boolean transversal = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getTransversal() { return transversal; }
    public void setTransversal(Boolean transversal) { this.transversal = transversal; }
}
