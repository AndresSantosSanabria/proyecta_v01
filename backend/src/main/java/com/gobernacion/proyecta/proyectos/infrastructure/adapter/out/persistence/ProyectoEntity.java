package com.gobernacion.proyecta.proyectos.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA para persistencia. Separada del modelo de dominio.
 */
@Entity
@Table(name = "proyecto")
public class ProyectoEntity {
    @Id
    @Column(name = "proyecto_id", length = 30)
    private String id;

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 200)
    private String dependencia;

    @Column(name = "director_nombre", length = 120)
    private String director;

    @Column(name = "director_correo", length = 200)
    private String correoDirector;

    @Column(name = "objetivo_general", columnDefinition = "TEXT")
    private String objetivoGeneral;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "es_peti")
    private Boolean peti = false;

    @Column(name = "vigencia_peti", length = 20)
    private String vigenciaPeti;

    @Column(name = "estrategia_peti", length = 80)
    private String estrategiaPeti;

    @Column(name = "tiene_plan_comunicaciones")
    private Boolean tienePlanComunicaciones = false;

    @Column(name = "cronograma_pdf", length = 255)
    private String cronogramaPdf;

    @Column(name = "acta_constitucion_pdf", length = 255)
    private String actaConstitucionPdf;

    @Column(name = "plan_comunicaciones_pdf", length = 255)
    private String planComunicacionesPdf;

    @Column(name = "viabilizacion_pdf", length = 255)
    private String viabilizacionPdf;

    @Column(length = 20)
    private String estado;

    @Column(name = "avance_total", precision = 5, scale = 2)
    private BigDecimal avanceTotal = BigDecimal.ZERO;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

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
}
