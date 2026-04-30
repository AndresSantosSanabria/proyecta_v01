package com.proyecta.api_gestion.dto.proyecto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ProyectoSummaryDTO {
    @JsonProperty("avance_total")
    private BigDecimal avance_total;

    @JsonProperty("entregables_ratio")
    private String entregables_ratio;

    @JsonProperty("total_atrasados")
    private Long total_atrasados;

    @JsonProperty("proximos_vencer")
    private Long proximos_vencer;

    public ProyectoSummaryDTO() {}

    public ProyectoSummaryDTO(BigDecimal avance_total, String entregables_ratio, Long total_atrasados, Long proximos_vencer) {
        this.avance_total = avance_total;
        this.entregables_ratio = entregables_ratio;
        this.total_atrasados = total_atrasados;
        this.proximos_vencer = proximos_vencer;
    }

    public BigDecimal getAvance_total() {
        return avance_total;
    }

    public void setAvance_total(BigDecimal avance_total) {
        this.avance_total = avance_total;
    }

    public String getEntregables_ratio() {
        return entregables_ratio;
    }

    public void setEntregables_ratio(String entregables_ratio) {
        this.entregables_ratio = entregables_ratio;
    }

    public Long getTotal_atrasados() {
        return total_atrasados;
    }

    public void setTotal_atrasados(Long total_atrasados) {
        this.total_atrasados = total_atrasados;
    }

    public Long getProximos_vencer() {
        return proximos_vencer;
    }

    public void setProximos_vencer(Long proximos_vencer) {
        this.proximos_vencer = proximos_vencer;
    }
}
