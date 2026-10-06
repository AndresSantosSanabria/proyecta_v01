package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.application.port.out.persistence.FaseRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.HitoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.notification.ProjectDelayNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvanceCalculatorServiceImpl implements IProgressCalculator {

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final FaseRepositoryPort faseRepositoryPort;
    private final HitoRepositoryPort hitoRepositoryPort;
    private final ProjectProgressMetricsService metricsService;
    private final ProjectDelayNotificationService projectDelayNotificationService;

    public AvanceCalculatorServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                                       FaseRepositoryPort faseRepositoryPort,
                                       HitoRepositoryPort hitoRepositoryPort,
                                       ProjectProgressMetricsService metricsService,
                                       ProjectDelayNotificationService projectDelayNotificationService) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.faseRepositoryPort = faseRepositoryPort;
        this.hitoRepositoryPort = hitoRepositoryPort;
        this.metricsService = metricsService;
        this.projectDelayNotificationService = projectDelayNotificationService;
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceProyecto(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepositoryPort.save(proyecto);
        projectDelayNotificationService.notifyIfDelayed(proyecto, snapshot, null);
        return snapshot.avanceTotal();
    }

    @Override
    @Transactional
    public BigDecimal calcularYActualizarAvanceFase(Integer faseId) {
        Fase fase = faseRepositoryPort.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada: " + faseId));
        Proyecto proyecto = fase.getProyecto();
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepositoryPort.save(proyecto);
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
        Hito hito = hitoRepositoryPort.findById(hitoId)
                .orElseThrow(() -> new RuntimeException("Hito no encontrado: " + hitoId));
        Proyecto proyecto = hito.getFase().getProyecto();
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
        sincronizarPersistencia(proyecto, snapshot);
        proyectoRepositoryPort.save(proyecto);
        projectDelayNotificationService.notifyIfDelayed(proyecto, snapshot, null);

        return snapshot.fases().stream()
                .flatMap(fase -> fase.hitos().stream())
                .filter(hitoAvance -> hitoId.equals(hitoAvance.id()))
                .map(HitoAvanceDTO::avance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private Proyecto cargarProyecto(String proyectoId) {
        return proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + proyectoId));
    }

    private void sincronizarPersistencia(Proyecto proyecto, ProyectoAvanceResponseDTO snapshot) {
        if (proyecto.getFases() == null || snapshot.fases() == null) {
            return;
        }

        Map<Integer, Fase> fasesPorId = indexarFases(proyecto.getFases());

        for (FaseAvanceDTO faseSnapshot : snapshot.fases()) {
            Fase fase = fasesPorId.get(faseSnapshot.id());
            if (fase != null) {
                fase.setAvanceCalculado(faseSnapshot.avance());
                sincronizarHitos(fase, faseSnapshot);
            }
        }

        proyecto.setAvanceTotal(snapshot.avanceTotal());
    }

    private Map<Integer, Fase> indexarFases(List<Fase> fases) {
        Map<Integer, Fase> fasesPorId = new LinkedHashMap<>();
        for (Fase fase : fases) {
            if (fase != null && fase.getId() != null) {
                fasesPorId.put(fase.getId(), fase);
            }
        }
        return fasesPorId;
    }

    private void sincronizarHitos(Fase fase, FaseAvanceDTO faseSnapshot) {
        if (fase.getHitos() == null || faseSnapshot.hitos() == null) {
            return;
        }

        Map<Integer, Hito> hitosPorId = indexarHitos(fase.getHitos());

        for (HitoAvanceDTO hitoSnapshot : faseSnapshot.hitos()) {
            Hito hito = hitosPorId.get(hitoSnapshot.id());
            if (hito != null) {
                hito.setAvanceCalculado(hitoSnapshot.avance());
            }
        }
    }

    private Map<Integer, Hito> indexarHitos(List<Hito> hitos) {
        Map<Integer, Hito> hitosPorId = new LinkedHashMap<>();
        for (Hito hito : hitos) {
            if (hito != null && hito.getId() != null) {
                hitosPorId.put(hito.getId(), hito);
            }
        }
        return hitosPorId;
    }
}
