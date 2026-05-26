package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ProjectClosureServiceImpl implements ProjectClosureService {

    private final ProyectoRepository proyectoRepository;
    private final ActaCierreRepository actaCierreRepository;
    private final IProgressCalculator progressCalculator;
    private final ProjectClosureValidator closureValidator;
    private final ProjectProgressMetricsService metricsService;
    private final ObjectMapper objectMapper;

    public ProjectClosureServiceImpl(ProyectoRepository proyectoRepository,
                                     ActaCierreRepository actaCierreRepository,
                                     IProgressCalculator progressCalculator,
                                     ProjectClosureValidator closureValidator,
                                     ProjectProgressMetricsService metricsService,
                                     ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.actaCierreRepository = actaCierreRepository;
        this.progressCalculator = progressCalculator;
        this.closureValidator = closureValidator;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error("El proyecto ya se encuentra cerrado o finalizado.");
        }

        closureValidator.validarCierre(projectId);

        BigDecimal avanceFinal = progressCalculator.calcularYActualizarAvanceProyecto(projectId);
        LocalDateTime fechaCierre = LocalDateTime.now();
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, fechaCierre.toLocalDate());
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible persistir el snapshot de avance del acta de cierre.", ex);
        }

        ActaCierre acta = new ActaCierre(
                proyecto,
                request.resumenEjecutivo(),
                fechaCierre,
                avanceFinal
        );
        acta.setProgresoProgramadoFinal(snapshot.progresoProgramado());
        acta.setProgresoEjecutadoFinal(snapshot.progresoEjecutado());
        acta.setDiferenciaFinal(snapshot.diferencia());
        acta.setEficaciaFinal(snapshot.eficacia());
        acta.setEstadoFinal(snapshot.estado());
        acta.setCorteCalculo(snapshot.corte());
        acta.setSnapshotJson(snapshotJson);

        actaCierreRepository.save(acta);

        proyecto.cerrar();
        proyecto.setAvanceTotal(avanceFinal);
        proyectoRepository.save(proyecto);

        return CierreProyectoResponse.success(
                "Proyecto cerrado exitosamente.",
                fechaCierre,
                avanceFinal
        );
    }
}
