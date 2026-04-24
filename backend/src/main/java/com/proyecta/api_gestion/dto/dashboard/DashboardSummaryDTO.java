package com.proyecta.api_gestion.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

public class DashboardSummaryDTO {

    @Schema(description = "Cantidad total de proyectos registrados", example = "25")
    private Long total_proyectos;

    @Schema(description = "Cantidad de proyectos en estado activo", example = "18")
    private Long activos;

    @Schema(description = "Cantidad de proyectos finalizados o cerrados", example = "7")
    private Long cerrados;

    @Schema(description = "Porcentaje de avance promedio de todos los proyectos", example = "65")
    private Integer avance_promedio;

    @Schema(description = "Tendencia actual del avance (positiva, negativa, estable)", example = "positiva",
            allowableValues = {"positiva", "negativa", "estable"})
    private String avance_tendencia;

    @Schema(description = "Número de entregables cuya fecha de entrega ha pasado", example = "4")
    private Long entregables_atrasados;

    @Schema(description = "Proyectos con hitos cercanos a vencer en la ventana configurada", example = "3")
    private Long proximos_a_vencer;

    @Schema(description = "Rango de días considerado para la ventana de vencimiento", example = "7")
    private Integer dias_ventana_vencimiento;

    public DashboardSummaryDTO() {}

    public DashboardSummaryDTO(Long total_proyectos, Long activos, Long cerrados,
                               Integer avance_promedio, String avance_tendencia,
                               Long entregables_atrasados, Long proximos_a_vencer,
                               Integer dias_ventana_vencimiento) {
        this.total_proyectos = total_proyectos;
        this.activos = activos;
        this.cerrados = cerrados;
        this.avance_promedio = avance_promedio;
        this.avance_tendencia = avance_tendencia;
        this.entregables_atrasados = entregables_atrasados;
        this.proximos_a_vencer = proximos_a_vencer;
        this.dias_ventana_vencimiento = dias_ventana_vencimiento;
    }

    public Long getTotal_proyectos() { return total_proyectos; }
    public void setTotal_proyectos(Long total_proyectos) { this.total_proyectos = total_proyectos; }

    public Long getActivos() { return activos; }
    public void setActivos(Long activos) { this.activos = activos; }

    public Long getCerrados() { return cerrados; }
    public void setCerrados(Long cerrados) { this.cerrados = cerrados; }

    public Integer getAvance_promedio() { return avance_promedio; }
    public void setAvance_promedio(Integer avance_promedio) { this.avance_promedio = avance_promedio; }

    public String getAvance_tendencia() { return avance_tendencia; }
    public void setAvance_tendencia(String avance_tendencia) { this.avance_tendencia = avance_tendencia; }

    public Long getEntregables_atrasados() { return entregables_atrasados; }
    public void setEntregables_atrasados(Long entregables_atrasados) { this.entregables_atrasados = entregables_atrasados; }

    public Long getProximos_a_vencer() { return proximos_a_vencer; }
    public void setProximos_a_vencer(Long proximos_a_vencer) { this.proximos_a_vencer = proximos_a_vencer; }

    public Integer getDias_ventana_vencimiento() { return dias_ventana_vencimiento; }
    public void setDias_ventana_vencimiento(Integer dias_ventana_vencimiento) { this.dias_ventana_vencimiento = dias_ventana_vencimiento; }
}
