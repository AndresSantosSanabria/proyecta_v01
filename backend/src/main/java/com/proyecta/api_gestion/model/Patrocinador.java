package com.proyecta.api_gestion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "patrocinador")
public class Patrocinador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patrocinador_id")
    private Integer id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 100)
    private String cargo;

    @Column(length = 100)
    private String dependencia;

    @Column(length = 150)
    private String entidad;

    @Column(name = "proceso_sigc", length = 100)
    private String procesoSigc;

    @Column(length = 150)
    private String procedimiento;

    public Patrocinador() {
    }

    // Manual Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getDependencia() {
        return dependencia;
    }

    public void setDependencia(String dependencia) {
        this.dependencia = dependencia;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getProcesoSigc() {
        return procesoSigc;
    }

    public void setProcesoSigc(String procesoSigc) {
        this.procesoSigc = procesoSigc;
    }

    public String getProcedimiento() {
        return procedimiento;
    }

    public void setProcedimiento(String procedimiento) {
        this.procedimiento = procedimiento;
    }
}
