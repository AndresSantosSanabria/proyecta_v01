package com.proyecta.api_gestion.domain.model;

import com.proyecta.api_gestion.domain.model.config.EstadoEntregableConfig;
import com.proyecta.api_gestion.domain.model.enums.EstadoEntregable;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entregable")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AttributeOverride(name = "id", column = @Column(name = "entregable_id"))
@AttributeOverride(name = "nombre", column = @Column(nullable = false, length = 300))
public class Entregable extends ProyectoNodoBase {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_config_id", referencedColumnName = "estado_entregable_id", nullable = false)
    private EstadoEntregableConfig estadoConfig;

    private Boolean conforme = false;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    @Column(name = "retroactivo", nullable = false)
    private Boolean retroactivo = false;

    @Column(name = "observacion_revision", length = 1000)
    private String observacionRevision;

    @Column(name = "archivo_pdf", length = 300)
    private String archivoPdf;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hito_id")
    private Hito hito;

    public Entregable() {
        // Constructor por defecto requerido por JPA, sin lógica que ejecutar.
    }

    public void asegurarModificable() {
        if (esTerminal()) {
            throw new IllegalStateException(
                "El entregable '" + getNombre() + "' ya está en estado terminal. No se permite modificar, reemplazar o eliminar su documento."
            );
        }
    }

    public void completar(String archivoPdf, LocalDate fechaEntrega, EstadoEntregableConfig estado) {
        asegurarModificable();
        this.archivoPdf = archivoPdf;
        this.fechaEntregaReal = fechaEntrega;
        this.conforme = false;
        this.observacionRevision = null;
        this.estadoConfig = estado;
    }

    public void aprobarEvidencia(EstadoEntregableConfig estado) {
        asegurarTieneEvidencia("aprobar");

        if (estaAprobadoFinal()) {
            throw new IllegalStateException(
                "El entregable '" + getNombre() + "' ya fue aprobado. Esta accion no es reversible."
            );
        }
        this.conforme = true;
        this.observacionRevision = null;
        this.estadoConfig = estado;
    }

    public void rechazarEvidencia(String observacion, EstadoEntregableConfig estado) {
        asegurarTieneEvidencia("rechazar");

        if (estaAprobadoFinal()) {
            throw new IllegalStateException(
                "El entregable '" + getNombre() + "' ya fue aprobado. No se permite observarlo ni modificar su documento."
            );
        }
        this.conforme = false;
        this.observacionRevision = observacion;
        this.estadoConfig = estado;
    }

    public EstadoEntregable getEstado() {
        return estadoConfig != null ? EstadoEntregable.valueOf(estadoConfig.getCodigo()) : null;
    }

    public boolean estaCompletado() {
        EstadoEntregable actual = getEstado();
        return EstadoEntregable.COMPLETADO.equals(actual) || EstadoEntregable.APROBADO.equals(actual);
    }

    public boolean esConforme() {
        if (estadoConfig != null) return estadoConfig.getEsConforme();
        return estaAprobadoFinal();
    }

    public boolean esTerminal() {
        if (estaAprobadoFinal()) return true;
        if (estadoConfig != null) return estadoConfig.getEsTerminal();
        return false;
    }

    private boolean estaAprobadoFinal() {
        return EstadoEntregable.APROBADO.equals(getEstado()) || Boolean.TRUE.equals(this.conforme);
    }

    private void asegurarTieneEvidencia(String accion) {
        if (this.archivoPdf == null || this.archivoPdf.isBlank()) {
            throw new IllegalStateException("No se puede " + accion + " un entregable sin evidencia cargada.");
        }
    }

    public String getEstadoCodigo() {
        return estadoConfig != null ? estadoConfig.getCodigo() : null;
    }

    // Getters and Setters
    public Boolean getConforme() { return conforme; }
    public void setConforme(Boolean conforme) { this.conforme = conforme; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }

    public LocalDate getFechaEntregaReal() { return fechaEntregaReal; }
    public void setFechaEntregaReal(LocalDate fechaEntregaReal) { this.fechaEntregaReal = fechaEntregaReal; }

    public Boolean getRetroactivo() { return retroactivo; }
    public void setRetroactivo(Boolean retroactivo) { this.retroactivo = retroactivo; }

    /**
     * Los entregables registrados durante la completitud inicial con una fecha limite historica
     * se reportan como entregados el mismo dia de su fecha limite.
     */
    public LocalDate getFechaEntregaEfectiva() {
        return Boolean.TRUE.equals(retroactivo) && fechaLimite != null ? fechaLimite : fechaEntregaReal;
    }

    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }

    public String getArchivoPdf() { return archivoPdf; }
    public void setArchivoPdf(String archivoPdf) { this.archivoPdf = archivoPdf; }

    public Hito getHito() { return hito; }
    public void setHito(Hito hito) { this.hito = hito; }

    public EstadoEntregableConfig getEstadoConfig() { return estadoConfig; }
    public void setEstadoConfig(EstadoEntregableConfig estadoConfig) { this.estadoConfig = estadoConfig; }
}
