package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.analytics.AnalyticsPortfolioDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.ActaCierre;
import com.proyecta.api_gestion.model.FuragRespuesta;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Riesgo;
import com.proyecta.api_gestion.model.config.MatrizRiesgo;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.repository.ActaCierreRepository;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FuragRespuestaRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import com.proyecta.api_gestion.repository.config.MatrizRiesgoRepository;
import com.proyecta.api_gestion.service.interfaces.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final RiesgoRepository riesgoRepository;
    private final FuragRespuestaRepository furagRespuestaRepository;
    private final ActaCierreRepository actaCierreRepository;
    private final ProjectProgressMetricsService progressMetricsService;
    private final MatrizRiesgoRepository matrizRiesgoRepository;

    public AnalyticsServiceImpl(
            ProyectoRepository proyectoRepository,
            EntregableRepository entregableRepository,
            RiesgoRepository riesgoRepository,
            FuragRespuestaRepository furagRespuestaRepository,
            ActaCierreRepository actaCierreRepository,
            ProjectProgressMetricsService progressMetricsService,
            MatrizRiesgoRepository matrizRiesgoRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.furagRespuestaRepository = furagRespuestaRepository;
        this.actaCierreRepository = actaCierreRepository;
        this.progressMetricsService = progressMetricsService;
        this.matrizRiesgoRepository = matrizRiesgoRepository;
    }

    @Override
    public AnalyticsPortfolioDTO getPortfolioAnalytics() {
        LocalDate corte = LocalDate.now();
        List<ProjectMetricsHolder> proyectos = proyectoRepository.findAll().stream()
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

    private AnalyticsPortfolioDTO.ExecutiveMetrics construirExecutiveMetrics(List<ProjectMetricsHolder> proyectos, LocalDate corte) {
        long total = proyectos.size();
        long activos = proyectos.stream().filter(holder -> holder.proyecto().getEstado() != null
                && !holder.proyecto().esEstadoTerminal()).count();
        long cerrados = total - activos;

        BigDecimal avancePromedio = average(proyectos.stream().map((ProjectMetricsHolder holder) -> holder.metrics().avance()).toList());
        BigDecimal eficaciaPromedio = average(proyectos.stream().map((ProjectMetricsHolder holder) -> holder.metrics().eficacia()).toList());
        BigDecimal eficienciaPromedio = average(proyectos.stream().map((ProjectMetricsHolder holder) -> holder.metrics().eficiencia()).toList());

        long entregablesAtrasados = entregableRepository.countAtrasadosTotal(corte);
        long proximosAVencer = entregableRepository.countProximosActivos(corte, corte.plusDays(7));

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
                    long riesgosTratados = items.stream().mapToLong(holder -> riesgoRepository.countByProyecto_IdAndEstado(holder.proyecto().getId(), EstadoRiesgo.TRATADO)).sum();
                    long riesgosPendientes = items.stream().mapToLong(holder -> riesgoRepository.countByProyecto_IdAndEstado(holder.proyecto().getId(), EstadoRiesgo.PENDIENTE)).sum();
                    long proyectosPeti = items.stream().filter(holder -> Boolean.TRUE.equals(holder.proyecto().getPeti())).count();
                    return new AnalyticsPortfolioDTO.DependenciaMetrics(
                            entry.getKey(),
                            items.size(),
                            average(items.stream().map(holder -> holder.metrics().avance()).toList()),
                            average(items.stream().map(holder -> holder.metrics().eficacia()).toList()),
                            average(items.stream().map(holder -> holder.metrics().eficiencia()).toList()),
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
                        holder -> Boolean.TRUE.equals(holder.proyecto().getPeti()) ? "PETI" : "NO_PETI",
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ProjectMetricsHolder> items = entry.getValue();
                    return new AnalyticsPortfolioDTO.EstrategiaMetrics(
                            entry.getKey(),
                            "PETI".equals(entry.getKey()) ? "PETI" : "NO PETI",
                            items.size(),
                            average(items.stream().map(holder -> holder.metrics().avance()).toList()),
                            average(items.stream().map(holder -> holder.metrics().eficacia()).toList()),
                            average(items.stream().map(holder -> holder.metrics().eficiencia()).toList())
                    );
                })
                .sorted(Comparator.comparing(AnalyticsPortfolioDTO.EstrategiaMetrics::codigo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AnalyticsPortfolioDTO.RiskMetrics construirRiesgos() {
        List<Riesgo> riesgos = riesgoRepository.findAll();
        long total = riesgos.size();
        long tratados = riesgos.stream().filter(r -> EstadoRiesgo.TRATADO.equals(r.getEstado())).count();
        long pendientes = riesgos.stream().filter(r -> EstadoRiesgo.PENDIENTE.equals(r.getEstado())).count();
        BigDecimal indiceMitigacion = total == 0 ? BigDecimal.ZERO : ratioPercent(tratados, total);

        Map<String, String> colorByLevel = matrizRiesgoRepository.findAllByOrderByIdAsc().stream()
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
            long obligatorias = furagRespuestaRepository.countByProyecto_IdAndObligatoriaTrue(proyectoId);
            long completas = furagRespuestaRepository.countByProyecto_IdAndObligatoriaTrueAndRespuestaIsNotNull(proyectoId);
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
        long riesgosTotal = riesgoRepository.countByProyecto_Id(proyecto.getId());
        long riesgosTratados = riesgoRepository.countByProyecto_IdAndEstado(proyecto.getId(), EstadoRiesgo.TRATADO);
        long riesgosPendientes = riesgoRepository.countByProyecto_IdAndEstado(proyecto.getId(), EstadoRiesgo.PENDIENTE);
        BigDecimal indiceMitigacion = riesgosTotal == 0 ? BigDecimal.ZERO : ratioPercent(riesgosTratados, riesgosTotal);

        AnalyticsPortfolioDTO.ProjectMetrics metrics = new AnalyticsPortfolioDTO.ProjectMetrics(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDependencia(),
                estrategiaLabel(proyecto),
                proyecto.getPeti(),
                avance.avanceTotal(),
                avance.eficacia(),
                avance.eficiencia(),
                avance.estado(),
                avance.entregablesAtrasados(),
                furagCoverage(proyecto.getId()),
                indiceMitigacion
        );
        return new ProjectMetricsHolder(proyecto, metrics, riesgosTratados, riesgosPendientes);
    }

    private ProyectoAvanceResponseDTO resolverAvance(Proyecto proyecto, LocalDate corte) {
        if (proyecto.esEstadoTerminal()) {
            ActaCierre acta = actaCierreRepository.findByProyectoId(proyecto.getId()).orElse(null);
            if (acta != null && acta.getSnapshotJson() != null && !acta.getSnapshotJson().isBlank()) {
                return progressMetricsService.deserializar(acta.getSnapshotJson());
            }
        }
        return progressMetricsService.construir(proyecto, corte);
    }

    private BigDecimal furagCoverage(String proyectoId) {
        long obligatorias = furagRespuestaRepository.countByProyecto_IdAndObligatoriaTrue(proyectoId);
        long completas = furagRespuestaRepository.countByProyecto_IdAndObligatoriaTrueAndRespuestaIsNotNull(proyectoId);
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
            return "NO_PETI";
        }
        return Boolean.TRUE.equals(proyecto.getPeti()) ? "PETI" : "NO_PETI";
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
            long riesgosPendientes) {
    }
}
