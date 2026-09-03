package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregable_cambio_descripcion")
public class EntregableCambioDescripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @Column(name = "descripcion_anterior", columnDefinition = "TEXT")
    private String descripcionAnterior;

    @Column(name = "descripcion_nueva", columnDefinition = "TEXT", nullable = false)
    private String descripcionNueva;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justificacion;

    @Column(name = "archivo_pdf", columnDefinition = "TEXT", nullable = false)
    private String archivoPdf;

    @Column(name = "nombre_original", length = 300)
    private String nombreOriginal;

    @Column(nullable = false, length = 255)
    private String usuario;

    @Column(name = "usuario_rol", length = 500)
    private String usuarioRol;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void onCreate() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Entregable getEntregable() { return entregable; }
    public void setEntregable(Entregable entregable) { this.entregable = entregable; }

    public String getDescripcionAnterior() { return descripcionAnterior; }
    public void setDescripcionAnterior(String descripcionAnterior) { this.descripcionAnterior = descripcionAnterior; }

    public String getDescripcionNueva() { return descripcionNueva; }
    public void setDescripcionNueva(String descripcionNueva) { this.descripcionNueva = descripcionNueva; }

    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }

    public String getArchivoPdf() { return archivoPdf; }
    public void setArchivoPdf(String archivoPdf) { this.archivoPdf = archivoPdf; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String usuarioRol) { this.usuarioRol = usuarioRol; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
