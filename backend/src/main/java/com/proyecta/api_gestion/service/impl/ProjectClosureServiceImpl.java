package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.ActaCierre;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Patrocinador;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.ActaCierreRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.report.ActaCierrePdfGenerator;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectClosureServiceImpl implements ProjectClosureService {

    private static final String STORAGE_SUBDIR = "actas_cierre";
    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ProyectoRepository proyectoRepository;
    private final ActaCierreRepository actaCierreRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final IProgressCalculator progressCalculator;
    private final ProjectClosureValidator closureValidator;
    private final ProjectProgressMetricsService metricsService;
    private final ActaCierrePdfGenerator pdfGenerator;
    private final IStorageProvider storageProvider;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;

    public ProjectClosureServiceImpl(ProyectoRepository proyectoRepository,
                                     ActaCierreRepository actaCierreRepository,
                                     SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
                                     IProgressCalculator progressCalculator,
                                     ProjectClosureValidator closureValidator,
                                     ProjectProgressMetricsService metricsService,
                                     ActaCierrePdfGenerator pdfGenerator,
                                     IStorageProvider storageProvider,
                                     ObjectMapper objectMapper,
                                     NotificationEventPublisherPort notificationPublisher,
                                     KeycloakIdentityExtractor identityExtractor) {
        this.proyectoRepository = proyectoRepository;
        this.actaCierreRepository = actaCierreRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.progressCalculator = progressCalculator;
        this.closureValidator = closureValidator;
        this.metricsService = metricsService;
        this.pdfGenerator = pdfGenerator;
        this.storageProvider = storageProvider;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
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
        closureValidator.validarDatosActa(projectId);

        LocalDateTime fechaCierre = LocalDateTime.now();
        LocalDate corteCalculo = fechaCierre.toLocalDate();
        LocalDate transferenciaFecha = corteCalculo;

        BigDecimal avanceFinal = progressCalculator.calcularYActualizarAvanceProyecto(projectId);
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, corteCalculo);
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible persistir el snapshot de avance del acta de cierre.", ex);
        }

        SeguridadUsuarioProyecto directorAsignado = usuarioProyectoRepository
                .findActiveDirectorAssignmentsByProyectoId(projectId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No fue posible obtener el director asignado del proyecto."));

        Patrocinador patrocinador = proyecto.getPatrocinador();
        String directorEntidad = firstNonBlank(
                directorAsignado.getUsuario() != null ? directorAsignado.getUsuario().getDependencia() : null,
                proyecto.getDependencia()
        );
        String patrocinadorEntidad = patrocinador != null ? patrocinador.getEntidad() : null;

        List<ObjetivoEspecifico> objetivosEspecificos = proyecto.getObjetivosEspecificos() == null
                ? List.of()
                : proyecto.getObjetivosEspecificos().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ObjetivoEspecifico::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = construirEntregablesActa(proyecto, corteCalculo);
        ActaCierrePdfGenerator.ActaCierrePdfData pdfData = new ActaCierrePdfGenerator.ActaCierrePdfData(
                proyecto.getId(),
                proyecto.getNombre(),
                patrocinador != null ? patrocinador.getNombre() : null,
                patrocinador != null ? patrocinador.getCargo() : null,
                patrocinadorEntidad,
                directorAsignado.getUsuario() != null ? directorAsignado.getUsuario().getNombre() : proyecto.getDirector(),
                directorAsignado.getCargo(),
                directorEntidad,
                formatDate(proyecto.getFechaInicio()),
                formatDate(corteCalculo),
                calculateDurationMonths(proyecto.getFechaInicio(), corteCalculo),
                proyecto.getObjetivoGeneral(),
                objetivosEspecificos.stream()
                        .map(ObjetivoEspecifico::getDescripcion)
                        .filter(value -> value != null && !value.isBlank())
                        .toList(),
                request.resumenEjecutivo(),
                request.leccionesPositivas(),
                request.leccionesMejorar(),
                request.recomendaciones(),
                request.transferenciaActividad(),
                formatDate(transferenciaFecha),
                request.transferenciaUbicacionEvidencia(),
                formatPercent(avanceFinal),
                formatPercent(snapshot.progresoProgramado()),
                formatPercent(snapshot.progresoEjecutado()),
                formatPercent(snapshot.diferencia()),
                formatRatio(snapshot.eficacia()),
                snapshot.estado(),
                entregables
        );

        byte[] pdfBytes = pdfGenerator.build(pdfData);
        String nombreArchivo = buildActaFileName(projectId, fechaCierre);
        String rutaArchivo = STORAGE_SUBDIR;
        String storedFileName = storageProvider.storeBytes(pdfBytes, rutaArchivo, nombreArchivo);

        try {
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
            acta.setArchivoPdf(storedFileName);
            acta.setRutaArchivoPdf(rutaArchivo);

            actaCierreRepository.save(acta);

            proyecto.cerrar();
            proyecto.setAvanceTotal(avanceFinal);
            proyectoRepository.save(proyecto);
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.PROJECT_CLOSED,
                    projectId,
                    "system",
                    java.util.Map.of(
                            "projectName", proyecto.getNombre(),
                            "state", proyecto.getEstadoCodigo(),
                            "recipients", List.of(proyecto.getCorreoDirector())
                    )));
        } catch (RuntimeException ex) {
            storageProvider.deleteFile(rutaArchivo, storedFileName);
            throw ex;
        }

        return CierreProyectoResponse.success(
                "Proyecto cerrado exitosamente. El acta de cierre quedó generada y disponible para descarga.",
                fechaCierre,
                avanceFinal,
                storedFileName,
                "/api/v1/proyectos/" + projectId + "/cierre/descargar"
        );
    }

    @Override
    @Transactional
    public CierreProyectoResponse solicitarCierre(String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error("El proyecto ya se encuentra cerrado o finalizado.");
        }

        if (Boolean.TRUE.equals(proyecto.getCierreSolicitado())) {
            return CierreProyectoResponse.error("Ya existe una solicitud de cierre activa para este proyecto.");
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now();
        proyecto.setCierreSolicitado(true);
        proyecto.setCierreSolicitadoEn(now);
        proyecto.setCierreSolicitadoPor(actorUsername);
        proyectoRepository.save(proyecto);

        List<String> gestorEmails = usuarioProyectoRepository
                .findActivasByProyectoIdAndCargoIn(projectId, List.of("GESTOR_TIC", "gestor_tic", "Gestor TIC"))
                .stream()
                .map(SeguridadUsuarioProyecto::getUsuario)
                .map(u -> u.getCorreo())
                .filter(Objects::nonNull)
                .toList();

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_REQUESTED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "requester", actorUsername,
                        "recipients", gestorEmails.isEmpty() ? List.of("sistema@proyecta.com") : gestorEmails
                )));

        return CierreProyectoResponse.success(
                "Solicitud de cierre enviada al Gestor del proyecto.",
                now,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarActaCierre(String projectId) {
        ActaCierre acta = actaCierreRepository.findByProyectoId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un acta de cierre para el proyecto: " + projectId));

        if (acta.getArchivoPdf() == null || acta.getRutaArchivoPdf() == null) {
            throw new ResourceNotFoundException("El acta de cierre no tiene un archivo PDF asociado.");
        }

        return storageProvider.loadFileAsResource(acta.getRutaArchivoPdf(), acta.getArchivoPdf());
    }

    private List<ActaCierrePdfGenerator.EntregableActaItem> construirEntregablesActa(Proyecto proyecto, LocalDate corteCalculo) {
        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = new ArrayList<>();

        List<Fase> fases = proyecto.getFases() == null ? List.of() : proyecto.getFases();
        fases.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Fase::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .forEach(fase -> {
                    List<Hito> hitos = fase.getHitos() == null ? List.of() : fase.getHitos();
                    hitos.stream()
                            .filter(Objects::nonNull)
                            .sorted(ProjectHierarchyOrdering.HITOS_BY_SEQUENCE)
                            .forEach(hito -> {
                                List<Entregable> items = hito.getEntregables() == null ? List.of() : hito.getEntregables();
                                items.stream()
                                        .filter(Objects::nonNull)
                                        .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_SCHEDULE)
                                        .forEach(entregable -> entregables.add(new ActaCierrePdfGenerator.EntregableActaItem(
                                                entregable.getNombre(),
                                                formatDate(entregable.getFechaEntregaReal() != null ? entregable.getFechaEntregaReal() : entregable.getFechaLimite()),
                                                entregable.getArchivoPdf() != null ? "Si" : "No",
                                                construirEstadoEntregable(entregable, corteCalculo)
                                        )));
                            });
                });

        return entregables;
    }

    private String construirEstadoEntregable(Entregable entregable, LocalDate corteCalculo) {
        if (entregable.esConforme()) {
            return "A conformidad";
        }
        if (entregable.getFechaLimite() != null && entregable.getFechaLimite().isBefore(corteCalculo)) {
            return "Vencido";
        }
        return "Pendiente";
    }

    private String buildActaFileName(String projectId, LocalDateTime fechaCierre) {
        return "acta_cierre_" + projectId + "_" + fechaCierre.format(FILE_DATE_FORMAT) + ".pdf";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "No registrado" : date.format(UI_DATE_FORMAT);
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "0.00%";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String formatRatio(BigDecimal value) {
        if (value == null) {
            return "0.0000";
        }
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private String calculateDurationMonths(LocalDate fechaInicio, LocalDate fechaCierre) {
        if (fechaInicio == null || fechaCierre == null) {
            return "0";
        }

        long months = java.time.temporal.ChronoUnit.MONTHS.between(fechaInicio.withDayOfMonth(1), fechaCierre.withDayOfMonth(1));
        return String.valueOf(Math.max(months, 0L));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
