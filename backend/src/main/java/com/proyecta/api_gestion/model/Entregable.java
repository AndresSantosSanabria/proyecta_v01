package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.config.EstadoEntregableConfig;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregable")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Entregable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entregable_id")
    private Integer id;

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoEntregable estado = EstadoEntregable.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_config_id", referencedColumnName = "estado_entregable_id")
    private EstadoEntregableConfig estadoConfig;

    private Boolean conforme = false;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    @Column(name = "archivo_pdf", length = 300)
    private String archivoPdf;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hito_id")
    private Hito hito;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    public Entregable() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public void asegurarModificable() {
        if (esTerminal()) {
            throw new IllegalStateException(
                "El entregable '" + this.nombre + "' ya está en estado terminal. No se permite modificar, reemplazar o eliminar su documento."
            );
        }
    }

    public void completar(String archivoPdf, LocalDate fechaEntrega) {
        completar(archivoPdf, fechaEntrega, true);
    }

    public void completar(String archivoPdf, LocalDate fechaEntrega, boolean esConforme) {
        asegurarModificable();
        this.archivoPdf = archivoPdf;
        this.fechaEntregaReal = fechaEntrega;
        this.conforme = esConforme;
        this.estadoConfig = null;
        this.estado = esConforme ? EstadoEntregable.A_CONFORMIDAD : EstadoEntregable.COMPLETADO;
    }

    public boolean estaCompletado() {
        return EstadoEntregable.COMPLETADO.equals(this.estado) || EstadoEntregable.A_CONFORMIDAD.equals(this.estado);
    }

    public boolean esConforme() {
        if (estadoConfig != null) return estadoConfig.getEsConforme();
        return EstadoEntregable.A_CONFORMIDAD.equals(this.estado) || Boolean.TRUE.equals(this.conforme);
    }

    public boolean esTerminal() {
        if (estadoConfig != null) return estadoConfig.getEsTerminal();
        return EstadoEntregable.COMPLETADO.equals(this.estado) || EstadoEntregable.A_CONFORMIDAD.equals(this.estado);
    }

    public String getEstadoCodigo() {
        if (estadoConfig != null) return estadoConfig.getCodigo();
        if (estado != null) return estado.name();
        return null;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }

    public EstadoEntregable getEstado() { return estado; }
    public void setEstado(EstadoEntregable estado) { this.estado = estado; }

    public Boolean getConforme() { return conforme; }
    public void setConforme(Boolean conforme) { this.conforme = conforme; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public LocalDate getFechaEntregaReal() { return fechaEntregaReal; }
    public void setFechaEntregaReal(LocalDate fechaEntregaReal) { this.fechaEntregaReal = fechaEntregaReal; }

    public String getArchivoPdf() { return archivoPdf; }
    public void setArchivoPdf(String archivoPdf) { this.archivoPdf = archivoPdf; }

    public Hito getHito() { return hito; }
    public void setHito(Hito hito) { this.hito = hito; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public EstadoEntregableConfig getEstadoConfig() { return estadoConfig; }
    public void setEstadoConfig(EstadoEntregableConfig estadoConfig) { this.estadoConfig = estadoConfig; }
}
