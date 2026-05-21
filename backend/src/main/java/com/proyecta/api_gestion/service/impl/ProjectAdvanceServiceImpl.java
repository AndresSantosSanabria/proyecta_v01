package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.*;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectAdvanceServiceImpl implements ProyectoAvanceService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final IProgressCalculator progressCalculator;
    private final IStorageProvider storageProvider;
    private final EntityManager entityManager;

    public ProjectAdvanceServiceImpl(ProyectoRepository proyectoRepository,
                                     EntregableRepository entregableRepository,
                                     FaseRepository faseRepository,
                                     HitoRepository hitoRepository,
                                     IProgressCalculator progressCalculator,
                                     IStorageProvider storageProvider,
                                     EntityManager entityManager) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.progressCalculator = progressCalculator;
        this.storageProvider = storageProvider;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ProyectoAvanceResponseDTO obtenerAvanceDetallado(String proyectoId) {
        progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

        entityManager.flush();
        entityManager.clear();

        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        LocalDate hoy = LocalDate.now();

        BigDecimal CIEN = new BigDecimal("100");

        List<FaseAvanceDTO> fasesDto = proyecto.getFases().stream()
                .sorted(Comparator.comparing(Fase::getNombre))
                .map(fase -> {
                    List<HitoAvanceDTO> hitosDto = fase.getHitos().stream()
                            .sorted(Comparator.comparing(Hito::getNombre))
                            .map(hito -> {
                                List<EntregableAvanceDTO> entregablesDto = hito.getEntregables().stream()
                                        .sorted(Comparator.comparing(Entregable::getNombre))
                                        .map(e -> mapToEntregableDTO(e, hoy))
                                        .collect(Collectors.toList());

                                BigDecimal sumaPonderacionEntregables = entregablesDto.stream()
                                        .map(EntregableAvanceDTO::ponderacion)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                BigDecimal avanceHito = BigDecimal.ZERO;
                                if (sumaPonderacionEntregables.compareTo(BigDecimal.ZERO) > 0) {
                                    avanceHito = entregablesDto.stream()
                                            .map(eDto -> eDto.avance().multiply(eDto.ponderacion())
                                                    .divide(sumaPonderacionEntregables, 10, RoundingMode.HALF_UP))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .setScale(2, RoundingMode.HALF_UP);
                                }

                                return new HitoAvanceDTO(
                                        hito.getId(),
                                        hito.getNombre(),
                                        hito.getPonderacion(),
                                        avanceHito,
                                        entregablesDto
                                );
                            })
                            .collect(Collectors.toList());

                    BigDecimal sumaPonderacionHitos = hitosDto.stream()
                            .map(HitoAvanceDTO::ponderacion)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal avanceFase = BigDecimal.ZERO;
                    if (sumaPonderacionHitos.compareTo(BigDecimal.ZERO) > 0) {
                        avanceFase = hitosDto.stream()
                                .map(hDto -> hDto.avance().multiply(hDto.ponderacion())
                                        .divide(sumaPonderacionHitos, 10, RoundingMode.HALF_UP))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    return new FaseAvanceDTO(
                            fase.getId(),
                            fase.getNombre(),
                            fase.getPonderacion(),
                            avanceFase,
                            hitosDto
                    );
                })
                .collect(Collectors.toList());

        BigDecimal sumaPonderacionFases = fasesDto.stream()
                .map(FaseAvanceDTO::ponderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avanceTotalProyecto = BigDecimal.ZERO;
        if (sumaPonderacionFases.compareTo(BigDecimal.ZERO) > 0) {
            avanceTotalProyecto = fasesDto.stream()
                    .map(fDto -> fDto.avance().multiply(fDto.ponderacion())
                            .divide(sumaPonderacionFases, 10, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        long conformes = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(Entregable::esConforme)
                .count();

        long total = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .count();

        long atrasados = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(e -> !e.esConforme()
                        && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy))
                .count();

        return new ProyectoAvanceResponseDTO(
                proyecto.getId(),
                proyecto.getId(),
                proyecto.getNombre(),
                avanceTotalProyecto,
                conformes,
                total,
                atrasados,
                0L,
                hoy,
                fasesDto
        );
    }

    private EntregableAvanceDTO mapToEntregableDTO(Entregable e, LocalDate hoy) {
        Long atraso = null;
        boolean completado = e.esConforme();

        if (!completado && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy)) {
            atraso = ChronoUnit.DAYS.between(e.getFechaLimite(), hoy);
        }

        BigDecimal avance = completado ? new BigDecimal("100") : BigDecimal.ZERO;

        return new EntregableAvanceDTO(
                e.getId(),
                e.getNombre(),
                e.getPonderacion(),
                e.getFechaLimite(),
                avance,
                e.getEstadoCodigo(),
                atraso,
                e.getFechaEntregaReal(),
                e.getArchivoPdf(),
                e.getArchivoPdf() != null ? "/api/v1/proyectos/" + e.getHito().getFase().getProyecto().getId() + "/avance/entregables/" + e.getId() + "/evidencia" : null
        );
    }

    @Override
    public ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        long conformes = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(Entregable::esConforme)
                .count();

        long total = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .count();

        return new ProyectoSummaryDTO(
                proyecto.getAvanceTotal(),
                conformes + "/" + total,
                0L,
                0L
        );
    }

    @Override
    @Transactional
    public EntregableConformidadResponseDTO actualizarConformidad(String proyectoId, Integer entregableId, Boolean conformidad, LocalDate fechaEntrega, MultipartFile evidencia) {
        if (evidencia == null || evidencia.isEmpty()) {
            throw new UnprocessableEntityException("El archivo de evidencia es requerido");
        }

        if (!"application/pdf".equals(evidencia.getContentType())) {
            throw new UnprocessableEntityException("El archivo debe ser un PDF");
        }

        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        entregable.asegurarModificable();

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        String fileName = "evidencia_" + proyectoId + "_" + entregableId + "_" + System.currentTimeMillis();
        String storedName = storageProvider.storeFile(evidencia, "evidencias", fileName);

        entregable.completar(storedName, fechaEntrega, Boolean.TRUE.equals(conformidad));
        entregableRepository.save(entregable);
        entityManager.flush();

        Integer hitoId = entregable.getHito().getId();
        progressCalculator.calcularYActualizarAvanceHito(hitoId);
        progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

        entityManager.flush();
        entityManager.refresh(entregable);

        Proyecto proyectoActualizado = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        LocalDate hoy = LocalDate.now();
        List<FaseAvanceDTO> fasesDto = construirFasesDTO(proyectoActualizado, hoy);
        BigDecimal avanceTotalProyecto = fasesDto.stream()
                .map(fDto -> fDto.avance().multiply(fDto.ponderacion())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        long conformes = contarEntregablesConformes(proyectoActualizado);
        long total = contarEntregablesTotales(proyectoActualizado);
        long atrasados = contarEntregablesAtrasados(proyectoActualizado, hoy);

        String evidenciaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregable.getId() + "/evidencia";
        ProyectoAvanceResponseDTO avanceMap = new ProyectoAvanceResponseDTO(
                proyectoId,
                proyectoId,
                proyectoActualizado.getNombre(),
                avanceTotalProyecto,
                conformes,
                total,
                atrasados,
                0L,
                hoy,
                fasesDto
        );

        return new EntregableConformidadResponseDTO(
                entregable.getId(),
                entregable.getEstadoCodigo(),
                entregable.getFechaEntregaReal(),
                evidenciaUrl,
                avanceMap
        );
    }

    private List<FaseAvanceDTO> construirFasesDTO(Proyecto proyecto, LocalDate hoy) {
        return proyecto.getFases().stream()
                .sorted(Comparator.comparing(Fase::getNombre))
                .map(fase -> {
                    List<HitoAvanceDTO> hitosDto = fase.getHitos().stream()
                            .sorted(Comparator.comparing(Hito::getNombre))
                            .map(hito -> {
                                List<EntregableAvanceDTO> entregablesDto = hito.getEntregables().stream()
                                        .sorted(Comparator.comparing(Entregable::getNombre))
                                        .map(e -> mapToEntregableDTO(e, hoy))
                                        .collect(Collectors.toList());

                                BigDecimal avanceHito = entregablesDto.stream()
                                        .map(eDto -> eDto.avance().multiply(eDto.ponderacion())
                                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

                                return new HitoAvanceDTO(
                                        hito.getId(),
                                        hito.getNombre(),
                                        hito.getPonderacion(),
                                        avanceHito,
                                        entregablesDto
                                );
                            })
                            .collect(Collectors.toList());

                    BigDecimal avanceFase = hitosDto.stream()
                            .map(hDto -> hDto.avance().multiply(hDto.ponderacion())
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

                    return new FaseAvanceDTO(
                            fase.getId(),
                            fase.getNombre(),
                            fase.getPonderacion(),
                            avanceFase,
                            hitosDto
                    );
                })
                .collect(Collectors.toList());
    }

    private long contarEntregablesConformes(Proyecto proyecto) {
        return proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(Entregable::esConforme)
                .count();
    }

    private long contarEntregablesTotales(Proyecto proyecto) {
        return proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .count();
    }

    private long contarEntregablesAtrasados(Proyecto proyecto, LocalDate hoy) {
        return proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(e -> !e.esConforme()
                        && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy))
                .count();
    }
}
