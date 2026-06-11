package com.proyecta.api_gestion.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas_cierre", schema = "proyecta_db")
public class ActaCierre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acta_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @Column(name = "resumen_ejecutivo", columnDefinition = "TEXT", nullable = false)
    private String resumenEjecutivo;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "avance_final", precision = 5, scale = 2, nullable = false)
    private BigDecimal avanceFinal;

    @Column(name = "progreso_programado_final", precision = 5, scale = 2)
    private BigDecimal progresoProgramadoFinal;

    @Column(name = "progreso_ejecutado_final", precision = 5, scale = 2)
    private BigDecimal progresoEjecutadoFinal;

    @Column(name = "diferencia_final", precision = 5, scale = 2)
    private BigDecimal diferenciaFinal;

    @Column(name = "eficacia_final", precision = 6, scale = 4)
    private BigDecimal eficaciaFinal;

    @Column(name = "estado_final", length = 30)
    private String estadoFinal;

    @Column(name = "corte_calculo")
    private LocalDate corteCalculo;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "archivo_pdf", length = 255)
    private String archivoPdf;

    @Column(name = "ruta_archivo_pdf", length = 255)
    private String rutaArchivoPdf;

    public ActaCierre() {
    }

    public ActaCierre(Proyecto proyecto, String resumenEjecutivo, LocalDateTime fechaCierre, BigDecimal avanceFinal) {
        this.proyecto = proyecto;
        this.resumenEjecutivo = resumenEjecutivo;
        this.fechaCierre = fechaCierre;
        this.avanceFinal = avanceFinal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public String getResumenEjecutivo() {
        return resumenEjecutivo;
    }

    public void setResumenEjecutivo(String resumenEjecutivo) {
        this.resumenEjecutivo = resumenEjecutivo;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getAvanceFinal() {
        return avanceFinal;
    }

    public void setAvanceFinal(BigDecimal avanceFinal) {
        this.avanceFinal = avanceFinal;
    }

    public BigDecimal getProgresoProgramadoFinal() {
        return progresoProgramadoFinal;
    }

    public void setProgresoProgramadoFinal(BigDecimal progresoProgramadoFinal) {
        this.progresoProgramadoFinal = progresoProgramadoFinal;
    }

    public BigDecimal getProgresoEjecutadoFinal() {
        return progresoEjecutadoFinal;
    }

    public void setProgresoEjecutadoFinal(BigDecimal progresoEjecutadoFinal) {
        this.progresoEjecutadoFinal = progresoEjecutadoFinal;
    }

    public BigDecimal getDiferenciaFinal() {
        return diferenciaFinal;
    }

    public void setDiferenciaFinal(BigDecimal diferenciaFinal) {
        this.diferenciaFinal = diferenciaFinal;
    }

    public BigDecimal getEficaciaFinal() {
        return eficaciaFinal;
    }

    public void setEficaciaFinal(BigDecimal eficaciaFinal) {
        this.eficaciaFinal = eficaciaFinal;
    }

    public String getEstadoFinal() {
        return estadoFinal;
    }

    public void setEstadoFinal(String estadoFinal) {
        this.estadoFinal = estadoFinal;
    }

    public LocalDate getCorteCalculo() {
        return corteCalculo;
    }

    public void setCorteCalculo(LocalDate corteCalculo) {
        this.corteCalculo = corteCalculo;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getArchivoPdf() {
        return archivoPdf;
    }

    public void setArchivoPdf(String archivoPdf) {
        this.archivoPdf = archivoPdf;
    }

    public String getRutaArchivoPdf() {
        return rutaArchivoPdf;
    }

    public void setRutaArchivoPdf(String rutaArchivoPdf) {
        this.rutaArchivoPdf = rutaArchivoPdf;
    }
}
