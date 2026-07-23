package com.proyecta.api_gestion.model.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_permiso", schema = "proyecta_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "permiso_id"}))
public class SeguridadUsuarioPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private SeguridadUsuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_id", nullable = false)
    private SeguridadPermiso permiso;

    @Column(name = "concedido", nullable = false)
    private Boolean concedido = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (fechaCreacion == null) fechaCreacion = now;
        if (fechaModificacion == null) fechaModificacion = now;
    }

    @PreUpdate
    void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SeguridadUsuario getUsuario() { return usuario; }
    public void setUsuario(SeguridadUsuario usuario) { this.usuario = usuario; }

    public SeguridadPermiso getPermiso() { return permiso; }
    public void setPermiso(SeguridadPermiso permiso) { this.permiso = permiso; }

    public Boolean getConcedido() { return concedido; }
    public void setConcedido(Boolean concedido) { this.concedido = concedido; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
