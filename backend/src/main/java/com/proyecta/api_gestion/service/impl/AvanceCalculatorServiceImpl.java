package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.AvanceCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implementación desacoplada de la lógica de cálculo.
 */
@Service
public class AvanceCalculatorServiceImpl implements AvanceCalculatorService {

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
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        List<Fase> fases = faseRepository.findByProyectoIdOrderByNumeroAsc(proyectoId);

        // El avance del proyecto es la suma de (avance_fase * ponderacion_fase / 100)
        BigDecimal totalAvance = fases.stream().map(f -> f.getAvanceCalculado()
        		.multiply(f.getPonderacion())
        		.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
        		.reduce(BigDecimal.ZERO, BigDecimal::add);

        proyecto.setAvanceCalculado(totalAvance);
        proyectoRepository.save(proyecto);

        return totalAvance;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceFase(Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada"));

        List<Hito> hitos = hitoRepository.findByFaseIdOrderByNumeroAsc(faseId);

        // El avance de la fase es la suma de (avance_hito * ponderacion_hito / 100)
        BigDecimal totalAvance = hitos.stream()
                .map(h -> h.getAvanceCalculado()
                        .multiply(h.getPonderacion())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        fase.setAvanceCalculado(totalAvance);
        faseRepository.save(fase);

        // Al cambiar el avance de la fase, también se debe recalcular el del proyecto
        if (fase.getProyecto() != null) {
            calcularYActualizarAvanceProyecto(fase.getProyecto().getId());
        }

        return totalAvance;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceHito(Integer hitoId) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new RuntimeException("Hito no encontrado"));

        List<Entregable> entregables = entregableRepository.findByHitoIdOrderByNumeroAsc(hitoId);

        // El avance del hito es la suma de las ponderaciones de los entregables conformes
        BigDecimal totalAvance = entregables.stream()
                .filter(Entregable::getConforme)
                .map(Entregable::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        hito.setAvanceCalculado(totalAvance);
        hitoRepository.save(hito);

        // Al cambiar el avance del hito, recalcular la fase
        if (hito.getFase() != null) {
            calcularYActualizarAvanceFase(hito.getFase().getId());
        }

        return totalAvance;
    }
}
