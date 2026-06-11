package com.proyecta.api_gestion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_auditoria")
public class DocumentoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_auditoria_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_version_id")
    private DocumentoVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_observacion_id")
    private DocumentoObservacion observacion;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(length = 200)
    private String actor;

    @Column(name = "actor_rol", length = 80)
    private String actorRol;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Entregable getEntregable() { return entregable; }
    public void setEntregable(Entregable entregable) { this.entregable = entregable; }

    public DocumentoVersion getVersion() { return version; }
    public void setVersion(DocumentoVersion version) { this.version = version; }

    public DocumentoObservacion getObservacion() { return observacion; }
    public void setObservacion(DocumentoObservacion observacion) { this.observacion = observacion; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getActorRol() { return actorRol; }
    public void setActorRol(String actorRol) { this.actorRol = actorRol; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
