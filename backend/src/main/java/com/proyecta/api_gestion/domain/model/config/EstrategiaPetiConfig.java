package com.proyecta.api_gestion.domain.model.config;

import com.proyecta.api_gestion.domain.model.catalogo.ConfigCatalogoBase;
import jakarta.persistence.*;

@Entity
@Table(name = "estrategia_peti_config", schema = "proyecta_db")
@AttributeOverride(name = "descripcion", column = @Column(columnDefinition = "TEXT"))
public class EstrategiaPetiConfig extends ConfigCatalogoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estrategia_peti_id")
    private Long id;

    @Column(name = "vigencia_desde", length = 20)
    private String vigenciaDesde;

    @Column(name = "vigencia_hasta", length = 20)
    private String vigenciaHasta;

    public EstrategiaPetiConfig() {
        // Constructor por defecto requerido por JPA, sin lógica que ejecutar.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVigenciaDesde() { return vigenciaDesde; }
    public void setVigenciaDesde(String vigenciaDesde) { this.vigenciaDesde = vigenciaDesde; }

    public String getVigenciaHasta() { return vigenciaHasta; }
    public void setVigenciaHasta(String vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }
}
