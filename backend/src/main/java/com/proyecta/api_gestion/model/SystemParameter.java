package com.proyecta.api_gestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_parameters")
public class SystemParameter {

    @Id
    @Column(name = "param_key", length = 50)
    private String key;

    @Column(name = "param_value", nullable = false)
    private String value;

    @Column
    private String descripcion;

    public SystemParameter() {}

    public SystemParameter(String key, String value, String descripcion) {
        this.key = key;
        this.value = value;
        this.descripcion = descripcion;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
