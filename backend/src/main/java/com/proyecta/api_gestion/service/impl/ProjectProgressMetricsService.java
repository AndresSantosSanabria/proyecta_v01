package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.avance.EntregableAvanceDTO;
import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectProgressMetricsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ObjectMapper objectMapper;

    public ProjectProgressMetricsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProyectoAvanceResponseDTO construir(Proyecto proyecto, LocalDate corte) {
        List<Fase> fasesProyecto = proyecto.getFases() == null ? List.of() : proyecto.getFases();
        List<FaseAvanceDTO> fases = fasesProyecto.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Fase::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(fase -> construirFase(fase, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoFase(fases, FaseAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoFase(fases, FaseAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eficacia = calcularEficacia(progresoProgramado, progresoEjecutado);
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        long entregablesConformes = contarEntregablesConformes(proyecto);
        long entregablesTotal = contarEntregablesTotales(proyecto);
        long entregablesAtrasados = contarEntregablesAtrasados(proyecto, corte);
        long proximosAVencer = contarEntregablesPorVencer(proyecto, corte);

        return new ProyectoAvanceResponseDTO(
                proyecto.getId(),
                proyecto.getId(),
                proyecto.getNombre(),
                progresoProgramado,
                progresoEjecutado,
                diferencia,
                eficacia,
                estado,
                progresoEjecutado,
                entregablesConformes,
                entregablesTotal,
                entregablesAtrasados,
                proximosAVencer,
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
        List<Hito> hitosFase = fase.getHitos() == null ? List.of() : fase.getHitos();
        List<HitoAvanceDTO> hitos = hitosFase.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Hito::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(hito -> construirHito(hito, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoHito(hitos, HitoAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoHito(hitos, HitoAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eficacia = calcularEficacia(progresoProgramado, progresoEjecutado);
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        return new FaseAvanceDTO(
                fase.getId(),
                fase.getNombre(),
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
        List<Entregable> entregablesHito = hito.getEntregables() == null ? List.of() : hito.getEntregables();
        List<EntregableAvanceDTO> entregables = entregablesHito.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Entregable::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(entregable -> construirEntregable(entregable, corte))
                .toList();

        BigDecimal progresoProgramado = escalar(agregarPorPesoEntregable(entregables, EntregableAvanceDTO::progresoProgramado));
        BigDecimal progresoEjecutado = escalar(agregarPorPesoEntregable(entregables, EntregableAvanceDTO::progresoEjecutado));
        BigDecimal diferencia = progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eficacia = calcularEficacia(progresoProgramado, progresoEjecutado);
        String estado = diferencia.compareTo(ZERO) <= 0 ? "EN_TIEMPO" : "ATRASO";

        return new HitoAvanceDTO(
                hito.getId(),
                hito.getNombre(),
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
        boolean vencido = entregable.getFechaLimite() != null && !entregable.getFechaLimite().isAfter(corte);
        boolean conforme = entregable.esConforme();

        BigDecimal progresoProgramado = vencido ? HUNDRED : ZERO;
        BigDecimal progresoEjecutado = conforme ? HUNDRED : ZERO;
        BigDecimal diferencia = progresoProgramado.subtract(progresoEjecutado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eficacia = progresoProgramado.compareTo(ZERO) == 0
                ? ONE.setScale(2, RoundingMode.HALF_UP)
                : progresoEjecutado.divide(progresoProgramado, 4, RoundingMode.HALF_UP);
        Long diasAtraso = calcularDiasAtraso(entregable, corte);
        String estado = construirEstadoEntregable(conforme, diasAtraso);

        return new EntregableAvanceDTO(
                entregable.getId(),
                entregable.getNombre(),
                normalizarPonderacion(entregable.getPonderacion()),
                entregable.getFechaLimite(),
                progresoProgramado,
                progresoEjecutado,
                diferencia,
                eficacia,
                estado,
                diasAtraso,
                entregable.getFechaEntregaReal(),
                entregable.getArchivoPdf(),
                entregable.getArchivoPdf() != null
                        ? "/api/v1/proyectos/" + entregable.getHito().getFase().getProyecto().getId() + "/avance/entregables/" + entregable.getId() + "/evidencia"
                        : null,
                progresoEjecutado,
                diasAtraso,
                estado
        );
    }

    private BigDecimal agregarPorPesoEntregable(List<EntregableAvanceDTO> entregables, java.util.function.Function<EntregableAvanceDTO, BigDecimal> selector) {
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

    private BigDecimal agregarPorPesoHito(List<HitoAvanceDTO> hitos, java.util.function.Function<HitoAvanceDTO, BigDecimal> selector) {
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

    private BigDecimal agregarPorPesoFase(List<FaseAvanceDTO> fases, java.util.function.Function<FaseAvanceDTO, BigDecimal> selector) {
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

    private Long calcularDiasAtraso(Entregable entregable, LocalDate corte) {
        if (entregable.getFechaLimite() == null) {
            return 0L;
        }

        if (entregable.getFechaEntregaReal() != null) {
            return ChronoUnit.DAYS.between(entregable.getFechaEntregaReal(), entregable.getFechaLimite());
        }

        return ChronoUnit.DAYS.between(corte, entregable.getFechaLimite());
    }

    private String construirEstadoEntregable(boolean conforme, Long diasAtraso) {
        if (conforme && diasAtraso != null && diasAtraso >= 0) {
            return "EN_TIEMPO";
        }
        if (diasAtraso != null && diasAtraso < 0) {
            return "ATRASO";
        }
        return "PENDIENTE";
    }

    private long contarEntregablesConformes(Proyecto proyecto) {
        return fasesSeguras(proyecto).stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> hitosSeguros(fase).stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> entregablesSeguros(hito).stream())
                .filter(Objects::nonNull)
                .filter(Entregable::esConforme)
                .count();
    }

    private long contarEntregablesTotales(Proyecto proyecto) {
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
