package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proyecto")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Proyecto {

    @Id
    @Column(name = "proyecto_id", length = 30, nullable = false, updatable = false)
    private String id; // Formato IS-PROY-CUN-NNN.

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

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObjetivoEspecifico> objetivosEspecificos = new ArrayList<>();

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "es_peti")
    private Boolean peti = false;

    @Column(name = "vigencia_peti", length = 20)
    private String vigenciaPeti;

    @Enumerated(EnumType.STRING)
    @Column(name = "estrategia_peti", length = 80)
    private EstrategiaPeti estrategiaPeti;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoProyecto estado = EstadoProyecto.ACTIVO;

    @Column(name = "avance_total", precision = 5, scale = 2)
    private BigDecimal avanceTotal = BigDecimal.ZERO;

    @Embedded
    private Furag furag;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "patrocinador_id")
    private Patrocinador patrocinador;

    @ElementCollection
    @CollectionTable(name = "proyecto_equipo", joinColumns = @JoinColumn(name = "proyecto_id"))
    private List<MiembroEquipo> equipoTrabajo = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fase> fases = new ArrayList<>();

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    public Proyecto() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    // Manual Getters and Setters
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

    public List<ObjetivoEspecifico> getObjetivosEspecificos() { return objetivosEspecificos; }
    public void setObjetivosEspecificos(List<ObjetivoEspecifico> objetivosEspecificos) { this.objetivosEspecificos = objetivosEspecificos; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public Boolean getPeti() { return peti; }
    public void setPeti(Boolean peti) { this.peti = peti; }

    public String getVigenciaPeti() { return vigenciaPeti; }
    public void setVigenciaPeti(String vigenciaPeti) { this.vigenciaPeti = vigenciaPeti; }

    public EstrategiaPeti getEstrategiaPeti() { return estrategiaPeti; }
    public void setEstrategiaPeti(EstrategiaPeti estrategiaPeti) { this.estrategiaPeti = estrategiaPeti; }

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

    public EstadoProyecto getEstado() { return estado; }
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }

    public BigDecimal getAvanceTotal() { return avanceTotal; }
    public void setAvanceTotal(BigDecimal avanceTotal) { this.avanceTotal = avanceTotal; }

    public Furag getFurag() { return furag; }
    public void setFurag(Furag furag) { this.furag = furag; }

    public Patrocinador getPatrocinador() { return patrocinador; }
    public void setPatrocinador(Patrocinador patrocinador) { this.patrocinador = patrocinador; }

    public List<MiembroEquipo> getEquipoTrabajo() { return equipoTrabajo; }
    public void setEquipoTrabajo(List<MiembroEquipo> equipoTrabajo) { this.equipoTrabajo = equipoTrabajo; }

    public List<Fase> getFases() { return fases; }
    public void setFases(List<Fase> fases) { this.fases = fases; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // --- Lógica de Negocio (Rich Domain Model) ---

    /**
     * Valida si el proyecto cumple con las condiciones para ser cerrado formalmente.
     * Requisito: Avance al 100%.
     */
    public boolean esAptoParaCierre() {
        return this.avanceTotal != null && this.avanceTotal.compareTo(new BigDecimal("100.00")) >= 0;
    }

    /**
     * Cambia el estado del proyecto a CERRADO si cumple las condiciones.
     */
    public void cerrar() {
        if (!esAptoParaCierre()) {
            throw new IllegalStateException("No se puede cerrar un proyecto que no ha alcanzado el 100% de avance.");
        }
        this.estado = EstadoProyecto.CERRADO;
    }
}