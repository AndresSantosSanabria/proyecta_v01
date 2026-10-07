package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "documento_auditoria")
@AttributeOverride(name = "id", column = @Column(name = "documento_auditoria_id"))
public class DocumentoAuditoria extends DocumentoTrazabilidadBase {

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
            fecha = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

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
