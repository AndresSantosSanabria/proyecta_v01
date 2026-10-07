package com.proyecta.api_gestion.domain.model.config;

import com.proyecta.api_gestion.domain.model.catalogo.ConfigCatalogoBase;
import jakarta.persistence.*;

@Entity
@Table(name = "estado_proyecto_config", schema = "proyecta_db")
public class EstadoProyectoConfig extends ConfigCatalogoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estado_proyecto_id")
    private Long id;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "es_terminal", nullable = false)
    private Boolean esTerminal = false;

    public EstadoProyectoConfig() {
        // Constructor vacio requerido por JPA.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public Boolean getEsTerminal() { return esTerminal; }
    public void setEsTerminal(Boolean esTerminal) { this.esTerminal = esTerminal; }

    public boolean esCerrado() {
        return "CERRADO".equals(getCodigo()) || "FINALIZADO".equals(getCodigo());
    }

    public boolean esActivo() {
        return "ACTIVO".equals(getCodigo()) || "CON_RETRASOS".equals(getCodigo()) || "EN_REVISION".equals(getCodigo());
    }
}
