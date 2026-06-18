package com.proyecta.api_gestion.model.beneficioimpacto;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoBeneficioImpacto;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proyecto_beneficio_impacto", schema = "proyecta_db")
public class ProyectoBeneficioImpacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoBeneficioImpacto estado = EstadoBeneficioImpacto.PENDIENTE;

    @Column(name = "requerido_en")
    private LocalDateTime requeridoEn;

    @Column(name = "requerido_por", length = 120)
    private String requeridoPor;

    @Column(name = "diligenciado_en")
    private LocalDateTime diligenciadoEn;

    @Column(name = "diligenciado_por", length = 120)
    private String diligenciadoPor;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "revisado_por", length = 120)
    private String revisadoPor;

    @Column(name = "poblacion_beneficiada_directa")
    private Integer poblacionBeneficiadaDirecta;

    @Column(name = "poblacion_beneficiada_indirecta")
    private Integer poblacionBeneficiadaIndirecta;

    @Column(name = "poblacion_objetivo")
    private Integer poblacionObjetivo;

    @Column(name = "territorio_beneficiado", length = 200)
    private String territorioBeneficiado;

    @Column(name = "beneficio_principal", columnDefinition = "TEXT")
    private String beneficioPrincipal;

    @Column(name = "impacto_social", columnDefinition = "TEXT")
    private String impactoSocial;

    @Column(name = "impacto_institucional", columnDefinition = "TEXT")
    private String impactoInstitucional;

    @Column(name = "impacto_economico", columnDefinition = "TEXT")
    private String impactoEconomico;

    @Column(name = "alineacion_plan_desarrollo", columnDefinition = "TEXT")
    private String alineacionPlanDesarrollo;

    @Column(name = "alineacion_peti", columnDefinition = "TEXT")
    private String alineacionPeti;

    @Column(name = "metas_contribuidas", columnDefinition = "TEXT")
    private String metasContribuidas;

    @Column(name = "indicador_base", columnDefinition = "TEXT")
    private String indicadorBase;

    @Column(name = "indicador_meta", columnDefinition = "TEXT")
    private String indicadorMeta;

    @Column(name = "indicador_resultado", columnDefinition = "TEXT")
    private String indicadorResultado;

    @Column(name = "fuente_verificacion", columnDefinition = "TEXT")
    private String fuenteVerificacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (estado == null) {
            estado = EstadoBeneficioImpacto.PENDIENTE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public EstadoBeneficioImpacto getEstado() { return estado; }
    public void setEstado(EstadoBeneficioImpacto estado) { this.estado = estado; }
    public LocalDateTime getRequeridoEn() { return requeridoEn; }
    public void setRequeridoEn(LocalDateTime requeridoEn) { this.requeridoEn = requeridoEn; }
    public String getRequeridoPor() { return requeridoPor; }
    public void setRequeridoPor(String requeridoPor) { this.requeridoPor = requeridoPor; }
    public LocalDateTime getDiligenciadoEn() { return diligenciadoEn; }
    public void setDiligenciadoEn(LocalDateTime diligenciadoEn) { this.diligenciadoEn = diligenciadoEn; }
    public String getDiligenciadoPor() { return diligenciadoPor; }
    public void setDiligenciadoPor(String diligenciadoPor) { this.diligenciadoPor = diligenciadoPor; }
    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public void setRevisadoEn(LocalDateTime revisadoEn) { this.revisadoEn = revisadoEn; }
    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }
    public Integer getPoblacionBeneficiadaDirecta() { return poblacionBeneficiadaDirecta; }
    public void setPoblacionBeneficiadaDirecta(Integer poblacionBeneficiadaDirecta) { this.poblacionBeneficiadaDirecta = poblacionBeneficiadaDirecta; }
    public Integer getPoblacionBeneficiadaIndirecta() { return poblacionBeneficiadaIndirecta; }
    public void setPoblacionBeneficiadaIndirecta(Integer poblacionBeneficiadaIndirecta) { this.poblacionBeneficiadaIndirecta = poblacionBeneficiadaIndirecta; }
    public Integer getPoblacionObjetivo() { return poblacionObjetivo; }
    public void setPoblacionObjetivo(Integer poblacionObjetivo) { this.poblacionObjetivo = poblacionObjetivo; }
    public String getTerritorioBeneficiado() { return territorioBeneficiado; }
    public void setTerritorioBeneficiado(String territorioBeneficiado) { this.territorioBeneficiado = territorioBeneficiado; }
    public String getBeneficioPrincipal() { return beneficioPrincipal; }
    public void setBeneficioPrincipal(String beneficioPrincipal) { this.beneficioPrincipal = beneficioPrincipal; }
    public String getImpactoSocial() { return impactoSocial; }
    public void setImpactoSocial(String impactoSocial) { this.impactoSocial = impactoSocial; }
    public String getImpactoInstitucional() { return impactoInstitucional; }
    public void setImpactoInstitucional(String impactoInstitucional) { this.impactoInstitucional = impactoInstitucional; }
    public String getImpactoEconomico() { return impactoEconomico; }
    public void setImpactoEconomico(String impactoEconomico) { this.impactoEconomico = impactoEconomico; }
    public String getAlineacionPlanDesarrollo() { return alineacionPlanDesarrollo; }
    public void setAlineacionPlanDesarrollo(String alineacionPlanDesarrollo) { this.alineacionPlanDesarrollo = alineacionPlanDesarrollo; }
    public String getAlineacionPeti() { return alineacionPeti; }
    public void setAlineacionPeti(String alineacionPeti) { this.alineacionPeti = alineacionPeti; }
    public String getMetasContribuidas() { return metasContribuidas; }
    public void setMetasContribuidas(String metasContribuidas) { this.metasContribuidas = metasContribuidas; }
    public String getIndicadorBase() { return indicadorBase; }
    public void setIndicadorBase(String indicadorBase) { this.indicadorBase = indicadorBase; }
    public String getIndicadorMeta() { return indicadorMeta; }
    public void setIndicadorMeta(String indicadorMeta) { this.indicadorMeta = indicadorMeta; }
    public String getIndicadorResultado() { return indicadorResultado; }
    public void setIndicadorResultado(String indicadorResultado) { this.indicadorResultado = indicadorResultado; }
    public String getFuenteVerificacion() { return fuenteVerificacion; }
    public void setFuenteVerificacion(String fuenteVerificacion) { this.fuenteVerificacion = fuenteVerificacion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
