package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.*;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.AvanceCalculatorService;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectAdvanceServiceImpl implements ProyectoAvanceService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final AvanceCalculatorService avanceCalculatorService;
    private final FileStorageServiceImpl fileStorageService;

    public ProjectAdvanceServiceImpl(ProyectoRepository proyectoRepository,
                                     EntregableRepository entregableRepository,
                                     AvanceCalculatorService avanceCalculatorService,
                                     FileStorageServiceImpl fileStorageService) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.avanceCalculatorService = avanceCalculatorService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoAvanceResponseDTO obtenerAvanceDetallado(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        LocalDate hoy = LocalDate.now();

        List<FaseAvanceDTO> fasesDto = proyecto.getFases().stream()
                .sorted(Comparator.comparing(Fase::getNombre))
                .map(fase -> new FaseAvanceDTO(
                        fase.getId(),
                        fase.getNombre(),
                        fase.getPonderacion(),
                        fase.getAvanceCalculado(),
                        fase.getHitos().stream()
                                .sorted(Comparator.comparing(Hito::getNombre))
                                .map(hito -> new HitoAvanceDTO(
                                        hito.getId(),
                                        hito.getNombre(),
                                        hito.getPonderacion(),
                                        hito.getAvanceCalculado(),
                                        hito.getEntregables().stream()
                                                .sorted(Comparator.comparing(Entregable::getNombre))
                                                .map(e -> mapToEntregableDTO(e, hoy))
                                                .collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        // Calcular métricas de resumen
        long conformes = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(Entregable::getConforme)
                .count();

        long total = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .count();

        long atrasados = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(e -> !e.getConforme() && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy))
                .count();

        return new ProyectoAvanceResponseDTO(
                proyecto.getId(),
                proyecto.getId(), // Usando ID como código si no hay campo separado
                proyecto.getNombre(),
                proyecto.getAvanceTotal(),
                conformes,
                total,
                atrasados,
                0L, // Proximos a vencer (pendiente lógica específica)
                hoy,
                fasesDto
        );
    }

    private EntregableAvanceDTO mapToEntregableDTO(Entregable e, LocalDate hoy) {
        Long atraso = null;
        if (!e.getConforme() && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy)) {
            atraso = ChronoUnit.DAYS.between(e.getFechaLimite(), hoy);
        }

        BigDecimal avance = e.getConforme() ? new BigDecimal("100") : BigDecimal.ZERO;

        return new EntregableAvanceDTO(
                e.getId(),
                e.getNombre(),
                e.getPonderacion(),
                e.getFechaLimite(),
                avance,
                e.getEstado(),
                atraso,
                e.getFechaEntregaReal(),
                e.getArchivoPdf(),
                e.getArchivoPdf() != null ? "/api/v1/archivos/entregables/" + e.getId() + "/evidencia" : null
        );
    }

    @Override
    public ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        
        long conformes = proyecto.getFases().stream()
                .flatMap(f -> f.getHitos().stream())
                .flatMap(h -> h.getEntregables().stream())
                .filter(Entregable::getConforme)
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

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        // Guardar archivo
        String fileName = "evidencia_" + proyectoId + "_" + entregableId + "_" + System.currentTimeMillis();
        String storedName = fileStorageService.storeFile(evidencia, fileName);

        // Actualizar entregable
        entregable.setConforme(conformidad);
        entregable.setFechaEntregaReal(fechaEntrega);
        entregable.setArchivoPdf(storedName);
        entregable.setEstado(EstadoEntregable.A_CONFORMIDAD);
        
        entregableRepository.save(entregable);

        // Recalcular avances
        avanceCalculatorService.calcularYActualizarAvanceHito(entregable.getHito().getId());

        // Obtener avance actualizado
        Proyecto proyecto = proyectoRepository.findById(proyectoId).get();

        return new EntregableConformidadResponseDTO(
                entregable.getId(),
                entregable.getEstado(),
                entregable.getFechaEntregaReal(),
                "/api/v1/archivos/entregables/" + entregable.getId() + "/evidencia",
                proyecto.getAvanceTotal()
        );
    }
}
