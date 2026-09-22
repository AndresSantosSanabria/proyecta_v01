package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.config.EstadoProyectoConfig;
import com.proyecta.api_gestion.model.config.EstrategiaPetiConfig;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
import com.proyecta.api_gestion.model.enums.ViabilidadEstado;
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
    @Column(name = "proyecto_id", length = 40, nullable = false, updatable = false)
    private String id; // Formato PROY-CUN-YYYY-NNN.

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 200)
    private String dependencia;

    @Column(name = "director_nombre", length = 120)
    private String director;

    @Column(name = "director_correo", length = 200)
    private String correoDirector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_usuario_id", referencedColumnName = "id")
    private com.proyecta.api_gestion.model.security.SeguridadUsuario directorUsuario;

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
    @Column(name = "viabilidad_estado", length = 20)
    private ViabilidadEstado viabilidadEstado = ViabilidadEstado.PENDIENTE;

    @Column(name = "viabilidad_observaciones", columnDefinition = "TEXT")
    private String viabilidadObservaciones;

    @Column(name = "viabilidad_revisado_por", length = 120)
    private String viabilidadRevisadoPor;

    @Column(name = "viabilidad_revisado_en")
    private LocalDateTime viabilidadRevisadoEn;

    @Column(name = "documentos_cargados", nullable = false)
    private Boolean documentosCargados = false;

    @Column(name = "documentos_verificados", nullable = false)
    private Boolean documentosVerificados = false;

    @Column(name = "fecha_verificacion_documentos")
    private LocalDateTime fechaVerificacionDocumentos;

    @Column(name = "fecha_limite_completar")
    private LocalDate fechaLimiteCompletar;

    @Column(name = "cierre_forzoso", nullable = false)
    private Boolean cierreForzoso = false;

    @Column(name = "cierre_forzoso_por", length = 120)
    private String cierreForzosoPor;

    @Column(name = "cierre_forzoso_en")
    private LocalDateTime cierreForzosoEn;

    @Column(name = "acta_constitucion_cargada", nullable = false)
    private Boolean actaConstitucionCargada = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoProyecto estado = EstadoProyecto.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_config_id", referencedColumnName = "estado_proyecto_id")
    private EstadoProyectoConfig estadoConfig;

    @Column(name = "avance_total", precision = 5, scale = 2)
    private BigDecimal avanceTotal = BigDecimal.ZERO;

    @Column(name = "cierre_solicitado", nullable = false)
    private Boolean cierreSolicitado = false;

    @Column(name = "cierre_solicitado_en")
    private LocalDateTime cierreSolicitadoEn;

    @Column(name = "cierre_solicitado_por", length = 120)
    private String cierreSolicitadoPor;

    @Column(name = "cierre_estado", length = 30)
    private String cierreEstado;

    @Column(name = "cierre_observaciones", columnDefinition = "TEXT")
    private String cierreObservaciones;

    @Column(name = "cierre_borrador_json", columnDefinition = "TEXT")
    private String cierreBorradorJson;

    @Column(name = "completitud_borrador_json", columnDefinition = "TEXT")
    private String completitudBorradorJson;

    @Column(name = "completitud_fases_completadas", columnDefinition = "TEXT")
    private String completitudFasesCompletadas;

    @Embedded
    private Furag furag;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "patrocinador_id")
    private Patrocinador patrocinador;

    @ElementCollection
    @CollectionTable(name = "proyecto_equipo", joinColumns = @JoinColumn(name = "proyecto_id"))
    private List<MiembroEquipo> equipoTrabajo = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "proyecto_stakeholder", joinColumns = @JoinColumn(name = "proyecto_id"))
    private List<Stakeholder> stakeholders = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fase> fases = new ArrayList<>();

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "requiere_completitud_director", nullable = false)
    private Boolean requiereCompletitudDirector = false;

    @Column(name = "primer_ingreso_director_at")
    private LocalDateTime primerIngresoDirectorAt;

    @Column(name = "completado_por_director_at")
    private LocalDateTime completadoPorDirectorAt;

    @Column(name = "registrado_inicial_por", length = 120)
    private String registradoInicialPor;

    @Column(name = "alcance_detallado", columnDefinition = "TEXT")
    private String alcanceDetallado;

    @Column(name = "presupuesto_estimado", precision = 18, scale = 2)
    private BigDecimal presupuestoEstimado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estrategia_peti_config_id", referencedColumnName = "estrategia_peti_id")
    private EstrategiaPetiConfig estrategiaPetiConfig;

    @Column(name = "email_message_id", length = 255)
    private String emailMessageId;

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

    public com.proyecta.api_gestion.model.security.SeguridadUsuario getDirectorUsuario() { return directorUsuario; }
    public void setDirectorUsuario(com.proyecta.api_gestion.model.security.SeguridadUsuario directorUsuario) { this.directorUsuario = directorUsuario; }

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

    public ViabilidadEstado getViabilidadEstado() { return viabilidadEstado; }
    public void setViabilidadEstado(ViabilidadEstado viabilidadEstado) { this.viabilidadEstado = viabilidadEstado; }

    public String getViabilidadObservaciones() { return viabilidadObservaciones; }
    public void setViabilidadObservaciones(String viabilidadObservaciones) { this.viabilidadObservaciones = viabilidadObservaciones; }

    public String getViabilidadRevisadoPor() { return viabilidadRevisadoPor; }
    public void setViabilidadRevisadoPor(String viabilidadRevisadoPor) { this.viabilidadRevisadoPor = viabilidadRevisadoPor; }

    public LocalDateTime getViabilidadRevisadoEn() { return viabilidadRevisadoEn; }
    public void setViabilidadRevisadoEn(LocalDateTime viabilidadRevisadoEn) { this.viabilidadRevisadoEn = viabilidadRevisadoEn; }

    public Boolean getDocumentosCargados() { return documentosCargados; }
    public void setDocumentosCargados(Boolean documentosCargados) { this.documentosCargados = documentosCargados; }

    public Boolean getDocumentosVerificados() { return documentosVerificados; }
    public void setDocumentosVerificados(Boolean documentosVerificados) { this.documentosVerificados = documentosVerificados; }

    public LocalDateTime getFechaVerificacionDocumentos() { return fechaVerificacionDocumentos; }
    public void setFechaVerificacionDocumentos(LocalDateTime fechaVerificacionDocumentos) { this.fechaVerificacionDocumentos = fechaVerificacionDocumentos; }

    public LocalDate getFechaLimiteCompletar() { return fechaLimiteCompletar; }
    public void setFechaLimiteCompletar(LocalDate fechaLimiteCompletar) { this.fechaLimiteCompletar = fechaLimiteCompletar; }

    public Boolean getCierreForzoso() { return cierreForzoso; }
    public void setCierreForzoso(Boolean cierreForzoso) { this.cierreForzoso = cierreForzoso; }

    public String getCierreForzosoPor() { return cierreForzosoPor; }
    public void setCierreForzosoPor(String cierreForzosoPor) { this.cierreForzosoPor = cierreForzosoPor; }

    public LocalDateTime getCierreForzosoEn() { return cierreForzosoEn; }
    public void setCierreForzosoEn(LocalDateTime cierreForzosoEn) { this.cierreForzosoEn = cierreForzosoEn; }

    public Boolean getActaConstitucionCargada() { return actaConstitucionCargada; }
    public void setActaConstitucionCargada(Boolean actaConstitucionCargada) { this.actaConstitucionCargada = actaConstitucionCargada; }

    public EstadoProyecto getEstado() { return estado; }
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }

    public BigDecimal getAvanceTotal() { return avanceTotal; }
    public void setAvanceTotal(BigDecimal avanceTotal) { this.avanceTotal = avanceTotal; }

    public Boolean getCierreSolicitado() { return cierreSolicitado; }
    public void setCierreSolicitado(Boolean cierreSolicitado) { this.cierreSolicitado = cierreSolicitado; }

    public LocalDateTime getCierreSolicitadoEn() { return cierreSolicitadoEn; }
    public void setCierreSolicitadoEn(LocalDateTime cierreSolicitadoEn) { this.cierreSolicitadoEn = cierreSolicitadoEn; }

    public String getCierreSolicitadoPor() { return cierreSolicitadoPor; }
    public void setCierreSolicitadoPor(String cierreSolicitadoPor) { this.cierreSolicitadoPor = cierreSolicitadoPor; }

    public String getCierreEstado() { return cierreEstado; }
    public void setCierreEstado(String cierreEstado) { this.cierreEstado = cierreEstado; }

    public String getCierreObservaciones() { return cierreObservaciones; }
    public void setCierreObservaciones(String cierreObservaciones) { this.cierreObservaciones = cierreObservaciones; }

    public String getCierreBorradorJson() { return cierreBorradorJson; }
    public void setCierreBorradorJson(String cierreBorradorJson) { this.cierreBorradorJson = cierreBorradorJson; }

    public String getCompletitudBorradorJson() { return completitudBorradorJson; }
    public void setCompletitudBorradorJson(String completitudBorradorJson) { this.completitudBorradorJson = completitudBorradorJson; }

    public String getCompletitudFasesCompletadas() { return completitudFasesCompletadas; }
    public void setCompletitudFasesCompletadas(String completitudFasesCompletadas) { this.completitudFasesCompletadas = completitudFasesCompletadas; }

    public Furag getFurag() { return furag; }
    public void setFurag(Furag furag) { this.furag = furag; }

    public Patrocinador getPatrocinador() { return patrocinador; }
    public void setPatrocinador(Patrocinador patrocinador) { this.patrocinador = patrocinador; }

    public List<MiembroEquipo> getEquipoTrabajo() { return equipoTrabajo; }
    public void setEquipoTrabajo(List<MiembroEquipo> equipoTrabajo) { this.equipoTrabajo = equipoTrabajo; }

    public List<Stakeholder> getStakeholders() { return stakeholders; }
    public void setStakeholders(List<Stakeholder> stakeholders) { this.stakeholders = stakeholders; }

    public List<Fase> getFases() { return fases; }
    public void setFases(List<Fase> fases) { this.fases = fases; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Boolean getRequiereCompletitudDirector() { return requiereCompletitudDirector; }
    public void setRequiereCompletitudDirector(Boolean requiereCompletitudDirector) { this.requiereCompletitudDirector = requiereCompletitudDirector; }

    public LocalDateTime getPrimerIngresoDirectorAt() { return primerIngresoDirectorAt; }
    public void setPrimerIngresoDirectorAt(LocalDateTime primerIngresoDirectorAt) { this.primerIngresoDirectorAt = primerIngresoDirectorAt; }

    public LocalDateTime getCompletadoPorDirectorAt() { return completadoPorDirectorAt; }
    public void setCompletadoPorDirectorAt(LocalDateTime completadoPorDirectorAt) { this.completadoPorDirectorAt = completadoPorDirectorAt; }

    public String getRegistradoInicialPor() { return registradoInicialPor; }
    public void setRegistradoInicialPor(String registradoInicialPor) { this.registradoInicialPor = registradoInicialPor; }

    public String getAlcanceDetallado() { return alcanceDetallado; }
    public void setAlcanceDetallado(String alcanceDetallado) { this.alcanceDetallado = alcanceDetallado; }

    public BigDecimal getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(BigDecimal presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public EstadoProyectoConfig getEstadoConfig() { return estadoConfig; }
    public void setEstadoConfig(EstadoProyectoConfig estadoConfig) { this.estadoConfig = estadoConfig; }

    public EstrategiaPetiConfig getEstrategiaPetiConfig() { return estrategiaPetiConfig; }
    public void setEstrategiaPetiConfig(EstrategiaPetiConfig estrategiaPetiConfig) { this.estrategiaPetiConfig = estrategiaPetiConfig; }

    public String getEmailMessageId() { return emailMessageId; }
    public void setEmailMessageId(String emailMessageId) { this.emailMessageId = emailMessageId; }

    public String getEstadoCodigo() {
        if (estadoConfig != null) return estadoConfig.getCodigo();
        if (estado != null) return estado.name();
        return null;
    }

    public boolean esEstadoTerminal() {
        if (estadoConfig != null) return estadoConfig.getEsTerminal();
        return EstadoProyecto.CERRADO.equals(estado) || EstadoProyecto.CERRADO_FORZOSO.equals(estado);
    }

    public boolean requiereCompletitudDirector() {
        return Boolean.TRUE.equals(requiereCompletitudDirector);
    }

    public void marcarRegistroInicialPendiente(String username) {
        this.estado = EstadoProyecto.PENDIENTE_COMPLETAR;
        this.requiereCompletitudDirector = true;
        this.registradoInicialPor = username;
    }

    public void registrarPrimerIngresoDirector() {
        if (this.primerIngresoDirectorAt == null) {
            this.primerIngresoDirectorAt = LocalDateTime.now();
        }
    }

    public void completarInformacionInicialPorDirector() {
        if (this.fechaInicio != null && !this.fechaInicio.isAfter(LocalDate.now())) {
            this.estado = EstadoProyecto.ACTIVO;
        } else {
            this.estado = EstadoProyecto.PLANIFICACION;
        }
        this.requiereCompletitudDirector = false;
        this.completadoPorDirectorAt = LocalDateTime.now();
    }

    // --- Lógica de Negocio (Rich Domain Model) ---

    /**
     * Valida si el proyecto cumple con las condiciones para ser cerrado formalmente.
     * Requisito: Avance al 100%.
     */
    public boolean esAptoParaCierre() {
        return this.avanceTotal != null && this.avanceTotal.compareTo(new BigDecimal("100.00")) >= 0;
    }

    public boolean cierrePendienteRevision() {
        return Boolean.TRUE.equals(cierreSolicitado) && (cierreEstado == null || "PENDIENTE".equals(cierreEstado));
    }

    public boolean cierreRechazado() {
        return "RECHAZADO".equals(cierreEstado);
    }

    public boolean cierreAprobado() {
        return "APROBADO".equals(cierreEstado);
    }

    // --- Quality Gate: Documentos Pre-Wizard ---

    public boolean viabilidadPendiente() {
        return ViabilidadEstado.PENDIENTE.equals(this.viabilidadEstado);
    }

    public boolean viabilidadCargada() {
        return ViabilidadEstado.CARGADA.equals(this.viabilidadEstado);
    }

    public boolean viabilidadAprobada() {
        return ViabilidadEstado.APROBADA.equals(this.viabilidadEstado);
    }

    public boolean viabilidadDevuelta() {
        return ViabilidadEstado.DEVUELTA.equals(this.viabilidadEstado);
    }

    public boolean documentosPreWizardCompletos() {
        return Boolean.TRUE.equals(documentosCargados) && Boolean.TRUE.equals(documentosVerificados);
    }

    public boolean puedeCompletarWizard() {
        return documentosPreWizardCompletos() && Boolean.TRUE.equals(requiereCompletitudDirector);
    }

    public boolean plazoCompletarVencido() {
        return fechaLimiteCompletar != null && LocalDate.now().isAfter(fechaLimiteCompletar);
    }

    public void marcarViabilidadCargada() {
        this.viabilidadEstado = ViabilidadEstado.CARGADA;
        this.viabilidadObservaciones = null;
    }

    public void aprobarDocumentos(String revisadoPor) {
        this.viabilidadEstado = ViabilidadEstado.APROBADA;
        this.viabilidadObservaciones = null;
        this.viabilidadRevisadoPor = revisadoPor;
        this.viabilidadRevisadoEn = LocalDateTime.now();
        this.documentosVerificados = true;
        this.fechaVerificacionDocumentos = LocalDateTime.now();
        this.fechaLimiteCompletar = LocalDate.now().plusDays(30);
    }

    public void devolverDocumentos(String observaciones, String revisadoPor) {
        if (observaciones == null || observaciones.isBlank()) {
            throw new IllegalStateException("Las observaciones son obligatorias al devolver los documentos.");
        }
        this.viabilidadEstado = ViabilidadEstado.DEVUELTA;
        this.viabilidadObservaciones = observaciones;
        this.viabilidadRevisadoPor = revisadoPor;
        this.viabilidadRevisadoEn = LocalDateTime.now();
        this.documentosVerificados = false;
        this.fechaVerificacionDocumentos = null;
        this.fechaLimiteCompletar = null;
    }

    public void cerrarForzoso(String gestorUsername) {
        this.cierreForzoso = true;
        this.cierreForzosoPor = gestorUsername;
        this.cierreForzosoEn = LocalDateTime.now();
        this.estado = EstadoProyecto.CERRADO_FORZOSO;
        this.estadoConfig = null;
        this.requiereCompletitudDirector = false;
    }

    public void marcarActaConstitucionCargada() {
        this.actaConstitucionCargada = true;
    }

    /**
     * Cambia el estado del proyecto a CERRADO si cumple las condiciones.
     */
    public void cerrar() {
        if (!esAptoParaCierre()) {
            throw new IllegalStateException("No se puede cerrar un proyecto que no ha alcanzado el 100% de avance.");
        }
        this.estado = EstadoProyecto.CERRADO;
        // estadoConfig se mantiene sincronizado via FK
    }
}
