package com.gobernacion.proyecta.entregables.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Entregable {
    private Integer id;
    private String nombre;
    private BigDecimal ponderacion;
    private String estado;
    private Boolean conforme;
    private LocalDate fechaLimite;
    private LocalDate fechaEntregaReal;
    private String archivoPdf;
    private Integer hitoId;
    private LocalDateTime fechaCreacion;

    public Entregable() {
        this.estado = "PENDIENTE";
        this.conforme = false;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Boolean getConforme() { return conforme; }
    public void setConforme(Boolean conforme) { this.conforme = conforme; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public LocalDate getFechaEntregaReal() { return fechaEntregaReal; }
    public void setFechaEntregaReal(LocalDate fechaEntregaReal) { this.fechaEntregaReal = fechaEntregaReal; }
    public String getArchivoPdf() { return archivoPdf; }
    public void setArchivoPdf(String archivoPdf) { this.archivoPdf = archivoPdf; }
    public Integer getHitoId() { return hitoId; }
    public void setHitoId(Integer hitoId) { this.hitoId = hitoId; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public void asegurarModificable() {
        if ("COMPLETADO".equals(this.estado)) {
            throw new IllegalStateException(
                "El entregable '" + this.nombre + "' ya está COMPLETADO. No se permite modificar, reemplazar o eliminar su documento."
            );
        }
    }

    public void completar(String archivoPdf, LocalDate fechaEntrega) {
        asegurarModificable();
        this.estado = "COMPLETADO";
        this.conforme = true;
        this.archivoPdf = archivoPdf;
        this.fechaEntregaReal = fechaEntrega;
    }

    public boolean estaCompletado() {
        return "COMPLETADO".equals(this.estado);
    }
}
