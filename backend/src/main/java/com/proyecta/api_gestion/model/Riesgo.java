package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_id")
    private Integer id;

    @Column(length = 10)
    private String codigo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(name = "categoria_riesgo", length = 120)
    private String categoriaRiesgo;

    @Column(columnDefinition = "TEXT")
    private String causa;

    @Column(columnDefinition = "TEXT")
    private String consecuencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Probabilidad probabilidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Impacto impacto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelRiesgo nivel;

    @Enumerated(EnumType.STRING)
    @Column(name = "probabilidad_residual", length = 20)
    private Probabilidad probabilidadResidual;

    @Enumerated(EnumType.STRING)
    @Column(name = "impacto_residual", length = 20)
    private Impacto impactoResidual;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_residual", length = 20)
    private NivelRiesgo nivelResidual;

    @Column(name = "controles_existentes", columnDefinition = "TEXT")
    private String controlesExistentes;

    @Column(name = "tipo_control", length = 80)
    private String tipoControl;

    @Column(name = "valoracion_control", length = 80)
    private String valoracionControl;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "acciones_mitigacion", columnDefinition = "TEXT")
    private String accionesMitigacion;

    @Column(name = "entidad_responsable", length = 150)
    private String entidadResponsable;

    @Column(name = "rol_responsable", length = 150)
    private String rolResponsable;

    @Column(name = "fecha_accion")
    private LocalDate fechaAccion;

    @Column(name = "evidencia_indicador", columnDefinition = "TEXT")
    private String evidenciaIndicador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado = EstadoRiesgo.PENDIENTE;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "riesgo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RiesgoSolucionAdjunto> soluciones = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    public Riesgo() {
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoriaRiesgo() {
        return categoriaRiesgo;
    }

    public void setCategoriaRiesgo(String categoriaRiesgo) {
        this.categoriaRiesgo = categoriaRiesgo;
    }

    public String getCausa() {
        return causa;
    }

    public void setCausa(String causa) {
        this.causa = causa;
    }

    public String getConsecuencia() {
        return consecuencia;
    }

    public void setConsecuencia(String consecuencia) {
        this.consecuencia = consecuencia;
    }

    public Probabilidad getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Probabilidad probabilidad) {
        this.probabilidad = probabilidad;
    }

    public Impacto getImpacto() {
        return impacto;
    }

    public void setImpacto(Impacto impacto) {
        this.impacto = impacto;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public Probabilidad getProbabilidadResidual() {
        return probabilidadResidual;
    }

    public void setProbabilidadResidual(Probabilidad probabilidadResidual) {
        this.probabilidadResidual = probabilidadResidual;
    }

    public Impacto getImpactoResidual() {
        return impactoResidual;
    }

    public void setImpactoResidual(Impacto impactoResidual) {
        this.impactoResidual = impactoResidual;
    }

    public NivelRiesgo getNivelResidual() {
        return nivelResidual;
    }

    public void setNivelResidual(NivelRiesgo nivelResidual) {
        this.nivelResidual = nivelResidual;
    }

    public String getControlesExistentes() {
        return controlesExistentes;
    }

    public void setControlesExistentes(String controlesExistentes) {
        this.controlesExistentes = controlesExistentes;
    }

    public String getTipoControl() {
        return tipoControl;
    }

    public void setTipoControl(String tipoControl) {
        this.tipoControl = tipoControl;
    }

    public String getValoracionControl() {
        return valoracionControl;
    }

    public void setValoracionControl(String valoracionControl) {
        this.valoracionControl = valoracionControl;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getAccionesMitigacion() {
        return accionesMitigacion;
    }

    public void setAccionesMitigacion(String accionesMitigacion) {
        this.accionesMitigacion = accionesMitigacion;
    }

    public String getEntidadResponsable() {
        return entidadResponsable;
    }

    public void setEntidadResponsable(String entidadResponsable) {
        this.entidadResponsable = entidadResponsable;
    }

    public String getRolResponsable() {
        return rolResponsable;
    }

    public void setRolResponsable(String rolResponsable) {
        this.rolResponsable = rolResponsable;
    }

    public LocalDate getFechaAccion() {
        return fechaAccion;
    }

    public void setFechaAccion(LocalDate fechaAccion) {
        this.fechaAccion = fechaAccion;
    }

    public String getEvidenciaIndicador() {
        return evidenciaIndicador;
    }

    public void setEvidenciaIndicador(String evidenciaIndicador) {
        this.evidenciaIndicador = evidenciaIndicador;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }

    public void setEstado(EstadoRiesgo estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public List<RiesgoSolucionAdjunto> getSoluciones() {
        return soluciones;
    }

    public void setSoluciones(List<RiesgoSolucionAdjunto> soluciones) {
        this.soluciones = soluciones;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }
}
