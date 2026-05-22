package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.project.*;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
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
    private final IProgressCalculator avanceCalculatorService;

    private static final String DIAS_POR_VENCER_PARAM = "dias_por_vencer";
    private static final int DIAS_POR_VENCER_DEFAULT = 7;

    public ProjectHierarchyServiceImpl(ProyectoRepository proyectoRepository,
                                       FaseRepository faseRepository,
                                       HitoRepository hitoRepository,
                                       EntregableRepository entregableRepository,
                                       SystemParameterRepository systemParameterRepository,
                                       IProgressCalculator avanceCalculatorService) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.systemParameterRepository = systemParameterRepository;
        this.avanceCalculatorService = avanceCalculatorService;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Fase agregarFase(String proyectoId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));
        Fase fase = new Fase();
        fase.setNombre(dto.nombre());
        fase.setDescripcion(dto.descripcion());
        fase.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        fase.setProyecto(proyecto);
        Fase guardada = faseRepository.save(fase);
        avanceCalculatorService.calcularYActualizarAvanceProyecto(proyectoId);
        return guardada;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Fase editarFase(Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        fase.setNombre(dto.nombre());
        fase.setDescripcion(dto.descripcion());
        fase.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        Fase actualizada = faseRepository.save(fase);
        avanceCalculatorService.calcularYActualizarAvanceProyecto(fase.getProyecto().getId());
        return actualizada;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarFase(Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        String proyectoId = fase.getProyecto().getId();
        faseRepository.delete(fase);
        avanceCalculatorService.calcularYActualizarAvanceProyecto(proyectoId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Hito agregarHito(Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        Hito hito = new Hito();
        hito.setNombre(dto.nombre());
        hito.setDescripcion(dto.descripcion());
        hito.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        hito.setFase(fase);
        Hito guardado = hitoRepository.save(hito);
        avanceCalculatorService.calcularYActualizarAvanceFase(faseId);
        return guardado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Hito editarHito(Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        hito.setNombre(dto.nombre());
        hito.setDescripcion(dto.descripcion());
        hito.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        Hito actualizado = hitoRepository.save(hito);
        avanceCalculatorService.calcularYActualizarAvanceFase(hito.getFase().getId());
        return actualizado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarHito(Integer hitoId) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        Integer faseId = hito.getFase().getId();
        hitoRepository.delete(hito);
        avanceCalculatorService.calcularYActualizarAvanceFase(faseId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Entregable agregarEntregable(Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        Entregable entregable = new Entregable();
        entregable.setNombre(dto.nombre());
        entregable.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        entregable.setFechaLimite(dto.fechaLimite());
        entregable.setHito(hito);
        Entregable guardado = entregableRepository.save(entregable);
        avanceCalculatorService.calcularYActualizarAvanceHito(hitoId);
        return guardado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Entregable editarEntregable(Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));
        entregable.setNombre(dto.nombre());
        entregable.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        entregable.setFechaLimite(dto.fechaLimite());
        Entregable actualizado = entregableRepository.save(entregable);
        avanceCalculatorService.calcularYActualizarAvanceHito(entregable.getHito().getId());
        return actualizado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarEntregable(Integer entregableId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));
        Integer hitoId = entregable.getHito().getId();
        entregableRepository.delete(entregable);
        avanceCalculatorService.calcularYActualizarAvanceHito(hitoId);
    }

    @Override
    public ProjectHierarchyDTO getProjectHierarchy(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);
        List<FaseHierarchyDTO> fasesDTO = new ArrayList<>();

        for (Fase fase : fases) {
            FaseHierarchyDTO faseDTO = buildFaseDTO(fase);
            fasesDTO.add(faseDTO);
        }

        return new ProjectHierarchyDTO(proyectoId, proyecto.getNombre(), fasesDTO);
    }

    private FaseHierarchyDTO buildFaseDTO(Fase fase) {
        List<Hito> hitos = hitoRepository.findByFaseId(fase.getId());
        List<HitoHierarchyDTO> hitosDTO = new ArrayList<>();

        for (Hito hito : hitos) {
            HitoHierarchyDTO hitoDTO = buildHitoDTO(hito);
            hitosDTO.add(hitoDTO);
        }

        return new FaseHierarchyDTO(
                fase.getId(),
                null, // numero field removed from model
                fase.getNombre(),
                fase.getPonderacion(),
                fase.getAvanceCalculado(),
                hitosDTO
        );
    }

    private HitoHierarchyDTO buildHitoDTO(Hito hito) {
        List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId());
        List<EntregableHierarchyDTO> entregablesDTO = new ArrayList<>();

        for (Entregable entregable : entregables) {
            EntregableHierarchyDTO entregableDTO = buildEntregableDTO(entregable);
            entregablesDTO.add(entregableDTO);
        }

        return new HitoHierarchyDTO(
                hito.getId(),
                null, // numero field removed from model
                hito.getNombre(),
                hito.getPonderacion(),
                hito.getAvanceCalculado(),
                entregablesDTO
        );
    }

    private EntregableHierarchyDTO buildEntregableDTO(Entregable entregable) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaEntrega = entregable.getFechaLimite();

        String estado = calcularEstado(entregable, hoy, fechaEntrega);
        Integer diasDiferencia = calcularDiasDiferencia(hoy, fechaEntrega);

        return new EntregableHierarchyDTO(
                entregable.getId(),
                null, // numero field removed from model
                entregable.getNombre(),
                entregable.getPonderacion(),
                entregable.getConforme(),
                fechaEntrega,
                estado,
                diasDiferencia
        );
    }

    private String calcularEstado(Entregable entregable, LocalDate hoy, LocalDate fechaEntrega) {
        if (EstadoEntregable.COMPLETADO.equals(entregable.getEstado())) {
            return "Completado";
        }

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

    @Override
    public List<EntregableHierarchyDTO> listarEntregablesProyecto(String proyectoId) {
        List<Entregable> entregables = entregableRepository.findByProyectoId(proyectoId);
        return entregables.stream()
                .map(this::buildEntregableDTO)
                .collect(java.util.stream.Collectors.toList());
    }
}
