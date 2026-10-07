package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;

/**
 * Trazabilidad comun de un documento: identidad y enlaces a entregable/version.
 */
@MappedSuperclass
public abstract class DocumentoTrazabilidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_version_id")
    private DocumentoVersion version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Entregable getEntregable() { return entregable; }
    public void setEntregable(Entregable entregable) { this.entregable = entregable; }

    public DocumentoVersion getVersion() { return version; }
    public void setVersion(DocumentoVersion version) { this.version = version; }
}
