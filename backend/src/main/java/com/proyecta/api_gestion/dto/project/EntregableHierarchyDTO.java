package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntregableHierarchyDTO {
    private Integer id;
    private Short numero;
    private String nombre;
    private BigDecimal ponderacion;
    private Boolean conforme;
    private LocalDate fechaInicio;
    private LocalDate fechaEntrega;
    private String estado;
    private Integer diasDiferencia;
    private String observacionRevision;
    private Boolean tieneHistorialCambiosFecha;

    public EntregableHierarchyDTO() {}

    public EntregableHierarchyDTO(Integer id, Short numero, String nombre, BigDecimal ponderacion,
                                   Boolean conforme, LocalDate fechaInicio, LocalDate fechaEntrega, String estado, Integer diasDiferencia) {
        this.id = id;
        this.numero = numero;
        this.nombre = nombre;
        this.ponderacion = ponderacion;
        this.conforme = conforme;
        this.fechaInicio = fechaInicio;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
        this.diasDiferencia = diasDiferencia;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Short getNumero() { return numero; }
    public void setNumero(Short numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    public Boolean getConforme() { return conforme; }
    public void setConforme(Boolean conforme) { this.conforme = conforme; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getDiasDiferencia() { return diasDiferencia; }
    public void setDiasDiferencia(Integer diasDiferencia) { this.diasDiferencia = diasDiferencia; }
    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }
    public Boolean getTieneHistorialCambiosFecha() { return tieneHistorialCambiosFecha; }
    public void setTieneHistorialCambiosFecha(Boolean tieneHistorialCambiosFecha) { this.tieneHistorialCambiosFecha = tieneHistorialCambiosFecha; }
}
