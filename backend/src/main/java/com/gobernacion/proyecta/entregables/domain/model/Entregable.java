package com.gobernacion.proyecta.entregables.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad de Dominio Pura: Entregable.
 */
public class Entregable {
    private Integer id;
    private String nombre;
    private BigDecimal ponderacion;
    private String estado;
    private Boolean conforme;
    private LocalDate fechaLimite;
    private LocalDate fechaEntregaReal;
    private String archivoPdf;
    private Integer hitoId; // Referencia por ID para desacoplar del modelo de Hito por ahora
    private LocalDateTime fechaCreacion;

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

    // Lógica de dominio
    public void marcarConforme() {
        this.conforme = true;
        this.estado = "CONFORME";
        this.fechaEntregaReal = LocalDate.now();
    }
}
