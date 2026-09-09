package com.proyecta.api_gestion.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_audit_log", schema = "proyecta_db")
public class SystemAuditLog {

    @Id
    private UUID id;

    @Column(name = "usuario_id", length = 100)
    private String usuarioId;

    @Column(name = "usuario_nombre", length = 255)
    private String usuarioNombre;

    @Column(name = "usuario_rol", length = 100)
    private String usuarioRol;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "modulo", nullable = false, length = 150)
    private String modulo;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "recurso", length = 255)
    private String recurso;

    @Column(name = "codigo_estado", nullable = false)
    private Integer codigoEstado;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "traza_error", columnDefinition = "TEXT")
    private String trazaError;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "entidad_tipo", length = 100)
    private String entidadTipo;

    @Column(name = "entidad_id", length = 100)
    private String entidadId;

    @Column(name = "respuesta_body", columnDefinition = "TEXT")
    private String respuestaBody;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(name = "eliminado", nullable = false)
    private Boolean eliminado = Boolean.FALSE;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (eliminado == null) {
            eliminado = Boolean.FALSE;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String usuarioRol) { this.usuarioRol = usuarioRol; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getMetodoHttp() { return metodoHttp; }
    public void setMetodoHttp(String metodoHttp) { this.metodoHttp = metodoHttp; }

    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }

    public Integer getCodigoEstado() { return codigoEstado; }
    public void setCodigoEstado(Integer codigoEstado) { this.codigoEstado = codigoEstado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getTrazaError() { return trazaError; }
    public void setTrazaError(String trazaError) { this.trazaError = trazaError; }

    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public String getEntidadTipo() { return entidadTipo; }
    public void setEntidadTipo(String entidadTipo) { this.entidadTipo = entidadTipo; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public String getRespuestaBody() { return respuestaBody; }
    public void setRespuestaBody(String respuestaBody) { this.respuestaBody = respuestaBody; }

    public Long getDuracionMs() { return duracionMs; }
    public void setDuracionMs(Long duracionMs) { this.duracionMs = duracionMs; }

    public Boolean getEliminado() { return eliminado; }
    public void setEliminado(Boolean eliminado) { this.eliminado = eliminado; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}