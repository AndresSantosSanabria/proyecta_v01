package com.proyecta.api_gestion.domain.model;

import com.proyecta.api_gestion.domain.model.enums.DocumentoPreWizardEstado;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "documento_pre_wizard_revision")
@AttributeOverride(name = "id", column = @Column(name = "documento_pre_wizard_revision_id"))
public class DocumentoPreWizardRevision extends DocumentoReferenciaBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private DocumentoPreWizardEstado estado = DocumentoPreWizardEstado.PENDIENTE;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "revisado_por", length = 200)
    private String revisadoPor;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (creadoEn == null) {
            creadoEn = now;
        }
        actualizadoEn = now;
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.systemDefault());
    }

    public DocumentoPreWizardEstado getEstado() { return estado; }
    public void setEstado(DocumentoPreWizardEstado estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }

    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public void setRevisadoEn(LocalDateTime revisadoEn) { this.revisadoEn = revisadoEn; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
