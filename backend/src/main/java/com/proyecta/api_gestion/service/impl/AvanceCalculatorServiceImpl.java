package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.AvanceCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);

        BigDecimal totalAvance = fases.stream()
                .map(f -> f.getAvanceCalculado()
                        .multiply(f.getPonderacion())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        proyecto.setAvanceTotal(totalAvance);
        proyectoRepository.save(proyecto);

        return totalAvance;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceFase(Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada: " + faseId));

        List<Hito> hitos = hitoRepository.findByFaseId(faseId);

        BigDecimal totalAvance = hitos.stream()
                .map(h -> h.getAvanceCalculado()
                        .multiply(h.getPonderacion())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        List<Entregable> entregables = entregableRepository.findByHitoId(hitoId);

        BigDecimal totalAvance = entregables.stream()
                .filter(Entregable::getConforme)
                .map(Entregable::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        hito.setAvanceCalculado(totalAvance);
        hitoRepository.save(hito);

        if (hito.getFase() != null) {
            calcularYActualizarAvanceFase(hito.getFase().getId());
        }

        return totalAvance;
    }
}
