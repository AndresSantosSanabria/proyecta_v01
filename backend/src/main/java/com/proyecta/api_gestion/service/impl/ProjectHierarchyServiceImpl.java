package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.project.*;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
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
    private static final double EPSILON_PONDERACION = 0.01;

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
        validarFaseConHitos(dto, proyecto.getFechaInicio());
        validarPonderacionAlAgregarFase(proyectoId, dto.ponderacion());
        Fase fase = new Fase();
        fase.setNombre(dto.nombre());
        fase.setDescripcion(dto.descripcion());
        fase.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        fase.setProyecto(proyecto);
        Fase guardada = faseRepository.save(fase);
        for (com.proyecta.api_gestion.dto.proyecto.HitoDTO hitoDto : dto.hitos()) {
            crearHitoConEntregables(guardada, hitoDto);
        }
        avanceCalculatorService.calcularYActualizarAvanceProyecto(proyectoId);
        return guardada;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Fase editarFase(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        asegurarPerteneceAlProyecto(proyectoId, fase.getProyecto().getId());
        validarTexto(dto.nombre(), "El nombre de la fase es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion de la fase debe estar entre 1 y 100.");
        validarPonderacionAlEditarFase(proyectoId, fase, dto.ponderacion());
        fase.setNombre(dto.nombre());
        fase.setDescripcion(dto.descripcion());
        fase.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        Fase actualizada = faseRepository.save(fase);
        avanceCalculatorService.calcularYActualizarAvanceProyecto(proyectoId);
        return actualizada;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarFase(String proyectoId, Integer faseId) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        asegurarPerteneceAlProyecto(proyectoId, fase.getProyecto().getId());
        throw new BadRequestException("No se permite eliminar fases ya creadas. Solo se pueden modificar sus textos y ponderacion.");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Hito agregarHito(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        Fase fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fase no encontrada"));
        asegurarPerteneceAlProyecto(proyectoId, fase.getProyecto().getId());
        validarHitoConEntregables(dto, fase.getProyecto() != null ? fase.getProyecto().getFechaInicio() : null);
        validarPonderacionAlAgregarHito(faseId, dto.ponderacion());
        Hito guardado = crearHitoConEntregables(fase, dto);
        avanceCalculatorService.calcularYActualizarAvanceFase(faseId);
        return guardado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Hito editarHito(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        asegurarHitoPerteneceAFase(hito, faseId);
        asegurarPerteneceAlProyecto(proyectoId, proyectoIdDeHito(hito));
        validarTexto(dto.nombre(), "El nombre del hito es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion del hito debe estar entre 1 y 100.");
        validarPonderacionAlEditarHito(faseId, hito, dto.ponderacion());
        hito.setNombre(dto.nombre());
        hito.setDescripcion(dto.descripcion());
        hito.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        Hito actualizado = hitoRepository.save(hito);
        avanceCalculatorService.calcularYActualizarAvanceFase(hito.getFase().getId());
        return actualizado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarHito(String proyectoId, Integer faseId, Integer hitoId) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        asegurarHitoPerteneceAFase(hito, faseId);
        asegurarPerteneceAlProyecto(proyectoId, proyectoIdDeHito(hito));
        throw new BadRequestException("No se permite eliminar hitos ya creados. Solo se pueden modificar sus textos y ponderacion.");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Entregable agregarEntregable(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hito no encontrado"));
        asegurarHitoPerteneceAFase(hito, faseId);
        asegurarPerteneceAlProyecto(proyectoId, proyectoIdDeHito(hito));
        validarEntregableNuevo(dto, hito.getFase() != null && hito.getFase().getProyecto() != null
                ? hito.getFase().getProyecto().getFechaInicio()
                : null);
        validarPonderacionAlAgregarEntregable(hitoId, dto.ponderacion());
        Entregable entregable = new Entregable();
        entregable.setNombre(dto.nombre());
        entregable.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        entregable.setFechaInicio(dto.fechaInicio());
        entregable.setFechaLimite(dto.fechaLimite());
        entregable.setHito(hito);
        Entregable guardado = entregableRepository.save(entregable);
        avanceCalculatorService.calcularYActualizarAvanceHito(hitoId);
        return guardado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Entregable editarEntregable(String proyectoId, Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));
        asegurarPerteneceAlProyecto(proyectoId, proyectoIdDeEntregable(entregable));
        validarTexto(dto.nombre(), "El nombre del entregable es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion del entregable debe estar entre 1 y 100.");
        if (dto.fechaInicio() != null && !dto.fechaInicio().equals(entregable.getFechaInicio())) {
            throw new BadRequestException("La fecha de inicio de un entregable existente no se puede editar.");
        }
        if (dto.fechaLimite() != null && !dto.fechaLimite().equals(entregable.getFechaLimite())) {
            throw new BadRequestException("La fecha limite de un entregable existente no se puede editar.");
        }
        validarPonderacionAlEditarEntregable(entregable, dto.ponderacion());
        entregable.setNombre(dto.nombre());
        entregable.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        Entregable actualizado = entregableRepository.save(entregable);
        avanceCalculatorService.calcularYActualizarAvanceHito(entregable.getHito().getId());
        return actualizado;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarEntregable(String proyectoId, Integer entregableId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));
        asegurarPerteneceAlProyecto(proyectoId, proyectoIdDeEntregable(entregable));
        throw new BadRequestException("No se permite eliminar entregables ya creados. Solo se pueden modificar sus textos y ponderacion.");
    }

    private void validarFaseConHitos(com.proyecta.api_gestion.dto.proyecto.FaseDTO dto, LocalDate fechaInicioProyecto) {
        validarTexto(dto.nombre(), "El nombre de la fase es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion de la fase debe estar entre 1 y 100.");
        if (dto.hitos() == null || dto.hitos().isEmpty()) {
            throw new BadRequestException("Una fase debe crearse con al menos un hito.");
        }
        for (com.proyecta.api_gestion.dto.proyecto.HitoDTO hitoDto : dto.hitos()) {
            validarHitoConEntregables(hitoDto, fechaInicioProyecto);
        }
        validarSumaHitosExacta(dto.hitos(), "Los hitos de una fase deben sumar exactamente 100%.");
    }

    private void validarHitoConEntregables(com.proyecta.api_gestion.dto.proyecto.HitoDTO dto, LocalDate fechaInicioProyecto) {
        validarTexto(dto.nombre(), "El nombre del hito es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion del hito debe estar entre 1 y 100.");
        if (dto.entregables() == null || dto.entregables().isEmpty()) {
            throw new BadRequestException("Un hito debe crearse con al menos un entregable.");
        }
        for (com.proyecta.api_gestion.dto.proyecto.EntregableDTO entregableDto : dto.entregables()) {
            validarEntregableNuevo(entregableDto, fechaInicioProyecto);
        }
        validarSumaEntregablesExacta(dto.entregables(), "Los entregables de un hito deben sumar exactamente 100%.");
    }

    private void validarEntregableNuevo(com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, LocalDate fechaInicioProyecto) {
        validarTexto(dto.nombre(), "El nombre del entregable es obligatorio.");
        validarPonderacion(dto.ponderacion(), "La ponderacion del entregable debe estar entre 1 y 100.");
        validarFechasNuevo(dto, fechaInicioProyecto);
    }

    private void validarFechasNuevo(com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, LocalDate fechaInicioProyecto) {
        if (dto.fechaInicio() == null) {
            throw new BadRequestException("La fecha de inicio del entregable es obligatoria.");
        }
        if (dto.fechaLimite() == null) {
            throw new BadRequestException("La fecha limite del entregable es obligatoria.");
        }
        if (fechaInicioProyecto != null && dto.fechaInicio().isBefore(fechaInicioProyecto)) {
            throw new BadRequestException("La fecha de inicio del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (" + fechaInicioProyecto + ").");
        }
        if (fechaInicioProyecto != null && dto.fechaLimite().isBefore(fechaInicioProyecto)) {
            throw new BadRequestException("La fecha limite del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (" + fechaInicioProyecto + ").");
        }
        if (dto.fechaLimite().isBefore(dto.fechaInicio())) {
            throw new BadRequestException("La fecha limite del entregable debe ser mayor o igual a la fecha de inicio.");
        }
    }

    private void validarTexto(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void validarPonderacion(Integer value, String message) {
        if (value == null || value < 1 || value > 100) {
            throw new BadRequestException(message);
        }
    }

    private void validarSumaHitosExacta(List<com.proyecta.api_gestion.dto.proyecto.HitoDTO> hitos, String message) {
        int total = hitos.stream().mapToInt(com.proyecta.api_gestion.dto.proyecto.HitoDTO::ponderacion).sum();
        if (Math.abs(total - 100) > EPSILON_PONDERACION) {
            throw new BadRequestException(message);
        }
    }

    private void validarSumaEntregablesExacta(List<com.proyecta.api_gestion.dto.proyecto.EntregableDTO> entregables, String message) {
        int total = entregables.stream().mapToInt(com.proyecta.api_gestion.dto.proyecto.EntregableDTO::ponderacion).sum();
        if (Math.abs(total - 100) > EPSILON_PONDERACION) {
            throw new BadRequestException(message);
        }
    }

    private void validarPonderacionAlAgregarFase(String proyectoId, Integer ponderacion) {
        double total = faseRepository.findByProyectoId(proyectoId).stream()
                .mapToDouble(fase -> toDouble(fase.getPonderacion()))
                .sum() + ponderacion;
        validarNoSupera100(total, "La suma de fases del proyecto no puede superar 100%.");
    }

    private void validarPonderacionAlEditarFase(String proyectoId, Fase fase, Integer nuevaPonderacion) {
        double actual = toDouble(fase.getPonderacion());
        if (nuevaPonderacion <= actual) return;

        double total = faseRepository.findByProyectoId(proyectoId).stream()
                .filter(item -> !item.getId().equals(fase.getId()))
                .mapToDouble(item -> toDouble(item.getPonderacion()))
                .sum() + nuevaPonderacion;
        validarNoSupera100(total, "La suma de fases del proyecto no puede superar 100%.");
    }

    private void validarPonderacionAlAgregarHito(Integer faseId, Integer ponderacion) {
        double total = hitoRepository.findByFaseId(faseId).stream()
                .mapToDouble(hito -> toDouble(hito.getPonderacion()))
                .sum() + ponderacion;
        validarNoSupera100(total, "La suma de hitos de una fase no puede superar 100%.");
    }

    private void validarPonderacionAlEditarHito(Integer faseId, Hito hito, Integer nuevaPonderacion) {
        double actual = toDouble(hito.getPonderacion());
        if (nuevaPonderacion <= actual) return;

        double total = hitoRepository.findByFaseId(faseId).stream()
                .filter(item -> !item.getId().equals(hito.getId()))
                .mapToDouble(item -> toDouble(item.getPonderacion()))
                .sum() + nuevaPonderacion;
        validarNoSupera100(total, "La suma de hitos de una fase no puede superar 100%.");
    }

    private void validarPonderacionAlAgregarEntregable(Integer hitoId, Integer ponderacion) {
        double total = entregableRepository.findByHitoId(hitoId).stream()
                .mapToDouble(entregable -> toDouble(entregable.getPonderacion()))
                .sum() + ponderacion;
        validarNoSupera100(total, "La suma de entregables de un hito no puede superar 100%.");
    }

    private void validarPonderacionAlEditarEntregable(Entregable entregable, Integer nuevaPonderacion) {
        double actual = toDouble(entregable.getPonderacion());
        if (nuevaPonderacion <= actual) return;

        Integer hitoId = entregable.getHito().getId();
        double total = entregableRepository.findByHitoId(hitoId).stream()
                .filter(item -> !item.getId().equals(entregable.getId()))
                .mapToDouble(item -> toDouble(item.getPonderacion()))
                .sum() + nuevaPonderacion;
        validarNoSupera100(total, "La suma de entregables de un hito no puede superar 100%.");
    }

    private void validarNoSupera100(double total, String message) {
        if (total - 100 > EPSILON_PONDERACION) {
            throw new BadRequestException(message);
        }
    }

    private double toDouble(java.math.BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private Hito crearHitoConEntregables(Fase fase, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        Hito hito = new Hito();
        hito.setNombre(dto.nombre());
        hito.setDescripcion(dto.descripcion());
        hito.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        hito.setFase(fase);

        List<Entregable> entregables = new ArrayList<>();
        for (com.proyecta.api_gestion.dto.proyecto.EntregableDTO entregableDto : dto.entregables()) {
            entregables.add(crearEntregableParaHito(hito, entregableDto));
        }
        hito.setEntregables(entregables);

        return hitoRepository.save(hito);
    }

    private Entregable crearEntregableParaHito(Hito hito, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        Entregable entregable = new Entregable();
        entregable.setNombre(dto.nombre());
        entregable.setPonderacion(java.math.BigDecimal.valueOf(dto.ponderacion()));
        entregable.setFechaInicio(dto.fechaInicio());
        entregable.setFechaLimite(dto.fechaLimite());
        entregable.setHito(hito);
        return entregable;
    }

    private void asegurarPerteneceAlProyecto(String proyectoEsperadoId, String proyectoActualId) {
        if (proyectoEsperadoId == null || proyectoActualId == null || !proyectoActualId.equalsIgnoreCase(proyectoEsperadoId)) {
            throw new ForbiddenException("El elemento no pertenece al proyecto indicado.");
        }
    }

    private void asegurarHitoPerteneceAFase(Hito hito, Integer faseId) {
        if (hito.getFase() == null || hito.getFase().getId() == null || !hito.getFase().getId().equals(faseId)) {
            throw new ForbiddenException("El hito no pertenece a la fase indicada.");
        }
    }

    private String proyectoIdDeHito(Hito hito) {
        if (hito.getFase() == null || hito.getFase().getProyecto() == null) {
            throw new ForbiddenException("El hito no pertenece al proyecto indicado.");
        }
        return hito.getFase().getProyecto().getId();
    }

    private String proyectoIdDeEntregable(Entregable entregable) {
        if (entregable.getHito() == null || entregable.getHito().getFase() == null || entregable.getHito().getFase().getProyecto() == null) {
            throw new ForbiddenException("El entregable no pertenece al proyecto indicado.");
        }
        return entregable.getHito().getFase().getProyecto().getId();
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
        List<Hito> hitos = hitoRepository.findByFaseId(fase.getId()).stream()
                .sorted(ProjectHierarchyOrdering.HITOS_BY_SEQUENCE)
                .toList();
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
        List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId()).stream()
                .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_SCHEDULE)
                .toList();
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

        EntregableHierarchyDTO dto = new EntregableHierarchyDTO(
                entregable.getId(),
                null, // numero field removed from model
                entregable.getNombre(),
                entregable.getPonderacion(),
                entregable.getConforme(),
                entregable.getFechaInicio(),
                fechaEntrega,
                estado,
                diasDiferencia
        );
        dto.setObservacionRevision(entregable.getObservacionRevision());
        return dto;
    }

    private String calcularEstado(Entregable entregable, LocalDate hoy, LocalDate fechaEntrega) {
        if (EstadoEntregable.COMPLETADO.equals(entregable.getEstado())) {
            return "Completado";
        }

        if (EstadoEntregable.EN_PROCESO.equals(entregable.getEstado())) {
            return "En revision";
        }

        if (EstadoEntregable.RECHAZADO.equals(entregable.getEstado())) {
            return "Rechazado";
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
                .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_SCHEDULE)
                .map(this::buildEntregableDTO)
                .collect(java.util.stream.Collectors.toList());
    }
}
