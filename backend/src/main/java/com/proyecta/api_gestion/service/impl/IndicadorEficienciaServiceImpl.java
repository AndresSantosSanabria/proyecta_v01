package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.IndicadoresEficienciaDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IIndicadorEficienciaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class IndicadorEficienciaServiceImpl implements IIndicadorEficienciaService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final ProyectoRepository proyectoRepository;

    public IndicadorEficienciaServiceImpl(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @Override
    public IndicadoresEficienciaDTO calcular(String proyectoId, LocalDate fechaCorte) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<Entregable> todosEntregables = proyecto.getFases().stream()
                .filter(Objects::nonNull)
                .flatMap(fase -> fase.getHitos().stream())
                .filter(Objects::nonNull)
                .flatMap(hito -> hito.getEntregables().stream())
                .filter(Objects::nonNull)
                .toList();

        List<Entregable> iniciados = todosEntregables.stream()
                .filter(e -> isEntregableIniciado(e, fechaCorte))
                .toList();

        long programadosAlCorte = iniciados.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .count();

        long entregadosAlCorte = iniciados.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .filter(e -> e.getFechaEntregaReal() != null)
                .filter(e -> !EstadoEntregable.RECHAZADO.equals(e.getEstado()))
                .count();

        long entregadosATiempo = iniciados.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .filter(e -> e.getFechaEntregaReal() != null)
                .filter(e -> !EstadoEntregable.RECHAZADO.equals(e.getEstado()))
                .filter(e -> !e.getFechaEntregaReal().isAfter(e.getFechaLimite()))
                .count();

        BigDecimal eficacia = clampRatio(calcularRatio(entregadosAlCorte, programadosAlCorte));
        BigDecimal eficiencia = clampRatio(calcularRatio(entregadosATiempo, entregadosAlCorte));

        return new IndicadoresEficienciaDTO(
                proyecto.getId(),
                proyecto.getNombre(),
                fechaCorte,
                programadosAlCorte,
                entregadosAlCorte,
                entregadosATiempo,
                eficacia,
                eficiencia,
                iniciados.size()
        );
    }

    private BigDecimal calcularRatio(long numerador, long denominador) {
        if (denominador <= 0L) {
            return ZERO;
        }
        return BigDecimal.valueOf(numerador)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
    }

    private static boolean isEntregableIniciado(Entregable e, LocalDate corte) {
        return e.getFechaInicio() != null && !e.getFechaInicio().isAfter(corte);
    }

    private static BigDecimal clampRatio(BigDecimal value) {
        if (value == null) return ZERO;
        if (value.compareTo(ZERO) < 0) return ZERO;
        if (value.compareTo(HUNDRED) > 0) return HUNDRED;
        return value;
    }
}
