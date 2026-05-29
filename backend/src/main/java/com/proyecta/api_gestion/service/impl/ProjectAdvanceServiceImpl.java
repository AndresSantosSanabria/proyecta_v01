package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.model.ActaCierre;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ActaCierreRepository;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

@Service
public class ProjectAdvanceServiceImpl implements ProyectoAvanceService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final IProgressCalculator progressCalculator;
    private final IStorageProvider storageProvider;
    private final EntityManager entityManager;
    private final ActaCierreRepository actaCierreRepository;
    private final ProjectProgressMetricsService metricsService;

    public ProjectAdvanceServiceImpl(ProyectoRepository proyectoRepository,
                                     EntregableRepository entregableRepository,
                                     IProgressCalculator progressCalculator,
                                     IStorageProvider storageProvider,
                                     EntityManager entityManager,
                                     ActaCierreRepository actaCierreRepository,
                                     ProjectProgressMetricsService metricsService) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.progressCalculator = progressCalculator;
        this.storageProvider = storageProvider;
        this.entityManager = entityManager;
        this.actaCierreRepository = actaCierreRepository;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoAvanceResponseDTO obtenerAvanceDetallado(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);

        if (proyecto.esEstadoTerminal()) {
            ActaCierre acta = actaCierreRepository.findByProyectoId(proyectoId)
                    .orElseThrow(() -> new IllegalStateException(
                            "El proyecto " + proyectoId + " está cerrado pero no tiene un snapshot de cierre persistido."));

            String snapshotJson = acta.getSnapshotJson();
            if (snapshotJson == null || snapshotJson.isBlank()) {
                throw new IllegalStateException(
                        "El proyecto " + proyectoId + " está cerrado pero su snapshot de cierre está vacío.");
            }

            return metricsService.deserializar(snapshotJson);
        }

        return metricsService.construir(proyecto, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId) {
        ProyectoAvanceResponseDTO avance = obtenerAvanceDetallado(proyectoId);

        return new ProyectoSummaryDTO(
                avance.progresoEjecutado(),
                avance.entregablesEntregadosAlCorte() + "/" + avance.entregablesProgramadosAlCorte(),
                avance.entregablesAtrasados(),
                avance.proximosAVencer()
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

        validarPdfReal(evidencia);

        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        entregable.asegurarModificable();

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        if (entregable.getHito().getFase().getProyecto().esEstadoTerminal()) {
            throw new ForbiddenException("No se pueden modificar entregables de un proyecto cerrado.");
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

        Proyecto proyectoActualizado = cargarProyecto(proyectoId);
        ProyectoAvanceResponseDTO avance = metricsService.construir(proyectoActualizado, LocalDate.now());
        String evidenciaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregable.getId() + "/evidencia";

        return new EntregableConformidadResponseDTO(
                entregable.getId(),
                entregable.getEstadoCodigo(),
                entregable.getFechaEntregaReal(),
                evidenciaUrl,
                avance
        );
    }

    private void validarPdfReal(MultipartFile evidencia) {
        try {
            byte[] encabezado = evidencia.getInputStream().readNBytes(5);
            String firma = new String(encabezado, StandardCharsets.US_ASCII);
            if (!firma.startsWith("%PDF-")) {
                throw new UnprocessableEntityException("El archivo cargado no es un PDF válido.");
            }
        } catch (IOException ex) {
            throw new UnprocessableEntityException("No fue posible validar el archivo PDF cargado.");
        }
    }

    private Proyecto cargarProyecto(String proyectoId) {
        return proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
    }
}
