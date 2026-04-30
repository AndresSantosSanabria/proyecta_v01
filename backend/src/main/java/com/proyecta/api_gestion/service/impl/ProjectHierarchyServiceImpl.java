package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.project.*;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.interfaces.ProjectHierarchyService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectHierarchyServiceImpl implements ProjectHierarchyService {

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final SystemParameterRepository systemParameterRepository;

    private static final String DIAS_POR_VENCER_PARAM = "dias_por_vencer";
    private static final int DIAS_POR_VENCER_DEFAULT = 7;

    public ProjectHierarchyServiceImpl(ProyectoRepository proyectoRepository,
                                       FaseRepository faseRepository,
                                       HitoRepository hitoRepository,
                                       EntregableRepository entregableRepository,
                                       SystemParameterRepository systemParameterRepository) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.systemParameterRepository = systemParameterRepository;
    }

    @Override
    public ProjectHierarchyDTO getProjectHierarchy(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoIdOrderByNumeroAsc(proyectoId);
        List<FaseHierarchyDTO> fasesDTO = new ArrayList<>();

        for (Fase fase : fases) {
            FaseHierarchyDTO faseDTO = buildFaseDTO(fase);
            fasesDTO.add(faseDTO);
        }

        return new ProjectHierarchyDTO(proyectoId, proyecto.getNombre(), fasesDTO);
    }

    private FaseHierarchyDTO buildFaseDTO(Fase fase) {
        List<Hito> hitos = hitoRepository.findByFaseIdOrderByNumeroAsc(fase.getId());
        List<HitoHierarchyDTO> hitosDTO = new ArrayList<>();

        for (Hito hito : hitos) {
            HitoHierarchyDTO hitoDTO = buildHitoDTO(hito);
            hitosDTO.add(hitoDTO);
        }

        return new FaseHierarchyDTO(
                fase.getId(),
                fase.getNumero(),
                fase.getDescripcion(),
                fase.getPonderacion(),
                fase.getAvanceCalculado(),
                hitosDTO
        );
    }

    private HitoHierarchyDTO buildHitoDTO(Hito hito) {
        List<Entregable> entregables = entregableRepository.findByHitoIdOrderByNumeroAsc(hito.getId());
        List<EntregableHierarchyDTO> entregablesDTO = new ArrayList<>();

        for (Entregable entregable : entregables) {
            EntregableHierarchyDTO entregableDTO = buildEntregableDTO(entregable);
            entregablesDTO.add(entregableDTO);
        }

        return new HitoHierarchyDTO(
                hito.getId(),
                hito.getNumero(),
                hito.getDescripcion(),
                hito.getPonderacion(),
                hito.getAvanceCalculado(),
                entregablesDTO
        );
    }

    private EntregableHierarchyDTO buildEntregableDTO(Entregable entregable) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaEntrega = entregable.getFechaEntrega();

        String estado = calcularEstado(entregable, hoy, fechaEntrega);
        Integer diasDiferencia = calcularDiasDiferencia(hoy, fechaEntrega);

        return new EntregableHierarchyDTO(
                entregable.getId(),
                entregable.getNumero(),
                entregable.getNombre(),
                entregable.getPonderacion(),
                entregable.getConforme(),
                fechaEntrega,
                estado,
                diasDiferencia
        );
    }

    private String calcularEstado(Entregable entregable, LocalDate hoy, LocalDate fechaEntrega) {
        if (entregable.getConforme()) {
            return "Conforme";
        }

        if (fechaEntrega == null) {
            return "Pendiente";
        }

        if (hoy.isAfter(fechaEntrega)) {
            return "Atrasado";
        }

        int diasParaVencer = obtenerDiasPorVencer();
        long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaEntrega);

        if (diasRestantes <= diasParaVencer) {
            return "Por vencer";
        }

        return "Pendiente";
    }

    private Integer calcularDiasDiferencia(LocalDate hoy, LocalDate fechaEntrega) {
        if (fechaEntrega == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(hoy, fechaEntrega);
    }

    private int obtenerDiasPorVencer() {
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByKey(DIAS_POR_VENCER_PARAM);
        if (paramOpt.isPresent()) {
            try {
                return Integer.parseInt(paramOpt.get().getValue());
            } catch (NumberFormatException e) {
                return DIAS_POR_VENCER_DEFAULT;
            }
        }
        return DIAS_POR_VENCER_DEFAULT;
    }
}
