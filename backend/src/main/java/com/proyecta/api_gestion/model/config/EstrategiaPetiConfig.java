package com.proyecta.api_gestion.model.config;

import jakarta.persistence.*;

@Entity
@Table(name = "estrategia_peti_config", schema = "proyecta_db")
public class EstrategiaPetiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estrategia_peti_id")
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "vigencia_desde", length = 20)
    private String vigenciaDesde;

    @Column(name = "vigencia_hasta", length = 20)
    private String vigenciaHasta;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public EstrategiaPetiConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getVigenciaDesde() { return vigenciaDesde; }
    public void setVigenciaDesde(String vigenciaDesde) { this.vigenciaDesde = vigenciaDesde; }

    public String getVigenciaHasta() { return vigenciaHasta; }
    public void setVigenciaHasta(String vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
