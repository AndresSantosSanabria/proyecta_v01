package com.proyecta.api_gestion.domain.model;

import com.proyecta.api_gestion.domain.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.domain.model.enums.TipoDocumento;
import jakarta.persistence.*;

@Entity
@Table(name = "documento")
public class Documento extends DocumentoCargaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 30, nullable = false)
    private TipoDocumento tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_config_id", referencedColumnName = "tipo_documento_id")
    private TipoDocumentoConfig tipoDocumentoConfig;

    public Documento() {
        // Constructor por defecto requerido por JPA, sin lógica que ejecutar.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public TipoDocumentoConfig getTipoDocumentoConfig() { return tipoDocumentoConfig; }
    public void setTipoDocumentoConfig(TipoDocumentoConfig tipoDocumentoConfig) { this.tipoDocumentoConfig = tipoDocumentoConfig; }

    public String getTipoDocumentoCodigo() {
        if (tipoDocumentoConfig != null) return tipoDocumentoConfig.getCodigo();
        if (tipoDocumento != null) return tipoDocumento.name();
        return null;
    }
}
