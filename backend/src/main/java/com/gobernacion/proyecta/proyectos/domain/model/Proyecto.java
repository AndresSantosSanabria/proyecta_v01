package com.gobernacion.proyecta.proyectos.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad de Dominio Pura: Proyecto.
 * Representa la lógica central de un proyecto TIC sin dependencias de frameworks.
 */
public class Proyecto {
    private String id;
    private String nombre;
    private String dependencia;
    private String director;
    private String correoDirector;
    private String objetivoGeneral;
    private LocalDate fechaInicio;
    private Boolean peti;
    private String vigenciaPeti;
    private String estrategiaPeti; // Simplificado a String o enum de dominio
    private Boolean tienePlanComunicaciones;
    private String cronogramaPdf;
    private String actaConstitucionPdf;
    private String planComunicacionesPdf;
    private String viabilizacionPdf;
    private String estado; // Simplificado a String o enum de dominio
    private BigDecimal avanceTotal;
    private LocalDateTime fechaRegistro;

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getCorreoDirector() { return correoDirector; }
    public void setCorreoDirector(String correoDirector) { this.correoDirector = correoDirector; }
    public String getObjetivoGeneral() { return objetivoGeneral; }
    public void setObjetivoGeneral(String objetivoGeneral) { this.objetivoGeneral = objetivoGeneral; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public Boolean getPeti() { return peti; }
    public void setPeti(Boolean peti) { this.peti = peti; }
    public String getVigenciaPeti() { return vigenciaPeti; }
    public void setVigenciaPeti(String vigenciaPeti) { this.vigenciaPeti = vigenciaPeti; }
    public String getEstrategiaPeti() { return estrategiaPeti; }
    public void setEstrategiaPeti(String estrategiaPeti) { this.estrategiaPeti = estrategiaPeti; }
    public Boolean getTienePlanComunicaciones() { return tienePlanComunicaciones; }
    public void setTienePlanComunicaciones(Boolean tienePlanComunicaciones) { this.tienePlanComunicaciones = tienePlanComunicaciones; }
    public String getCronogramaPdf() { return cronogramaPdf; }
    public void setCronogramaPdf(String cronogramaPdf) { this.cronogramaPdf = cronogramaPdf; }
    public String getActaConstitucionPdf() { return actaConstitucionPdf; }
    public void setActaConstitucionPdf(String actaConstitucionPdf) { this.actaConstitucionPdf = actaConstitucionPdf; }
    public String getPlanComunicacionesPdf() { return planComunicacionesPdf; }
    public void setPlanComunicacionesPdf(String planComunicacionesPdf) { this.planComunicacionesPdf = planComunicacionesPdf; }
    public String getViabilizacionPdf() { return viabilizacionPdf; }
    public void setViabilizacionPdf(String viabilizacionPdf) { this.viabilizacionPdf = viabilizacionPdf; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getAvanceTotal() { return avanceTotal; }
    public void setAvanceTotal(BigDecimal avanceTotal) { this.avanceTotal = avanceTotal; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // Lógica de Dominio
    public boolean esAptoParaCierre() {
        return this.avanceTotal != null && this.avanceTotal.compareTo(new BigDecimal("100.00")) >= 0;
    }

    public void cerrar() {
        if (!esAptoParaCierre()) {
            throw new IllegalStateException("No se puede cerrar un proyecto que no ha alcanzado el 100% de avance.");
        }
        this.estado = "CERRADO";
    }
}
