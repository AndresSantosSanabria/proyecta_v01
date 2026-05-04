package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectClosureServiceImpl implements ProjectClosureService {

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final ActaCierreRepository actaCierreRepository;

    public ProjectClosureServiceImpl(ProyectoRepository proyectoRepository,
                                     FaseRepository faseRepository,
                                     HitoRepository hitoRepository,
                                     EntregableRepository entregableRepository,
                                     ActaCierreRepository actaCierreRepository) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.actaCierreRepository = actaCierreRepository;
    }

    @Override
    @Transactional
    public CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (Boolean.TRUE.equals(proyecto.getCerrado())) {
            return CierreProyectoResponse.error("El proyecto ya se encuentra cerrado.");
        }

        // 1. Validación de Reglas: Hitos al 100% y APROBADOS
        validateMilestones(projectId);

        // 2. Cálculo de Avance: Promedio ponderado de entregables finalizados
        BigDecimal avanceFinal = calcularAvanceFinal(projectId);

        // 3. Persistencia
        LocalDateTime fechaCierre = LocalDateTime.now();
        
        ActaCierre acta = new ActaCierre(
                proyecto,
                request.resumenEjecutivo(),
                fechaCierre,
                avanceFinal
        );
        
        actaCierreRepository.save(acta);

        // Actualizar estado del proyecto
        proyecto.setCerrado(true);
        proyecto.setAvanceCalculado(avanceFinal);
        proyectoRepository.save(proyecto);

        return CierreProyectoResponse.success(
                "Proyecto cerrado exitosamente.",
                fechaCierre,
                avanceFinal
        );
    }

    private void validateMilestones(String projectId) {
        List<Fase> fases = faseRepository.findByProyectoIdOrderByNumeroAsc(projectId);
        
        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepository.findByFaseIdOrderByNumeroAsc(fase.getId());
            for (Hito hito : hitos) {
                // Validación de cumplimiento == 100
                if (hito.getAvanceCalculado() == null || hito.getAvanceCalculado().compareTo(new BigDecimal("100")) < 0) {
                    throw new BadRequestException("Validación fallida: El hito '" + hito.getDescripcion() + 
                        "' no tiene un cumplimiento del 100% (Actual: " + hito.getAvanceCalculado() + "%).");
                }
                
                // Validación de estado de revisión == 'APROBADO'
                if (!"APROBADO".equalsIgnoreCase(hito.getEstadoRevision())) {
                    throw new BadRequestException("Validación fallida: El hito '" + hito.getDescripcion() + 
                        "' no ha sido APROBADO por el Gestor de Proyectos.");
                }
            }
        }
    }

    /**
     * Calcula el porcentaje de avance total basado en el promedio ponderado de los entregables finalizados.
     * Formula: Sum(Deliverable.Ponderacion where conforme is true) / Sum(All Deliverable.Ponderacion) * 100
     */
    private BigDecimal calcularAvanceFinal(String projectId) {
        List<Fase> fases = faseRepository.findByProyectoIdOrderByNumeroAsc(projectId);
        
        BigDecimal totalPonderacionEntregables = BigDecimal.ZERO;
        BigDecimal ponderacionCumplida = BigDecimal.ZERO;

        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepository.findByFaseIdOrderByNumeroAsc(fase.getId());
            for (Hito hito : hitos) {
                List<Entregable> entregables = entregableRepository.findByHitoIdOrderByNumeroAsc(hito.getId());
                for (Entregable entregable : entregables) {
                    totalPonderacionEntregables = totalPonderacionEntregables.add(entregable.getPonderacion());
                    if (Boolean.TRUE.equals(entregable.getConforme())) {
                        ponderacionCumplida = ponderacionCumplida.add(entregable.getPonderacion());
                    }
                }
            }
        }

        if (totalPonderacionEntregables.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return ponderacionCumplida
                .multiply(new BigDecimal("100"))
                .divide(totalPonderacionEntregables, 2, RoundingMode.HALF_UP);
    }
}
