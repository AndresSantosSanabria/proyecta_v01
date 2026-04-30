package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proyecto")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Proyecto {

    @Id
    @Column(name = "proyecto_id", length = 30, nullable = false, updatable = false)
    private String id; // Formato IS-PROY-CUN-NNN. PK manual, NO AUTOCOMPLETADA.

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 200)
    private String dependencia;

    @Column(name = "objetivo_general", columnDefinition = "TEXT")
    private String objetivoGeneral;

    @Column(name = "es_peti")
    private Boolean esPeti = false;

    @Column(name = "estrategia_peti", length = 80)
    private String estrategiaPeti;

    @Column(name = "vigencia_peti", length = 20)
    private String vigenciaPeti;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "plan_comunicaciones_pdf", length = 300)
    private String planComunicacionesPdf;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoProyecto estado = EstadoProyecto.activo;

    private Boolean cerrado = false;

    @Column(name = "viabilizacion_pdf", length = 300)
    private String viabilizacionPdf;

    @Column(name = "acta_constitucion_pdf", length = 300)
    private String actaConstitucionPdf;

    @Column(name = "cronograma_pdf", length = 300)
    private String cronogramaPdf;

    @Column(name = "avance_calculado", precision = 5, scale = 2)
    private BigDecimal avanceCalculado = BigDecimal.ZERO;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    // Relaciones LAZY para prevenir N+1 y mejorar rendimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_id")
    private Usuario gestor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Usuario director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrocinador_id")
    private Patrocinador patrocinador;

    public Proyecto() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    // Manual Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDependencia() {
        return dependencia;
    }

    public void setDependencia(String dependencia) {
        this.dependencia = dependencia;
    }

    public String getObjetivoGeneral() {
        return objetivoGeneral;
    }

    public void setObjetivoGeneral(String objetivoGeneral) {
        this.objetivoGeneral = objetivoGeneral;
    }

    public Boolean getEsPeti() {
        return esPeti;
    }

    public void setEsPeti(Boolean esPeti) {
        this.esPeti = esPeti;
    }

    public String getEstrategiaPeti() {
        return estrategiaPeti;
    }

    public void setEstrategiaPeti(String estrategiaPeti) {
        this.estrategiaPeti = estrategiaPeti;
    }

    public String getVigenciaPeti() {
        return vigenciaPeti;
    }

    public void setVigenciaPeti(String vigenciaPeti) {
        this.vigenciaPeti = vigenciaPeti;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getPlanComunicacionesPdf() {
        return planComunicacionesPdf;
    }

    public void setPlanComunicacionesPdf(String planComunicacionesPdf) {
        this.planComunicacionesPdf = planComunicacionesPdf;
    }

    public EstadoProyecto getEstado() {
        return estado;
    }

    public void setEstado(EstadoProyecto estado) {
        this.estado = estado;
    }

    public Boolean getCerrado() {
        return cerrado;
    }

    public void setCerrado(Boolean cerrado) {
        this.cerrado = cerrado;
    }

    public String getViabilizacionPdf() {
        return viabilizacionPdf;
    }

    public void setViabilizacionPdf(String viabilizacionPdf) {
        this.viabilizacionPdf = viabilizacionPdf;
    }

    public String getActaConstitucionPdf() {
        return actaConstitucionPdf;
    }

    public void setActaConstitucionPdf(String actaConstitucionPdf) {
        this.actaConstitucionPdf = actaConstitucionPdf;
    }

    public String getCronogramaPdf() {
        return cronogramaPdf;
    }

    public void setCronogramaPdf(String cronogramaPdf) {
        this.cronogramaPdf = cronogramaPdf;
    }

    public BigDecimal getAvanceCalculado() {
        return avanceCalculado;
    }

    public void setAvanceCalculado(BigDecimal avanceCalculado) {
        this.avanceCalculado = avanceCalculado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public void setGestor(Usuario gestor) {
        this.gestor = gestor;
    }

    public Usuario getDirector() {
        return director;
    }

    public void setDirector(Usuario director) {
        this.director = director;
    }

    public Patrocinador getPatrocinador() {
        return patrocinador;
    }

    public void setPatrocinador(Patrocinador patrocinador) {
        this.patrocinador = patrocinador;
    }
}