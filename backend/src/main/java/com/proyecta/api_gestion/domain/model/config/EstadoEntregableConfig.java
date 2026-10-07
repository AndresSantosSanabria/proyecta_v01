package com.proyecta.api_gestion.domain.model.config;

import com.proyecta.api_gestion.domain.model.catalogo.ConfigCatalogoBase;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "estado_entregable_config", schema = "proyecta_db")
public class EstadoEntregableConfig extends ConfigCatalogoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estado_entregable_id")
    private Long id;

    @Column(name = "es_conforme", nullable = false)
    private Boolean esConforme = false;

    @Column(name = "es_terminal", nullable = false)
    private Boolean esTerminal = false;

    @Column(name = "cuenta_avance", precision = 5, scale = 2, nullable = false)
    private BigDecimal cuentaAvance = BigDecimal.ZERO;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    public EstadoEntregableConfig() {
        // Constructor vacío intencional: requerido por JPA para instanciar la entidad.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getEsConforme() { return esConforme; }
    public void setEsConforme(Boolean esConforme) { this.esConforme = esConforme; }

    public Boolean getEsTerminal() { return esTerminal; }
    public void setEsTerminal(Boolean esTerminal) { this.esTerminal = esTerminal; }

    public BigDecimal getCuentaAvance() { return cuentaAvance; }
    public void setCuentaAvance(BigDecimal cuentaAvance) { this.cuentaAvance = cuentaAvance; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean esConforme() {
        return Boolean.TRUE.equals(esConforme);
    }

    public boolean esTerminal() {
        return Boolean.TRUE.equals(esTerminal);
    }
}
