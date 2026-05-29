package com.proyecta.api_gestion.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsPortfolioDTO(
        LocalDate corte,
        ExecutiveMetrics indicadores,
        List<DependenciaMetrics> dependencias,
        List<EstrategiaMetrics> estrategias,
        FuragMetrics furag,
        RiskMetrics riesgos,
        List<ProjectMetrics> proyectos
) {

    public record ExecutiveMetrics(
            long totalProyectos,
            long activos,
            long cerrados,
            BigDecimal avancePromedio,
            BigDecimal eficaciaPromedio,
            BigDecimal eficienciaPromedio,
            long entregablesAtrasados,
            long proximosAVencer
    ) {}

    public record DependenciaMetrics(
            String dependencia,
            long totalProyectos,
            BigDecimal avancePromedio,
            BigDecimal eficaciaPromedio,
            BigDecimal eficienciaPromedio,
            long proyectosPeti,
            long riesgosTratados,
            long riesgosPendientes
    ) {}

    public record EstrategiaMetrics(
            String codigo,
            String nombre,
            long proyectos,
            BigDecimal avancePromedio,
            BigDecimal eficaciaPromedio,
            BigDecimal eficienciaPromedio
    ) {}

    public record RiskLevelMetrics(
            String nivel,
            String color,
            long cantidad
    ) {}

    public record RiskMetrics(
            long total,
            long tratados,
            long pendientes,
            BigDecimal indiceMitigacion,
            List<RiskLevelMetrics> porNivel
    ) {}

    public record FuragProjectMetrics(
            String proyectoId,
            String nombre,
            BigDecimal cobertura,
            long respuestasCompletas,
            long respuestasObligatorias
    ) {}

    public record FuragMetrics(
            long proyectosConFurag,
            long proyectosCumplidos,
            long respuestasCompletas,
            long respuestasObligatorias,
            BigDecimal coberturaPromedio,
            List<FuragProjectMetrics> proyectos
    ) {}

    public record ProjectMetrics(
            String proyectoId,
            String nombre,
            String dependencia,
            String estrategia,
            Boolean peti,
            BigDecimal avance,
            BigDecimal eficacia,
            BigDecimal eficiencia,
            String estado,
            long atrasados,
            BigDecimal furagCobertura,
            BigDecimal indiceMitigacion
    ) {}
}
