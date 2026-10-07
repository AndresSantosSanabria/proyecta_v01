package com.proyecta.api_gestion.domain.model.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class CatalogoCodigoBase {

    @Column(nullable = false, unique = true)
    private String codigo;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
