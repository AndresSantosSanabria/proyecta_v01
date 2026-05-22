package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AvanceCalculatorServiceImpl implements IProgressCalculator {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;

    public AvanceCalculatorServiceImpl(ProyectoRepository proyectoRepository,
                                       FaseRepository faseRepository,
                                       HitoRepository hitoRepository,
                                       EntregableRepository entregableRepository) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceProyecto(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);

        BigDecimal totalAvance = calcularAvanceProyecto(fases);
        proyecto.setAvanceTotal(totalAvance);
        proyectoRepository.save(proyecto);

        return totalAvance;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceFase(Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada: " + faseId));

        BigDecimal totalAvance = calcularAvanceFase(fase);
        fase.setAvanceCalculado(totalAvance);
        faseRepository.save(fase);

        if (fase.getProyecto() != null) {
            calcularYActualizarAvanceProyecto(fase.getProyecto().getId());
        }

        return totalAvance;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceHito(Integer hitoId) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new RuntimeException("Hito no encontrado: " + hitoId));

        BigDecimal totalAvance = calcularAvanceHito(hito);
        hito.setAvanceCalculado(totalAvance);
        hitoRepository.save(hito);

        if (hito.getFase() != null) {
            calcularYActualizarAvanceFase(hito.getFase().getId());
        }

        return totalAvance;
    }

    private BigDecimal calcularAvanceProyecto(List<Fase> fases) {
        if (fases == null || fases.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sumaPonderaciones = fases.stream()
                .map(Fase::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPonderaciones.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return fases.stream()
                .map(f -> {
                    BigDecimal pesoNormalizado = f.getPonderacion()
                            .divide(sumaPonderaciones, 10, RoundingMode.HALF_UP);
                    return f.getAvanceCalculado().multiply(pesoNormalizado);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularAvanceFase(Fase fase) {
        List<Hito> hitos = hitoRepository.findByFaseId(fase.getId());

        if (hitos == null || hitos.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sumaPonderaciones = hitos.stream()
                .map(Hito::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPonderaciones.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return hitos.stream()
                .map(h -> {
                    BigDecimal pesoNormalizado = h.getPonderacion()
                            .divide(sumaPonderaciones, 10, RoundingMode.HALF_UP);
                    return h.getAvanceCalculado().multiply(pesoNormalizado);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularAvanceHito(Hito hito) {
        List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId());

        if (entregables == null || entregables.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sumaPonderaciones = entregables.stream()
                .map(Entregable::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPonderaciones.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal sumaConformes = entregables.stream()
                .filter(Entregable::esConforme)
                .map(Entregable::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sumaConformes
                .divide(sumaPonderaciones, 10, RoundingMode.HALF_UP)
                .multiply(CIEN)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
