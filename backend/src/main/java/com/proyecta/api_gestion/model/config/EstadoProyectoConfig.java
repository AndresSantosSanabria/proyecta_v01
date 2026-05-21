package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;

@Entity
@Table(name = "estado_proyecto_config", schema = "proyecta_db")
public class EstadoProyectoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estado_proyecto_id")
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "es_terminal", nullable = false)
    private Boolean esTerminal = false;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public EstadoProyectoConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public Boolean getEsTerminal() { return esTerminal; }
    public void setEsTerminal(Boolean esTerminal) { this.esTerminal = esTerminal; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean esCerrado() {
        return "CERRADO".equals(codigo) || "FINALIZADO".equals(codigo);
    }

    public boolean esActivo() {
        return "ACTIVO".equals(codigo) || "CON_RETRASOS".equals(codigo) || "EN_REVISION".equals(codigo);
    }
}
