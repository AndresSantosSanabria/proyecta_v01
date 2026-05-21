package com.proyecta.api_gestion.dto.ponderacion;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO para respuestas de auditoría y gestión de ponderaciones.
 * Proporciona información detallada sobre el estado de consistencia.
 */
public class PonderacionAuditoriaResponseDTO {

    @JsonProperty("proyecto_id")
    private String proyectoId;

    @JsonProperty("proyecto_nombre")
    private String proyectoNombre;

    @JsonProperty("cantidad_fases")
    private Integer cantidadFases;

    @JsonProperty("suma_ponderaciones")
    private BigDecimal sumaPonderaciones;

    @JsonProperty("es_consistente")
    private Boolean esConsistente;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("necesita_normalizacion")
    private Boolean necesitaNormalizacion;

    @JsonProperty("fases")
    private List<FaseResumenDTO> fases;

    // Constructors
    public PonderacionAuditoriaResponseDTO() {
    }

    public PonderacionAuditoriaResponseDTO(String proyectoId, String proyectoNombre, 
            Integer cantidadFases, BigDecimal sumaPonderaciones, Boolean esConsistente, 
            String estado, Boolean necesitaNormalizacion, List<FaseResumenDTO> fases) {
        this.proyectoId = proyectoId;
        this.proyectoNombre = proyectoNombre;
        this.cantidadFases = cantidadFases;
        this.sumaPonderaciones = sumaPonderaciones;
        this.esConsistente = esConsistente;
        this.estado = estado;
        this.necesitaNormalizacion = necesitaNormalizacion;
        this.fases = fases;
    }

    // Getters and Setters
    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getProyectoNombre() { return proyectoNombre; }
    public void setProyectoNombre(String proyectoNombre) { this.proyectoNombre = proyectoNombre; }

    public Integer getCantidadFases() { return cantidadFases; }
    public void setCantidadFases(Integer cantidadFases) { this.cantidadFases = cantidadFases; }

    public BigDecimal getSumaPonderaciones() { return sumaPonderaciones; }
    public void setSumaPonderaciones(BigDecimal sumaPonderaciones) { this.sumaPonderaciones = sumaPonderaciones; }

    public Boolean getEsConsistente() { return esConsistente; }
    public void setEsConsistente(Boolean esConsistente) { this.esConsistente = esConsistente; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getNecesitaNormalizacion() { return necesitaNormalizacion; }
    public void setNecesitaNormalizacion(Boolean necesitaNormalizacion) { this.necesitaNormalizacion = necesitaNormalizacion; }

    public List<FaseResumenDTO> getFases() { return fases; }
    public void setFases(List<FaseResumenDTO> fases) { this.fases = fases; }

    /**
     * DTO interno para resumen de fases
     */
    public static class FaseResumenDTO {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("ponderacion")
        private BigDecimal ponderacion;

        public FaseResumenDTO(Integer id, String nombre, BigDecimal ponderacion) {
            this.id = id;
            this.nombre = nombre;
            this.ponderacion = ponderacion;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public BigDecimal getPonderacion() { return ponderacion; }
        public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
    }
}
