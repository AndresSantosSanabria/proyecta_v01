package com.proyecta.api_gestion.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"total_proyectos":25,"activos":18,"cerrados":7,"avance_promedio":65,"avance_tendencia":"positiva","entregables_atrasados":4,"proximos_a_vencer":3,"dias_ventana_vencimiento":7}
    """)
public class DashboardSummaryDTO {

    @JsonProperty("total_proyectos")
    @Schema(description = "Cantidad total de proyectos registrados", example = "25")
    private Long totalProyectos;

    @Schema(description = "Cantidad de proyectos en estado activo", example = "18")
    private Long activos;

    @Schema(description = "Cantidad de proyectos finalizados o cerrados", example = "7")
    private Long cerrados;

    @JsonProperty("avance_promedio")
    @Schema(description = "Porcentaje de avance promedio de todos los proyectos", example = "65")
    private Integer avancePromedio;

    @JsonProperty("avance_tendencia")
    @Schema(description = "Tendencia actual del avance (positiva, negativa, estable)", example = "positiva",
            allowableValues = {"positiva", "negativa", "estable"})
    private String avanceTendencia;

    @JsonProperty("entregables_atrasados")
    @Schema(description = "Número de entregables cuya fecha de entrega ha pasado", example = "4")
    private Long entregablesAtrasados;

    @JsonProperty("proximos_a_vencer")
    @Schema(description = "Proyectos con hitos cercanos a vencer en la ventana configurada", example = "3")
    private Long proximosAVencer;

    @JsonProperty("dias_ventana_vencimiento")
    @Schema(description = "Rango de días considerado para la ventana de vencimiento", example = "7")
    private Integer diasVentanaVencimiento;

    // Constructor vacío requerido por Jackson para la deserialización
    @SuppressWarnings("java:S1186")
    public DashboardSummaryDTO() {}

    public Long getTotalProyectos() { return totalProyectos; }
    public void setTotalProyectos(Long totalProyectos) { this.totalProyectos = totalProyectos; }

    public Long getActivos() { return activos; }
    public void setActivos(Long activos) { this.activos = activos; }

    public Long getCerrados() { return cerrados; }
    public void setCerrados(Long cerrados) { this.cerrados = cerrados; }

    public Integer getAvancePromedio() { return avancePromedio; }
    public void setAvancePromedio(Integer avancePromedio) { this.avancePromedio = avancePromedio; }

    public String getAvanceTendencia() { return avanceTendencia; }
    public void setAvanceTendencia(String avanceTendencia) { this.avanceTendencia = avanceTendencia; }

    public Long getEntregablesAtrasados() { return entregablesAtrasados; }
    public void setEntregablesAtrasados(Long entregablesAtrasados) { this.entregablesAtrasados = entregablesAtrasados; }

    public Long getProximosAVencer() { return proximosAVencer; }
    public void setProximosAVencer(Long proximosAVencer) { this.proximosAVencer = proximosAVencer; }

    public Integer getDiasVentanaVencimiento() { return diasVentanaVencimiento; }
    public void setDiasVentanaVencimiento(Integer diasVentanaVencimiento) { this.diasVentanaVencimiento = diasVentanaVencimiento; }
}
