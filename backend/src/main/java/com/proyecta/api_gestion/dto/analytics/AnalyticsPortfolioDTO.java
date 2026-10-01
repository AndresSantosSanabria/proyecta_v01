package com.proyecta.api_gestion.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
    {
      "corte": "2026-07-15",
      "indicadores": {"totalProyectos": 25, "activos": 18, "cerrados": 7, "avancePromedio": 65.40, "eficaciaPromedio": 91.20, "eficienciaPromedio": 88.70, "entregablesAtrasados": 4, "proximosAVencer": 3},
      "dependencias": [{"dependencia": "Secretaría de Transformación Digital", "totalProyectos": 12, "avancePromedio": 72.40, "eficaciaPromedio": 93.10, "eficienciaPromedio": 90.50, "proyectosPeti": 6, "riesgosTratados": 5, "riesgosPendientes": 3}],
      "estrategias": [{"codigo": "TECNOLOGIAS_INFORMACION", "nombre": "Tecnologías de la Información", "proyectos": 9, "avancePromedio": 68.90, "eficaciaPromedio": 92.00, "eficienciaPromedio": 89.40}],
      "furag": {"proyectosConFurag": 20, "proyectosCumplidos": 17, "respuestasCompletas": 85, "respuestasObligatorias": 100, "coberturaPromedio": 85.00, "proyectos": [{"proyectoId": "PROY-CUN-2026-008", "nombre": "Modernización de Redes LAN", "cobertura": 100.00, "respuestasCompletas": 15, "respuestasObligatorias": 15}]},
      "riesgos": {"total": 48, "tratados": 31, "pendientes": 17, "indiceMitigacion": 64.58, "porNivel": [{"nivel": "ALTO", "color": "#D00000", "cantidad": 6}]},
      "proyectos": [{"proyectoId": "PROY-CUN-2026-008", "nombre": "Modernización de Redes LAN", "dependencia": "Secretaría de Transformación Digital", "estrategia": "TECNOLOGIAS_INFORMACION", "peti": true, "avance": 55.00, "eficacia": 91.67, "eficiencia": 83.33, "estado": "ACTIVO", "atrasados": 2, "furagCobertura": 100.00, "indiceMitigacion": 75.00, "documentosCargados": true, "documentosVerificados": true, "viabilidadEstado": "APROBADO", "tieneCronograma": true, "tieneActaConstitucion": true, "tienePlanComunicaciones": true, "registradoInicialPor": "ana.gestion@proyecta.gov.co", "correoDirector": "maria.gomez@proyecta.gov.co"}]
    }
    """)
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
            BigDecimal indiceMitigacion,
            Boolean documentosCargados,
            Boolean documentosVerificados,
            String viabilidadEstado,
            Boolean tieneCronograma,
            Boolean tieneActaConstitucion,
            Boolean tienePlanComunicaciones,
            String registradoInicialPor,
            String correoDirector
    ) {}
}
