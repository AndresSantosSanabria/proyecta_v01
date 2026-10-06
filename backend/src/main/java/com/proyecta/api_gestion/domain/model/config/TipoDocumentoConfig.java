package com.proyecta.api_gestion.domain.model.config;

import com.proyecta.api_gestion.domain.model.catalogo.ConfigCatalogoBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "tipo_documento_config", schema = "proyecta_db")
public class TipoDocumentoConfig extends ConfigCatalogoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_documento_id")
    private Long id;

    @Column(name = "require_pdf", nullable = false)
    private Boolean requierePdf = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public TipoDocumentoConfig() {
        // Constructor vacío intencional: lo requiere JPA para instanciar la entidad.
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getRequierePdf() { return requierePdf; }
    public void setRequierePdf(Boolean requierePdf) { this.requierePdf = requierePdf; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
