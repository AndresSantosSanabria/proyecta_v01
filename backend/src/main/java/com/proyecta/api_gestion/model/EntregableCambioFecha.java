package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregable_cambio_fecha")
public class EntregableCambioFecha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @Column(name = "fecha_anterior", nullable = false)
    private LocalDate fechaAnterior;

    @Column(name = "fecha_nueva", nullable = false)
    private LocalDate fechaNueva;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "archivo_pdf", length = 300, nullable = false)
    private String archivoPdf;

    @Column(name = "nombre_original", length = 300)
    private String nombreOriginal;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(name = "usuario_rol", length = 50)
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

    public LocalDate getFechaAnterior() { return fechaAnterior; }
    public void setFechaAnterior(LocalDate fechaAnterior) { this.fechaAnterior = fechaAnterior; }

    public LocalDate getFechaNueva() { return fechaNueva; }
    public void setFechaNueva(LocalDate fechaNueva) { this.fechaNueva = fechaNueva; }

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
