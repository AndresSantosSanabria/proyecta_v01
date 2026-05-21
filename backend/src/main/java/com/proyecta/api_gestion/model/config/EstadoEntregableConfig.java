package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "estado_entregable_config", schema = "proyecta_db")
public class EstadoEntregableConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estado_entregable_id")
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "es_conforme", nullable = false)
    private Boolean esConforme = false;

    @Column(name = "es_terminal", nullable = false)
    private Boolean esTerminal = false;

    @Column(name = "cuenta_avance", precision = 5, scale = 2, nullable = false)
    private BigDecimal cuentaAvance = BigDecimal.ZERO;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public EstadoEntregableConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getEsConforme() { return esConforme; }
    public void setEsConforme(Boolean esConforme) { this.esConforme = esConforme; }

    public Boolean getEsTerminal() { return esTerminal; }
    public void setEsTerminal(Boolean esTerminal) { this.esTerminal = esTerminal; }

    public BigDecimal getCuentaAvance() { return cuentaAvance; }
    public void setCuentaAvance(BigDecimal cuentaAvance) { this.cuentaAvance = cuentaAvance; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean esConforme() {
        return Boolean.TRUE.equals(esConforme);
    }

    public boolean esTerminal() {
        return Boolean.TRUE.equals(esTerminal);
    }
}
