package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;

@Entity
@Table(name = "rol_config", schema = "proyecta_db")
public class RolConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "nivel_acceso", nullable = false)
    private Integer nivelAcceso = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public RolConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getNivelAcceso() { return nivelAcceso; }
    public void setNivelAcceso(Integer nivelAcceso) { this.nivelAcceso = nivelAcceso; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(codigo);
    }

    public boolean tieneAccesoGestion() {
        return nivelAcceso != null && nivelAcceso >= 60;
    }
}
