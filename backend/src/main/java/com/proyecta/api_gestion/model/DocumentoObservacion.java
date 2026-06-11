package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.enums.DocumentoObservacionEstado;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_observacion")
public class DocumentoObservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_observacion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_version_id")
    private DocumentoVersion version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentoObservacionEstado estado = DocumentoObservacionEstado.ABIERTA;

    @Column(name = "creada_por", length = 200)
    private String creadaPor;

    @Column(name = "creada_rol", length = 80)
    private String creadaRol;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "subsanada_por", length = 200)
    private String subsanadaPor;

    @Column(name = "subsanada_en")
    private LocalDateTime subsanadaEn;

    @Column(name = "comentario_subsanacion", length = 1000)
    private String comentarioSubsanacion;

    @Column(name = "cerrada_por", length = 200)
    private String cerradaPor;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @PrePersist
    void onCreate() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Entregable getEntregable() { return entregable; }
    public void setEntregable(Entregable entregable) { this.entregable = entregable; }

    public DocumentoVersion getVersion() { return version; }
    public void setVersion(DocumentoVersion version) { this.version = version; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public DocumentoObservacionEstado getEstado() { return estado; }
    public void setEstado(DocumentoObservacionEstado estado) { this.estado = estado; }

    public String getCreadaPor() { return creadaPor; }
    public void setCreadaPor(String creadaPor) { this.creadaPor = creadaPor; }

    public String getCreadaRol() { return creadaRol; }
    public void setCreadaRol(String creadaRol) { this.creadaRol = creadaRol; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public String getSubsanadaPor() { return subsanadaPor; }
    public void setSubsanadaPor(String subsanadaPor) { this.subsanadaPor = subsanadaPor; }

    public LocalDateTime getSubsanadaEn() { return subsanadaEn; }
    public void setSubsanadaEn(LocalDateTime subsanadaEn) { this.subsanadaEn = subsanadaEn; }

    public String getComentarioSubsanacion() { return comentarioSubsanacion; }
    public void setComentarioSubsanacion(String comentarioSubsanacion) { this.comentarioSubsanacion = comentarioSubsanacion; }

    public String getCerradaPor() { return cerradaPor; }
    public void setCerradaPor(String cerradaPor) { this.cerradaPor = cerradaPor; }

    public LocalDateTime getCerradaEn() { return cerradaEn; }
    public void setCerradaEn(LocalDateTime cerradaEn) { this.cerradaEn = cerradaEn; }
}
