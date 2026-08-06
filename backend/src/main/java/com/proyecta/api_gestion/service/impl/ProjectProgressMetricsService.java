package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.avance.EntregableAvanceDTO;
import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.DocumentoVersion;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.DocumentoVersionEstado;
import com.proyecta.api_gestion.repository.DocumentoVersionRepository;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class ProjectProgressMetricsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static boolean isEntregableIniciado(Entregable e, LocalDate corte) {
        return e.getFechaInicio() != null && !e.getFechaInicio().isAfter(corte);
    }

    private static BigDecimal clampPct(BigDecimal value) {
        if (value == null) return ZERO;
        if (value.compareTo(ZERO) < 0) return ZERO;
        if (value.compareTo(HUNDRED) > 0) return HUNDRED;
        return value;
    }

    private static BigDecimal clampRatio(BigDecimal value) {
        if (value == null) return ZERO;
        if (value.compareTo(ZERO) < 0) return ZERO;
        if (value.compareTo(ONE) > 0) return ONE;
        return value;
    }

    private final ObjectMapper objectMapper;
    private final DocumentoVersionRepository documentoVersionRepository;

    public ProjectProgressMetricsService(ObjectMapper objectMapper,
                                         DocumentoVersionRepository documentoVersionRepository) {
        this.objectMapper = objectMapper;
        this.documentoVersionRepository = documentoVersionRepository;
    }

    public ProyectoAvanceResponseDTO construir(Proyecto proyecto, LocalDate corte) {
        List<Fase> fasesProyecto = fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Fase::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<FaseAvanceDTO> fases = fasesProyecto.stream()
                .map(fase -> construirFase(fase, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoFase(fases, FaseAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoFase(fases, FaseAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = clampPct(progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP));
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        long entregablesProgramadosAlCorte = contarEntregablesProgramadosAlCorte(proyecto, corte);
        long entregablesEntregadosAlCorte = contarEntregablesEntregadosAlCorte(proyecto, corte);
        long entregablesEntregadosATiempo = contarEntregablesEntregadosATiempo(proyecto, corte);
        BigDecimal eficaciaCorte = clampRatio(calcularRatio(entregablesEntregadosAlCorte, entregablesProgramadosAlCorte));
        BigDecimal eficienciaCorte = clampRatio(calcularRatio(entregablesEntregadosATiempo, entregablesProgramadosAlCorte));
        long entregablesConformes = contarEntregablesConformes(proyecto, corte);
        long entregablesTotal = contarEntregablesTotales(proyecto, corte);
        long entregablesAtrasados = contarEntregablesAtrasados(proyecto, corte);
        long proximosAVencer = contarEntregablesPorVencer(proyecto, corte);

        boolean tieneActividad = tieneEntregablesIniciados(proyecto, corte);

        return new ProyectoAvanceResponseDTO(
                proyecto.getId(),
                proyecto.getId(),
                proyecto.getNombre(),
                progresoProgramado,
                progresoEjecutado,
                tieneActividad ? diferencia : ZERO,
                tieneActividad ? eficaciaCorte : ZERO,
                tieneActividad ? eficienciaCorte : ZERO,
                estado,
                progresoEjecutado,
                entregablesConformes,
                entregablesTotal,
                entregablesAtrasados,
                proximosAVencer,
                tieneActividad ? entregablesProgramadosAlCorte : 0L,
                tieneActividad ? entregablesEntregadosAlCorte : 0L,
                tieneActividad ? entregablesEntregadosATiempo : 0L,
                corte,
                fases
        );
    }

    public String serializar(ProyectoAvanceResponseDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible serializar el snapshot de avance del proyecto.", ex);
        }
    }

    public ProyectoAvanceResponseDTO deserializar(String json) {
        try {
            return objectMapper.readValue(json, ProyectoAvanceResponseDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible leer el snapshot de avance del proyecto.", ex);
        }
    }

    private FaseAvanceDTO construirFase(Fase fase, LocalDate corte) {
        List<Hito> hitosFase = hitosSeguros(fase).stream()
                .filter(Objects::nonNull)
                .sorted(ProjectHierarchyOrdering.HITOS_BY_ORDEN)
                .toList();

        List<HitoAvanceDTO> hitos = hitosFase.stream()
                .map(hito -> construirHito(hito, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoHito(hitos, HitoAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoHito(hitos, HitoAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = clampPct(progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP));
        BigDecimal eficacia = clampRatio(calcularEficacia(progresoProgramado, progresoEjecutado));
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        return new FaseAvanceDTO(
                fase.getId(),
                fase.getNombre(),
                fase.getDescripcion(),
                normalizarPonderacion(fase.getPonderacion()),
                progresoProgramado,
                progresoEjecutado,
                diferencia,
                eficacia,
                estado,
                progresoEjecutado,
                hitos
        );
    }

    private HitoAvanceDTO construirHito(Hito hito, LocalDate corte) {
        List<Entregable> entregablesHito = entregablesSeguros(hito).stream()
                .filter(Objects::nonNull)
                .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_ORDEN)
                .toList();

        List<EntregableAvanceDTO> entregables = entregablesHito.stream()
                .map(entregable -> construirEntregable(entregable, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoEntregable(entregables, EntregableAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoEntregable(entregables, EntregableAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = clampPct(progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP));
        BigDecimal eficacia = clampRatio(calcularEficacia(progresoProgramado, progresoEjecutado));
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        return new HitoAvanceDTO(
                hito.getId(),
                hito.getNombre(),
                hito.getDescripcion(),
                normalizarPonderacion(hito.getPonderacion()),
                progresoProgramado,
                progresoEjecutado,
                diferencia,
                eficacia,
                estado,
                progresoEjecutado,
                entregables
        );
    }

    private EntregableAvanceDTO construirEntregable(Entregable entregable, LocalDate corte) {
        boolean iniciado = isEntregableIniciado(entregable, corte);
        boolean vencido = iniciado && entregable.getFechaLimite() != null && !entregable.getFechaLimite().isAfter(corte);
        boolean conforme = iniciado && entregable.esConforme();

        BigDecimal progresoProgramado = vencido ? HUNDRED : ZERO;
        BigDecimal progresoEjecutado = conforme ? HUNDRED : ZERO;
        BigDecimal diferencia = clampPct(progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP));
        BigDecimal eficacia = clampRatio(calcularEficacia(progresoProgramado, progresoEjecutado));
        Long diasAtraso = iniciarDiasAtraso(entregable, iniciado, corte);
        String estadoCodigo = entregable.getEstadoCodigo();
        String estado = iniciado
                ? construirEstadoEntregable(estadoCodigo, conforme, diasAtraso, entregable.getFechaLimite(), corte)
                : "NO_INICIADO";
        String evidenciaNombre = nombreEvidenciaActual(entregable);

        return new EntregableAvanceDTO(
                entregable.getId(),
                entregable.getNombre(),
                entregable.getDescripcion(),
                normalizarPonderacion(entregable.getPonderacion()),
                entregable.getFechaInicio(),
                entregable.getFechaLimite(),
                progresoProgramado,
                progresoEjecutado,
                diferencia,
                eficacia,
                estado,
                diasAtraso,
                entregable.getFechaEntregaReal(),
                evidenciaNombre,
                entregable.getArchivoPdf() != null
                        ? "/api/v1/proyectos/" + entregable.getHito().getFase().getProyecto().getId() + "/avance/entregables/" + entregable.getId() + "/evidencia"
                        : null,
                progresoEjecutado,
                diasAtraso,
                estadoCodigo,
                entregable.getObservacionRevision()
        );
    }

    private String nombreEvidenciaActual(Entregable entregable) {
        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            return null;
        }

        return documentoVersionRepository.findFirstByEntregableIdAndEstadoOrderByNumeroVersionDesc(
                        entregable.getId(),
                        DocumentoVersionEstado.ACTUAL
                )
                .map(DocumentoVersion::getNombreArchivoOriginal)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .orElse(entregable.getArchivoPdf());
    }

    private BigDecimal agregarPorPesoEntregable(List<EntregableAvanceDTO> entregables,
                                                java.util.function.Function<EntregableAvanceDTO, BigDecimal> selector) {
        BigDecimal totalPeso = entregables.stream()
                .map(EntregableAvanceDTO::ponderacion)
                .reduce(ZERO, BigDecimal::add);
        if (totalPeso.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return entregables.stream()
                .map(entregable -> entregable.ponderacion()
                        .divide(totalPeso, 10, RoundingMode.HALF_UP)
                        .multiply(selector.apply(entregable).divide(HUNDRED, 10, RoundingMode.HALF_UP)))
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal agregarPorPesoHito(List<HitoAvanceDTO> hitos,
                                          java.util.function.Function<HitoAvanceDTO, BigDecimal> selector) {
        BigDecimal totalPeso = hitos.stream()
                .map(HitoAvanceDTO::ponderacion)
                .reduce(ZERO, BigDecimal::add);
        if (totalPeso.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return hitos.stream()
                .map(hito -> hito.ponderacion()
                        .divide(totalPeso, 10, RoundingMode.HALF_UP)
                        .multiply(selector.apply(hito).divide(HUNDRED, 10, RoundingMode.HALF_UP)))
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal agregarPorPesoFase(List<FaseAvanceDTO> fases,
                                          java.util.function.Function<FaseAvanceDTO, BigDecimal> selector) {
        BigDecimal totalPeso = fases.stream()
                .map(FaseAvanceDTO::ponderacion)
                .reduce(ZERO, BigDecimal::add);
        if (totalPeso.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return fases.stream()
                .map(fase -> fase.ponderacion()
                        .divide(totalPeso, 10, RoundingMode.HALF_UP)
                        .multiply(selector.apply(fase).divide(HUNDRED, 10, RoundingMode.HALF_UP)))
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal normalizarPonderacion(BigDecimal ponderacion) {
        return ponderacion == null ? ZERO.setScale(2, RoundingMode.HALF_UP) : ponderacion.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal escalar(BigDecimal value) {
        return value.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularEficacia(BigDecimal progresoProgramado, BigDecimal progresoEjecutado) {
        if (progresoProgramado.compareTo(ZERO) == 0) {
            return ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return progresoEjecutado.divide(progresoProgramado, 4, RoundingMode.HALF_UP);
    }

    private Long iniciarDiasAtraso(Entregable entregable, boolean iniciado, LocalDate corte) {
        if (!iniciado) {
            return 0L;
        }
        return calcularDiasAtraso(entregable, corte);
    }

    private Long calcularDiasAtraso(Entregable entregable, LocalDate corte) {
        if (entregable.getFechaLimite() == null) {
            return 0L;
        }

        if (entregable.getFechaEntregaReal() != null) {
            return ChronoUnit.DAYS.between(entregable.getFechaEntregaReal(), entregable.getFechaLimite());
        }

        if (entregable.getFechaLimite().isBefore(corte)) {
            return ChronoUnit.DAYS.between(corte, entregable.getFechaLimite());
        }

        return 0L;
    }

    private BigDecimal calcularRatio(long numerador, long denominador) {
        if (denominador <= 0L) {
            return ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerador)
                .divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
    }

    private boolean tieneEntregablesIniciados(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .anyMatch(e -> isEntregableIniciado(e, corte));
    }

    private long contarEntregablesProgramadosAlCorte(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(entregable -> entregable.getFechaLimite() != null
                        && !entregable.getFechaLimite().isAfter(corte))
                .count();
    }

    private long contarEntregablesEntregadosAlCorte(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(Entregable::esConforme)
                .filter(entregable -> entregable.getFechaEntregaReal() == null || !entregable.getFechaEntregaReal().isAfter(corte))
                .count();
    }

    private long contarEntregablesEntregadosATiempo(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(Entregable::esConforme)
                .filter(entregable -> entregable.getFechaEntregaReal() == null || !entregable.getFechaEntregaReal().isAfter(corte))
                .filter(entregable -> entregable.getFechaEntregaReal() != null
                        && entregable.getFechaLimite() != null
                        && !entregable.getFechaEntregaReal().isAfter(entregable.getFechaLimite()))
                .count();
    }

    private String construirEstadoEntregable(String estadoCodigo, boolean conforme, Long diasAtraso, LocalDate fechaLimite, LocalDate corte) {
        if ("RECHAZADO".equals(estadoCodigo)) {
            return "RECHAZADO";
        }
        if ("EN_PROCESO".equals(estadoCodigo) || "COMPLETADO".equals(estadoCodigo)) {
            return "EN_REVISION";
        }
        if (conforme && diasAtraso != null && diasAtraso >= 0) {
            return "EN_TIEMPO";
        }
        if (diasAtraso != null && diasAtraso < 0) {
            return "ATRASO";
        }
        if (!conforme && fechaLimite != null) {
            long diasRestantes = ChronoUnit.DAYS.between(corte, fechaLimite);
            if (diasRestantes >= 0 && diasRestantes <= 8) {
                return "ALERTA";
            }
        }
        return "PENDIENTE";
    }

    private long contarEntregablesConformes(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(Entregable::esConforme)
                .count();
    }

    private long contarEntregablesTotales(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .count();
    }

    private long contarEntregablesAtrasados(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(entregable -> !entregable.esConforme()
                        && entregable.getFechaLimite() != null
                        && ChronoUnit.DAYS.between(corte, entregable.getFechaLimite()) < 0)
                .count();
    }

    private long contarEntregablesPorVencer(Proyecto proyecto, LocalDate corte) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(e -> isEntregableIniciado(e, corte))
                .filter(entregable -> !entregable.esConforme()
                        && entregable.getFechaLimite() != null)
                .mapToLong(entregable -> ChronoUnit.DAYS.between(corte, entregable.getFechaLimite()))
                .filter(dias -> dias >= 0 && dias <= 8)
                .count();
    }

    private List<Fase> fasesSeguras(Proyecto proyecto) {
        return proyecto.getFases() == null ? List.of() : proyecto.getFases();
    }

    private List<Hito> hitosSeguros(Fase fase) {
        return fase.getHitos() == null ? List.of() : fase.getHitos();
    }

    private List<Entregable> entregablesSeguros(Hito hito) {
        return hito.getEntregables() == null ? List.of() : hito.getEntregables();
    }
}
