package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.IndicadoresEficienciaDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
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

        long programadosAlCorte = todosEntregables.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .count();

        long entregadosAlCorte = todosEntregables.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .filter(Entregable::esConforme)
                .count();

        long entregadosATiempo = todosEntregables.stream()
                .filter(e -> e.getFechaLimite() != null && !e.getFechaLimite().isAfter(fechaCorte))
                .filter(Entregable::esConforme)
                .filter(e -> e.getFechaEntregaReal() != null
                        && !e.getFechaEntregaReal().isAfter(e.getFechaLimite()))
                .count();

        BigDecimal eficacia = calcularRatio(entregadosAlCorte, programadosAlCorte);
        BigDecimal eficiencia = calcularRatio(entregadosATiempo, programadosAlCorte);

        return new IndicadoresEficienciaDTO(
                proyecto.getId(),
                proyecto.getNombre(),
                fechaCorte,
                programadosAlCorte,
                entregadosAlCorte,
                entregadosATiempo,
                eficacia,
                eficiencia,
                todosEntregables.size()
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
}
