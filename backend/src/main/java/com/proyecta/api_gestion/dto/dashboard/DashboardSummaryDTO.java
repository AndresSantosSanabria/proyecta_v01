package com.proyecta.api_gestion.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"total_proyectos":25,"activos":18,"cerrados":7,"avance_promedio":65,"avance_tendencia":"positiva","entregables_atrasados":4,"proximos_a_vencer":3,"dias_ventana_vencimiento":7}
    """)
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

    public DashboardSummaryDTO(Long totalProyectos, Long activos, Long cerrados,
                               Integer avancePromedio, String avanceTendencia,
                               Long entregablesAtrasados, Long proximosAVencer,
                               Integer diasVentanaVencimiento) {
        this.total_proyectos = totalProyectos;
        this.activos = activos;
        this.cerrados = cerrados;
        this.avance_promedio = avancePromedio;
        this.avance_tendencia = avanceTendencia;
        this.entregables_atrasados = entregablesAtrasados;
        this.proximos_a_vencer = proximosAVencer;
        this.dias_ventana_vencimiento = diasVentanaVencimiento;
    }

    public Long getTotal_proyectos() { return total_proyectos; }
    public void setTotal_proyectos(Long totalProyectos) { this.total_proyectos = totalProyectos; }

    public Long getActivos() { return activos; }
    public void setActivos(Long activos) { this.activos = activos; }

    public Long getCerrados() { return cerrados; }
    public void setCerrados(Long cerrados) { this.cerrados = cerrados; }

    public Integer getAvance_promedio() { return avance_promedio; }
    public void setAvance_promedio(Integer avancePromedio) { this.avance_promedio = avancePromedio; }

    public String getAvance_tendencia() { return avance_tendencia; }
    public void setAvance_tendencia(String avanceTendencia) { this.avance_tendencia = avanceTendencia; }

    public Long getEntregables_atrasados() { return entregables_atrasados; }
    public void setEntregables_atrasados(Long entregablesAtrasados) { this.entregables_atrasados = entregablesAtrasados; }

    public Long getProximos_a_vencer() { return proximos_a_vencer; }
    public void setProximos_a_vencer(Long proximosAVencer) { this.proximos_a_vencer = proximosAVencer; }

    public Integer getDias_ventana_vencimiento() { return dias_ventana_vencimiento; }
    public void setDias_ventana_vencimiento(Integer diasVentanaVencimiento) { this.dias_ventana_vencimiento = diasVentanaVencimiento; }
}
