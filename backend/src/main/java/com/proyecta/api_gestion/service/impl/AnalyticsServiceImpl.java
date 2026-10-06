package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.analytics.AnalyticsPortfolioDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.domain.model.ActaCierre;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.Riesgo;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.application.port.out.persistence.ActaCierreRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.FuragRespuestaRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.RiesgoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.config.MatrizRiesgoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String NO_PETI = "NO_PETI";

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final EntregableRepositoryPort entregableRepositoryPort;
    private final RiesgoRepositoryPort riesgoRepositoryPort;
    private final FuragRespuestaRepositoryPort furagRespuestaRepositoryPort;
    private final ActaCierreRepositoryPort actaCierreRepositoryPort;
    private final ProjectProgressMetricsService progressMetricsService;
    private final MatrizRiesgoRepositoryPort matrizRiesgoRepositoryPort;

    public AnalyticsServiceImpl(
            ProyectoRepositoryPort proyectoRepositoryPort,
            EntregableRepositoryPort entregableRepositoryPort,
            RiesgoRepositoryPort riesgoRepositoryPort,
            FuragRespuestaRepositoryPort furagRespuestaRepositoryPort,
            ActaCierreRepositoryPort actaCierreRepositoryPort,
            ProjectProgressMetricsService progressMetricsService,
            MatrizRiesgoRepositoryPort matrizRiesgoRepositoryPort) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.riesgoRepositoryPort = riesgoRepositoryPort;
        this.furagRespuestaRepositoryPort = furagRespuestaRepositoryPort;
        this.actaCierreRepositoryPort = actaCierreRepositoryPort;
        this.progressMetricsService = progressMetricsService;
        this.matrizRiesgoRepositoryPort = matrizRiesgoRepositoryPort;
    }

    @Override
    public AnalyticsPortfolioDTO getPortfolioAnalytics() {
        LocalDate corte = LocalDate.now(ZoneId.systemDefault());
        List<ProjectMetricsHolder> proyectos = proyectoRepositoryPort.findAll().stream()
                .map(proyecto -> construirProyectoMetrics(proyecto, corte))
                .sorted(Comparator.comparing(
                                (ProjectMetricsHolder holder) -> normalizeGroup(holder.metrics().dependencia()),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                (ProjectMetricsHolder holder) -> normalizeGroup(holder.metrics().nombre()),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();

        AnalyticsPortfolioDTO.ExecutiveMetrics executive = construirExecutiveMetrics(proyectos, corte);
        List<AnalyticsPortfolioDTO.DependenciaMetrics> dependencias = construirDependencias(proyectos);
        List<AnalyticsPortfolioDTO.EstrategiaMetrics> estrategias = construirEstrategias(proyectos);
        AnalyticsPortfolioDTO.RiskMetrics riesgos = construirRiesgos();
        AnalyticsPortfolioDTO.FuragMetrics furag = construirFurag(proyectos);

        List<AnalyticsPortfolioDTO.ProjectMetrics> proyectosDto = proyectos.stream()
                .map(ProjectMetricsHolder::metrics)
                .toList();

        return new AnalyticsPortfolioDTO(corte, executive, dependencias, estrategias, furag, riesgos, proyectosDto);
    }

    private void addMetric(List<BigDecimal> acc, BigDecimal value, boolean tieneActividad) {
        if (tieneActividad && value != null) acc.add(value);
    }

    private AnalyticsPortfolioDTO.ExecutiveMetrics construirExecutiveMetrics(List<ProjectMetricsHolder> proyectos, LocalDate corte) {
        long total = proyectos.size();
        long activos = proyectos.stream().filter(holder -> holder.proyecto().getEstado() != null
                && !holder.proyecto().esEstadoTerminal()).count();
        long cerrados = total - activos;

        List<BigDecimal> avances = new ArrayList<>();
        List<BigDecimal> eficacias = new ArrayList<>();
        List<BigDecimal> eficiencias = new ArrayList<>();
        for (ProjectMetricsHolder h : proyectos) {
            addMetric(avances, h.metrics().avance(), true);
            addMetric(eficacias, h.metrics().eficacia(), h.tieneActividad());
            addMetric(eficiencias, h.metrics().eficiencia(), h.tieneActividad());
        }
        BigDecimal avancePromedio = average(avances);
        BigDecimal eficaciaPromedio = average(eficacias);
        BigDecimal eficienciaPromedio = average(eficiencias);

        long entregablesAtrasados = entregableRepositoryPort.countAtrasadosTotal(corte);
        long proximosAVencer = entregableRepositoryPort.countProximosActivos(corte, corte.plusDays(7));

        return new AnalyticsPortfolioDTO.ExecutiveMetrics(
                total,
                activos,
                cerrados,
                avancePromedio,
                eficaciaPromedio,
                eficienciaPromedio,
                entregablesAtrasados,
                proximosAVencer
        );
    }

    private List<AnalyticsPortfolioDTO.DependenciaMetrics> construirDependencias(List<ProjectMetricsHolder> proyectos) {
        return proyectos.stream()
                .collect(Collectors.groupingBy(
                        holder -> normalizeGroup(holder.metrics().dependencia()),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ProjectMetricsHolder> items = entry.getValue();
                    long riesgosTratados = items.stream().mapToLong(holder -> riesgoRepositoryPort.countByProyectoIdAndEstado(holder.proyecto().getId(), EstadoRiesgo.TRATADO)).sum();
                    long riesgosPendientes = items.stream().mapToLong(holder -> riesgoRepositoryPort.countByProyectoIdAndEstado(holder.proyecto().getId(), EstadoRiesgo.PENDIENTE)).sum();
                    long proyectosPeti = items.stream().filter(holder -> Boolean.TRUE.equals(holder.proyecto().getPeti())).count();

                    List<BigDecimal> avances = new ArrayList<>();
                    List<BigDecimal> eficacias = new ArrayList<>();
                    List<BigDecimal> eficiencias = new ArrayList<>();
                    for (ProjectMetricsHolder h : items) {
                        addMetric(avances, h.metrics().avance(), true);
                        addMetric(eficacias, h.metrics().eficacia(), h.tieneActividad());
                        addMetric(eficiencias, h.metrics().eficiencia(), h.tieneActividad());
                    }
                    return new AnalyticsPortfolioDTO.DependenciaMetrics(
                            entry.getKey(),
                            items.size(),
                            average(avances),
                            average(eficacias),
                            average(eficiencias),
                            proyectosPeti,
                            riesgosTratados,
                            riesgosPendientes
                    );
                })
                .sorted(Comparator.comparing(AnalyticsPortfolioDTO.DependenciaMetrics::dependencia, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<AnalyticsPortfolioDTO.EstrategiaMetrics> construirEstrategias(List<ProjectMetricsHolder> proyectos) {
        return proyectos.stream()
                .collect(Collectors.groupingBy(
                        holder -> Boolean.TRUE.equals(holder.proyecto().getPeti()) ? "PETI" : NO_PETI,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ProjectMetricsHolder> items = entry.getValue();
                    List<BigDecimal> avances = new ArrayList<>();
                    List<BigDecimal> eficacias = new ArrayList<>();
                    List<BigDecimal> eficiencias = new ArrayList<>();
                    for (ProjectMetricsHolder h : items) {
                        addMetric(avances, h.metrics().avance(), true);
                        addMetric(eficacias, h.metrics().eficacia(), h.tieneActividad());
                        addMetric(eficiencias, h.metrics().eficiencia(), h.tieneActividad());
                    }
                    return new AnalyticsPortfolioDTO.EstrategiaMetrics(
                            entry.getKey(),
                            "PETI".equals(entry.getKey()) ? "PETI" : "NO PETI",
                            items.size(),
                            average(avances),
                            average(eficacias),
                            average(eficiencias)
                    );
                })
                .sorted(Comparator.comparing(AnalyticsPortfolioDTO.EstrategiaMetrics::codigo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AnalyticsPortfolioDTO.RiskMetrics construirRiesgos() {
        List<Riesgo> riesgos = riesgoRepositoryPort.findAll();
        long total = riesgos.size();
        long tratados = riesgos.stream().filter(r -> EstadoRiesgo.TRATADO.equals(r.getEstado())).count();
        long pendientes = riesgos.stream().filter(r -> EstadoRiesgo.PENDIENTE.equals(r.getEstado())).count();
        BigDecimal indiceMitigacion = total == 0 ? BigDecimal.ZERO : ratioPercent(tratados, total);

        Map<String, String> colorByLevel = matrizRiesgoRepositoryPort.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(
                        item -> normalizeGroup(item.getNivelResultante()),
                        item -> normalizeGroup(item.getColor()),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<AnalyticsPortfolioDTO.RiskLevelMetrics> porNivel = riesgos.stream()
                .collect(Collectors.groupingBy(
                        riesgo -> normalizeGroup(riesgo.getNivel() != null ? riesgo.getNivel().name() : "SIN_NIVEL"),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new AnalyticsPortfolioDTO.RiskLevelMetrics(
                        entry.getKey(),
                        colorByLevel.getOrDefault(entry.getKey(), "#94a3b8"),
                        entry.getValue()))
                .sorted(Comparator.comparingInt(metric -> riskLevelOrder(metric.nivel())))
                .toList();

        return new AnalyticsPortfolioDTO.RiskMetrics(total, tratados, pendientes, indiceMitigacion, porNivel);
    }

    private AnalyticsPortfolioDTO.FuragMetrics construirFurag(List<ProjectMetricsHolder> proyectos) {
        List<AnalyticsPortfolioDTO.FuragProjectMetrics> proyectosFurag = new ArrayList<>();
        long totalObligatorias = 0L;
        long totalCompletas = 0L;
        long proyectosConFurag = 0L;
        long proyectosCumplidos = 0L;

        for (ProjectMetricsHolder holder : proyectos) {
            String proyectoId = holder.proyecto().getId();
            long obligatorias = furagRespuestaRepositoryPort.countByProyectoIdAndObligatoriaTrue(proyectoId);
            long completas = furagRespuestaRepositoryPort.countByProyectoIdAndObligatoriaTrueAndRespuestaIsNotNull(proyectoId);
            totalObligatorias += obligatorias;
            totalCompletas += completas;
            if (obligatorias > 0) {
                proyectosConFurag++;
            }
            if (obligatorias > 0 && completas >= obligatorias) {
                proyectosCumplidos++;
            }
            proyectosFurag.add(new AnalyticsPortfolioDTO.FuragProjectMetrics(
                    proyectoId,
                    holder.metrics().nombre(),
                    obligatorias == 0 ? BigDecimal.ZERO : ratioPercent(completas, obligatorias),
                    completas,
                    obligatorias
            ));
        }

        BigDecimal coberturaPromedio = proyectosFurag.stream()
                .map(AnalyticsPortfolioDTO.FuragProjectMetrics::cobertura)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!proyectosFurag.isEmpty()) {
            coberturaPromedio = coberturaPromedio.divide(BigDecimal.valueOf(proyectosFurag.size()), 4, RoundingMode.HALF_UP);
        }

        return new AnalyticsPortfolioDTO.FuragMetrics(
                proyectosConFurag,
                proyectosCumplidos,
                totalCompletas,
                totalObligatorias,
                coberturaPromedio,
                proyectosFurag
        );
    }

    private ProjectMetricsHolder construirProyectoMetrics(Proyecto proyecto, LocalDate corte) {
        ProyectoAvanceResponseDTO avance = resolverAvance(proyecto, corte);
        long riesgosTotal = riesgoRepositoryPort.countByProyectoId(proyecto.getId());
        long riesgosTratados = riesgoRepositoryPort.countByProyectoIdAndEstado(proyecto.getId(), EstadoRiesgo.TRATADO);
        long riesgosPendientes = riesgoRepositoryPort.countByProyectoIdAndEstado(proyecto.getId(), EstadoRiesgo.PENDIENTE);
        BigDecimal indiceMitigacion = riesgosTotal == 0 ? BigDecimal.ZERO : ratioPercent(riesgosTratados, riesgosTotal);
        boolean tieneActividad = tieneEntregablesIniciados(proyecto, corte);

        String viabEstado = proyecto.getViabilidadEstado() != null
                ? proyecto.getViabilidadEstado().name() : "PENDIENTE";

        AnalyticsPortfolioDTO.ProjectMetrics metrics = new AnalyticsPortfolioDTO.ProjectMetrics(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDependencia(),
                estrategiaLabel(proyecto),
                proyecto.getPeti(),
                avance.avanceTotal(),
                tieneActividad ? avance.eficacia() : null,
                tieneActividad ? avance.eficiencia() : null,
                avance.estado(),
                avance.entregablesAtrasados(),
                furagCoverage(proyecto.getId()),
                indiceMitigacion,
                proyecto.getDocumentosCargados(),
                proyecto.getDocumentosVerificados(),
                viabEstado,
                proyecto.getCronogramaPdf() != null && !proyecto.getCronogramaPdf().isBlank(),
                proyecto.getActaConstitucionPdf() != null && !proyecto.getActaConstitucionPdf().isBlank(),
                Boolean.TRUE.equals(proyecto.getTienePlanComunicaciones())
                        || (proyecto.getPlanComunicacionesPdf() != null && !proyecto.getPlanComunicacionesPdf().isBlank()),
                proyecto.getRegistradoInicialPor(),
                proyecto.getCorreoDirector()
        );
        return new ProjectMetricsHolder(proyecto, metrics, riesgosTratados, riesgosPendientes, tieneActividad);
    }

    private boolean tieneEntregablesIniciados(Proyecto proyecto, LocalDate corte) {
        return proyecto.getFases().stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> fase.getHitos().stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> hito.getEntregables().stream())
                .filter(Objects::nonNull)
                .anyMatch(e -> e.getFechaInicio() != null && !e.getFechaInicio().isAfter(corte));
    }

    private ProyectoAvanceResponseDTO resolverAvance(Proyecto proyecto, LocalDate corte) {
        if (proyecto.esEstadoTerminal()) {
            ActaCierre acta = actaCierreRepositoryPort.findByProyectoId(proyecto.getId()).orElse(null);
            if (acta != null && acta.getSnapshotJson() != null && !acta.getSnapshotJson().isBlank()) {
                return progressMetricsService.deserializar(acta.getSnapshotJson());
            }
        }
        return progressMetricsService.construir(proyecto, corte);
    }

    private BigDecimal furagCoverage(String proyectoId) {
        long obligatorias = furagRespuestaRepositoryPort.countByProyectoIdAndObligatoriaTrue(proyectoId);
        long completas = furagRespuestaRepositoryPort.countByProyectoIdAndObligatoriaTrueAndRespuestaIsNotNull(proyectoId);
        if (obligatorias == 0) {
            return BigDecimal.ZERO;
        }
        return ratioPercent(completas, obligatorias);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> filtered = values.stream().filter(Objects::nonNull).toList();
        if (filtered.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal sum = filtered.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(filtered.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratioPercent(long numerador, long denominador) {
        if (denominador <= 0L) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
    }

    private String estrategiaLabel(Proyecto proyecto) {
        if (proyecto == null) {
            return NO_PETI;
        }
        return Boolean.TRUE.equals(proyecto.getPeti()) ? "PETI" : NO_PETI;
    }

    private String normalizeGroup(String value) {
        if (value == null || value.isBlank()) {
            return "SIN_CLASIFICAR";
        }
        return value.trim();
    }

    private int riskLevelOrder(String nivel) {
        String normalized = normalizeGroup(nivel).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BAJO" -> 1;
            case "MODERADO" -> 2;
            case "ALTO" -> 3;
            case "EXTREMO", "CRITICO" -> 4;
            default -> 99;
        };
    }

    private record ProjectMetricsHolder(
            Proyecto proyecto,
            AnalyticsPortfolioDTO.ProjectMetrics metrics,
            long riesgosTratados,
            long riesgosPendientes,
            boolean tieneActividad) {
    }
}
