package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.notification.ProjectDelayNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AvanceCalculatorServiceImpl implements IProgressCalculator {

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final ProjectProgressMetricsService metricsService;
    private final ProjectDelayNotificationService projectDelayNotificationService;

    public AvanceCalculatorServiceImpl(ProyectoRepository proyectoRepository,
                                       FaseRepository faseRepository,
                                       HitoRepository hitoRepository,
                                       EntregableRepository entregableRepository,
                                       ProjectProgressMetricsService metricsService,
                                       ProjectDelayNotificationService projectDelayNotificationService) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.metricsService = metricsService;
        this.projectDelayNotificationService = projectDelayNotificationService;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceProyecto(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now());
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepository.save(proyecto);
        projectDelayNotificationService.notifyIfDelayed(proyecto, snapshot, null);
        return snapshot.avanceTotal();
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceFase(Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada: " + faseId));
        Proyecto proyecto = fase.getProyecto();
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now());
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepository.save(proyecto);
        projectDelayNotificationService.notifyIfDelayed(proyecto, snapshot, null);

        return snapshot.fases().stream()
                .filter(faseAvance -> faseId.equals(faseAvance.id()))
                .map(FaseAvanceDTO::avance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceHito(Integer hitoId) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new RuntimeException("Hito no encontrado: " + hitoId));
        Proyecto proyecto = hito.getFase().getProyecto();
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now());
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepository.save(proyecto);
        projectDelayNotificationService.notifyIfDelayed(proyecto, snapshot, null);

        return snapshot.fases().stream()
                .flatMap(fase -> fase.hitos().stream())
                .filter(hitoAvance -> hitoId.equals(hitoAvance.id()))
                .map(HitoAvanceDTO::avance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private Proyecto cargarProyecto(String proyectoId) {
        return proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + proyectoId));
    }

    private void sincronizarPersistencia(Proyecto proyecto, ProyectoAvanceResponseDTO snapshot) {
        if (proyecto.getFases() == null || snapshot.fases() == null) {
            return;
        }

        Map<Integer, Fase> fasesPorId = new LinkedHashMap<>();
        for (Fase fase : proyecto.getFases()) {
            if (fase != null && fase.getId() != null) {
                fasesPorId.put(fase.getId(), fase);
            }
        }

        for (FaseAvanceDTO faseSnapshot : snapshot.fases()) {
            Fase fase = fasesPorId.get(faseSnapshot.id());
            if (fase == null) {
                continue;
            }
            fase.setAvanceCalculado(faseSnapshot.avance());

            if (fase.getHitos() == null || faseSnapshot.hitos() == null) {
                continue;
            }

            Map<Integer, Hito> hitosPorId = new LinkedHashMap<>();
            for (Hito hito : fase.getHitos()) {
                if (hito != null && hito.getId() != null) {
                    hitosPorId.put(hito.getId(), hito);
                }
            }

            for (HitoAvanceDTO hitoSnapshot : faseSnapshot.hitos()) {
                Hito hito = hitosPorId.get(hitoSnapshot.id());
                if (hito != null) {
                    hito.setAvanceCalculado(hitoSnapshot.avance());
                }
            }
        }

        proyecto.setAvanceTotal(snapshot.avanceTotal());
    }
}
