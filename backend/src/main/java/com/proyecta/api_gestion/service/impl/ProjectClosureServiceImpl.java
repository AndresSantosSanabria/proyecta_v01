package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.ActaCierre;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.domain.model.Patrocinador;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.config.EstadoProyectoConfig;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.application.port.out.persistence.ActaCierreRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureAnswerRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.config.EstadoProyectoConfigRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import com.proyecta.api_gestion.service.report.ActaCierrePdfGenerator;
import com.proyecta.api_gestion.service.report.ActaCierreDocxGenerator;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectClosureServiceImpl implements ProjectClosureService {

    private static final Logger log = LoggerFactory.getLogger(ProjectClosureServiceImpl.class);
    private static final String STORAGE_SUBDIR = "actas_cierre";
    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String MSG_PROYECTO_NO_ENCONTRADO = "Proyecto no encontrado: ";
    private static final String MSG_PROYECTO_YA_CERRADO = "El proyecto ya se encuentra cerrado o finalizado.";
    private static final String EXT_DOCX = ".docx";
    private static final String KEY_PROJECT_NAME = "projectName";
    private static final String KEY_RECIPIENTS = "recipients";

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final ActaCierreRepositoryPort actaCierreRepositoryPort;
    private final SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort;
    private final IProgressCalculator progressCalculator;
    private final ProjectClosureValidator closureValidator;
    private final ProjectProgressMetricsService metricsService;
    private final ActaCierreDocxGenerator docxGenerator;
    private final IStorageProvider storageProvider;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ClosureTemplateService templateService;
    private final ClosureAnswerRepositoryPort closureAnswerRepositoryPort;
    private final PublicEvidenceAccessService publicEvidenceAccessService;
    private final PublicUrlProperties publicUrlProperties;
    private final EstadoProyectoConfigRepositoryPort estadoProyectoConfigRepositoryPort;

    public ProjectClosureServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                                     ActaCierreRepositoryPort actaCierreRepositoryPort,
                                     SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort,
                                     IProgressCalculator progressCalculator,
                                     ProjectClosureValidator closureValidator,
                                     ProjectProgressMetricsService metricsService,
                                     ActaCierreDocxGenerator docxGenerator,
                                     IStorageProvider storageProvider,
                                     ObjectMapper objectMapper,
                                     NotificationEventPublisherPort notificationPublisher,
                                     KeycloakIdentityExtractor identityExtractor,
                                     ClosureTemplateService templateService,
                                     ClosureAnswerRepositoryPort closureAnswerRepositoryPort,
                                     PublicEvidenceAccessService publicEvidenceAccessService,
                                     PublicUrlProperties publicUrlProperties,
                                     EstadoProyectoConfigRepositoryPort estadoProyectoConfigRepositoryPort) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.actaCierreRepositoryPort = actaCierreRepositoryPort;
        this.usuarioProyectoRepositoryPort = usuarioProyectoRepositoryPort;
        this.progressCalculator = progressCalculator;
        this.closureValidator = closureValidator;
        this.metricsService = metricsService;
        this.docxGenerator = docxGenerator;
        this.storageProvider = storageProvider;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
        this.templateService = templateService;
        this.closureAnswerRepositoryPort = closureAnswerRepositoryPort;
        this.publicEvidenceAccessService = publicEvidenceAccessService;
        this.publicUrlProperties = publicUrlProperties;
        this.estadoProyectoConfigRepositoryPort = estadoProyectoConfigRepositoryPort;
    }

    private EstadoProyectoConfig estadoProyectoConfig(String codigo) {
        return estadoProyectoConfigRepositoryPort.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Estado de proyecto no configurado: " + codigo));
    }

    @Override
    @Transactional
    public CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error(MSG_PROYECTO_YA_CERRADO);
        }

        closureValidator.validarCierre(projectId);
        closureValidator.validarDatosActa(projectId);

        if (request == null) {
            request = buildRequestFromSavedAnswers(projectId);
        } else {
            request = mergeRequestWithSavedAnswers(projectId, request);
        }

        LocalDateTime fechaCierre = LocalDateTime.now(ZoneId.systemDefault());
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

        SeguridadUsuarioProyecto directorAsignado = usuarioProyectoRepositoryPort
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
        ActaCierrePdfGenerator.ActaCierrePdfData actaData = new ActaCierrePdfGenerator.ActaCierrePdfData(
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
                entregables,
                List.of(),
                List.of()
        );
        byte[] docxBytes = docxGenerator.build(actaData);
        String nombreArchivo = buildActaFileName(projectId, fechaCierre, EXT_DOCX);
        String rutaArchivo = STORAGE_SUBDIR;
        String storedFileName = storageProvider.storeBytes(docxBytes, rutaArchivo, nombreArchivo);

        try {
            ActaCierre acta = new ActaCierre(
                    proyecto,
                    request.resumenEjecutivo(),
                    fechaCierre,
                    avanceFinal
            );
            applyActaSnapshot(acta, snapshot, snapshotJson, storedFileName, rutaArchivo);

            actaCierreRepositoryPort.save(acta);

            proyecto.cerrar(estadoProyectoConfig("CERRADO"));
            proyecto.setAvanceTotal(avanceFinal);
            proyectoRepositoryPort.save(proyecto);
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.PROJECT_CLOSED,
                    projectId,
                    "system",
                    java.util.Map.of(
                            KEY_PROJECT_NAME, proyecto.getNombre(),
                            "state", proyecto.getEstadoCodigo(),
                            KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
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
    public CierreProyectoResponse solicitarCierre(String projectId, CierreProyectoRequest request, Authentication authentication) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error(MSG_PROYECTO_YA_CERRADO);
        }

        if (Boolean.TRUE.equals(proyecto.getCierreSolicitado())) {
            return CierreProyectoResponse.error("Ya existe una solicitud de cierre activa para este proyecto.");
        }

        if (request == null) {
            request = buildRequestFromSavedAnswers(projectId);
        }

        if (request.formData() != null && !request.formData().isBlank()) {
            String templateJson = templateService.getActiveTemplateJson();
            templateService.validarFormData(templateJson, request.formData());
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        proyecto.setCierreSolicitado(true);
        proyecto.setCierreSolicitadoEn(now);
        proyecto.setCierreSolicitadoPor(actorUsername);
        proyecto.setCierreEstado("PENDIENTE");
        proyecto.setCierreObservaciones(null);
        
        try {
            proyecto.setCierreBorradorJson(objectMapper.writeValueAsString(request));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible guardar el borrador del acta de cierre", ex);
        }
        
        proyectoRepositoryPort.save(proyecto);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_REQUESTED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        "requester", actorUsername,
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
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
    @Transactional
    public CierreProyectoResponse aprobarCierre(String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        if (!proyecto.cierrePendienteRevision()) {
            return CierreProyectoResponse.error("No hay una solicitud de cierre pendiente de revision para este proyecto.");
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        proyecto.setCierreEstado("APROBADO");
        proyecto.setCierreObservaciones(null);
        proyectoRepositoryPort.save(proyecto);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_APPROVED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        "approver", actorUsername,
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
                )));

        return CierreProyectoResponse.success(
                "Cierre del proyecto aprobado exitosamente.",
                now,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public CierreProyectoResponse rechazarCierre(String projectId, String observaciones, Authentication authentication) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        if (!proyecto.cierrePendienteRevision()) {
            return CierreProyectoResponse.error("No hay una solicitud de cierre pendiente de revision para este proyecto.");
        }

        if (observaciones == null || observaciones.isBlank()) {
            return CierreProyectoResponse.error("Debe ingresar una observacion o motivo del rechazo.");
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        proyecto.setCierreEstado("RECHAZADO");
        proyecto.setCierreObservaciones(observaciones.trim());
        proyecto.setCierreSolicitado(false);
        proyectoRepositoryPort.save(proyecto);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_REJECTED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        "rejector", actorUsername,
                        "observaciones", observaciones.trim(),
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
                )));

        return CierreProyectoResponse.success(
                "Solicitud de cierre rechazada. El Director sera notificado con las observaciones.",
                now,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public CierreProyectoResponse cierreExtraordinario(String projectId, CierreProyectoRequest request, Authentication authentication) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error(MSG_PROYECTO_YA_CERRADO);
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);

        if (request == null) {
            request = buildRequestFromSavedAnswers(projectId);
        } else {
            request = mergeRequestWithSavedAnswers(projectId, request);
        }

        LocalDateTime fechaCierre = LocalDateTime.now(ZoneId.systemDefault());
        LocalDate corteCalculo = fechaCierre.toLocalDate();

        BigDecimal avanceFinal = progressCalculator.calcularYActualizarAvanceProyecto(projectId);
        ProyectoAvanceResponseDTO snapshot = metricsService.construir(proyecto, corteCalculo);
        String snapshotJson = serializeSnapshot(snapshot);

        ActaCierrePdfGenerator.ActaCierrePdfData actaData = buildActaDataExtraordinario(
                projectId, proyecto, request, snapshot, corteCalculo, avanceFinal);
        byte[] docxBytes = docxGenerator.build(actaData);
        String nombreArchivo = buildActaFileName(projectId, fechaCierre, EXT_DOCX);
        String rutaArchivo = STORAGE_SUBDIR;
        String storedFileName = storageProvider.storeBytes(docxBytes, rutaArchivo, nombreArchivo);

        try {
            guardarActaExtraordinaria(projectId, proyecto, request, snapshot, fechaCierre,
                    avanceFinal, snapshotJson, storedFileName, rutaArchivo, actorUsername);
        } catch (RuntimeException ex) {
            storageProvider.deleteFile(rutaArchivo, storedFileName);
            throw ex;
        }

        return CierreProyectoResponse.success(
                "Proyecto cerrado extraordinariamente. El acta de cierre quedo generada y disponible para descarga.",
                fechaCierre,
                avanceFinal,
                storedFileName,
                "/api/v1/proyectos/" + projectId + "/cierre/descargar"
        );
    }

    @Override
    @Transactional
    public Resource descargarActaCierre(String projectId) {
        Resource draftDocx = generarBorradorDocxSiExiste(projectId);
        if (draftDocx != null) {
            return draftDocx;
        }

        ActaCierre acta = actaCierreRepositoryPort.findByProyectoId(projectId).orElse(null);

        if (acta != null) {
            if (acta.getArchivoDocx() != null && acta.getRutaArchivoDocx() != null) {
                try {
                    return storageProvider.loadFileAsResource(acta.getRutaArchivoDocx(), acta.getArchivoDocx());
                } catch (Exception ex) {
                    log.warn("No se pudo cargar el DOCX almacenado para proyecto {}: {}", projectId, ex.getMessage());
                }
            }

            if (acta.getArchivoPdf() != null && acta.getRutaArchivoPdf() != null) {
                try {
                    return storageProvider.loadFileAsResource(acta.getRutaArchivoPdf(), acta.getArchivoPdf());
                } catch (Exception ex) {
                    log.warn("No se pudo cargar el PDF almacenado para proyecto {}: {}", projectId, ex.getMessage());
                }
            }
        }

        if (acta == null) {
            throw new ResourceNotFoundException("No existe un acta de cierre para el proyecto: " + projectId
                    + ". Verifique que la plantilla este configurada y las respuestas esten completas.");
        }

        throw new ResourceNotFoundException("El acta de cierre no tiene un archivo asociado para descarga.");
    }

    private Resource generarBorradorDocxSiExiste(String projectId) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + projectId));

        CierreProyectoRequest request = buildRequestFromSavedAnswers(projectId);

        LocalDate corteCalculo = LocalDate.now(ZoneId.systemDefault());
        LocalDate transferenciaFecha = resolveTransferenciaFecha(request, corteCalculo);

        DirectorInfo director = resolveDirectorBorrador(projectId, proyecto);
        Patrocinador patrocinador = proyecto.getPatrocinador();
        String patrocinadorEntidad = patrocinador != null ? patrocinador.getEntidad() : null;

        List<ObjetivoEspecifico> objetivosEspecificos = ordenarObjetivos(proyecto);

        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = construirEntregablesActa(proyecto, corteCalculo);

        ProyectoAvanceResponseDTO snapshot = construirSnapshotBorrador(proyecto, corteCalculo);

        ActaCierrePdfGenerator.ActaCierrePdfData actaData = buildActaDataBorrador(proyecto, request, patrocinador,
                patrocinadorEntidad, director, objetivosEspecificos, entregables, snapshot, corteCalculo, transferenciaFecha);

        return buildResourceBorrador(actaData, projectId);
    }

    private String buildPublicEvidenceUrl(String token) {
        String base = publicUrlProperties.getBase();
        String normalized = (base == null) ? "" : base;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/api/v1/public/evidencia/" + token;
    }

    private List<ActaCierrePdfGenerator.EntregableActaItem> construirEntregablesActa(Proyecto proyecto, LocalDate corteCalculo) {
        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = new ArrayList<>();

        List<Fase> fases = proyecto.getFases() == null ? List.of() : proyecto.getFases();
        for (Fase fase : ordenarNoNulos(fases, ProjectHierarchyOrdering.FASES_BY_ORDEN)) {
            for (Hito hito : ordenarNoNulos(fase.getHitos(), ProjectHierarchyOrdering.HITOS_BY_ORDEN)) {
                for (Entregable entregable : ordenarNoNulos(hito.getEntregables(), ProjectHierarchyOrdering.ENTREGABLES_BY_ORDEN)) {
                    entregables.add(construirItemEntregableActa(entregable, corteCalculo));
                }
            }
        }

        return entregables;
    }

    private String construirEstadoEntregable(Entregable entregable, LocalDate corteCalculo) {
        if (entregable.esConforme()) {
            return "Aprobado";
        }
        if (entregable.getFechaLimite() != null && entregable.getFechaLimite().isBefore(corteCalculo)) {
            return "Vencido";
        }
        return "Pendiente";
    }

    private String buildActaFileName(String projectId, LocalDateTime fechaCierre, String extension) {
        return "acta_cierre_" + projectId + "_" + fechaCierre.format(FILE_DATE_FORMAT) + extension;
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

    private String serializeSnapshot(ProyectoAvanceResponseDTO snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible persistir el snapshot de avance del acta de cierre.", ex);
        }
    }

    private DirectorInfo resolveDirectorExtraordinario(String projectId, Proyecto proyecto) {
        SeguridadUsuarioProyecto directorAsignado = usuarioProyectoRepositoryPort
                .findActiveDirectorAssignmentsByProyectoId(projectId)
                .stream()
                .findFirst()
                .orElse(null);

        String directorNombre = proyecto.getDirector();
        String directorCargo = null;
        String directorEntidad = proyecto.getDependencia();
        if (directorAsignado != null) {
            directorCargo = directorAsignado.getCargo();
            if (directorAsignado.getUsuario() != null) {
                directorNombre = directorAsignado.getUsuario().getNombre();
                directorEntidad = firstNonBlank(directorAsignado.getUsuario().getDependencia(), proyecto.getDependencia());
            }
        }
        return new DirectorInfo(directorNombre, directorCargo, directorEntidad);
    }

    private DirectorInfo resolveDirectorBorrador(String projectId, Proyecto proyecto) {
        SeguridadUsuarioProyecto directorAsignado = usuarioProyectoRepositoryPort
                .findActiveDirectorAssignmentsByProyectoId(projectId)
                .stream()
                .findFirst()
                .orElse(null);

        String directorEntidad = null;
        String directorCargo = null;
        String directorNombre = proyecto.getDirector();
        if (directorAsignado != null) {
            directorCargo = directorAsignado.getCargo();
            if (directorAsignado.getUsuario() != null) {
                directorNombre = directorAsignado.getUsuario().getNombre();
                directorEntidad = firstNonBlank(directorAsignado.getUsuario().getDependencia(), proyecto.getDependencia());
            } else {
                directorEntidad = proyecto.getDependencia();
            }
        }
        return new DirectorInfo(directorNombre, directorCargo, directorEntidad);
    }

    private List<ObjetivoEspecifico> ordenarObjetivos(Proyecto proyecto) {
        return proyecto.getObjetivosEspecificos() == null
                ? List.of()
                : proyecto.getObjetivosEspecificos().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ObjetivoEspecifico::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private SnapshotFields formatSnapshotFields(ProyectoAvanceResponseDTO snapshot) {
        if (snapshot == null) {
            return new SnapshotFields(
                    formatPercent(BigDecimal.ZERO),
                    formatPercent(BigDecimal.ZERO),
                    formatPercent(BigDecimal.ZERO),
                    formatRatio(BigDecimal.ZERO),
                    "SIN_DATOS");
        }
        return new SnapshotFields(
                formatPercent(snapshot.progresoProgramado()),
                formatPercent(snapshot.progresoEjecutado()),
                formatPercent(snapshot.diferencia()),
                formatRatio(snapshot.eficacia()),
                snapshot.estado());
    }

    private RequestFields nullSafeRequestFields(CierreProyectoRequest request) {
        if (request == null) {
            return new RequestFields(null, null, null, null, null, null);
        }
        return new RequestFields(
                request.resumenEjecutivo(),
                request.leccionesPositivas(),
                request.leccionesMejorar(),
                request.recomendaciones(),
                request.transferenciaActividad(),
                request.transferenciaUbicacionEvidencia());
    }

    private void applyActaSnapshot(ActaCierre acta, ProyectoAvanceResponseDTO snapshot, String snapshotJson,
                                   String storedFileName, String rutaArchivo) {
        if (snapshot != null) {
            acta.setProgresoProgramadoFinal(snapshot.progresoProgramado());
            acta.setProgresoEjecutadoFinal(snapshot.progresoEjecutado());
            acta.setDiferenciaFinal(snapshot.diferencia());
            acta.setEficaciaFinal(snapshot.eficacia());
            acta.setEstadoFinal(snapshot.estado());
            acta.setCorteCalculo(snapshot.corte());
        }
        acta.setSnapshotJson(snapshotJson);
        acta.setArchivoDocx(storedFileName);
        acta.setRutaArchivoDocx(rutaArchivo);
    }

    @SuppressWarnings("java:S107")
    private void guardarActaExtraordinaria(String projectId, Proyecto proyecto, CierreProyectoRequest request,
                                           ProyectoAvanceResponseDTO snapshot, LocalDateTime fechaCierre,
                                           BigDecimal avanceFinal, String snapshotJson, String storedFileName,
                                           String rutaArchivo, String actorUsername) {
        actaCierreRepositoryPort.findByProyectoId(projectId)
                .ifPresent(actaCierreRepositoryPort::delete);

        ActaCierre acta = new ActaCierre(
                proyecto,
                request.resumenEjecutivo() != null ? request.resumenEjecutivo() : "Cierre extraordinario sin resumen",
                fechaCierre,
                avanceFinal
        );
        applyActaSnapshot(acta, snapshot, snapshotJson, storedFileName, rutaArchivo);

        actaCierreRepositoryPort.save(acta);

        proyecto.setAvanceTotal(avanceFinal);
        proyecto.setEstadoConfig(estadoProyectoConfig("CERRADO_FORZOSO"));
        proyecto.setCierreEstado("EXTRAORDINARIO");
        proyecto.setCierreSolicitado(false);
        proyecto.setCierreObservaciones("Cierre extraordinario realizado por " + actorUsername);
        proyectoRepositoryPort.save(proyecto);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_CLOSED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        "state", proyecto.getEstadoCodigo(),
                        "extraordinaryClosure", "true",
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
                )));
    }

    private ActaCierrePdfGenerator.ActaCierrePdfData buildActaDataExtraordinario(String projectId, Proyecto proyecto,
            CierreProyectoRequest request, ProyectoAvanceResponseDTO snapshot, LocalDate corteCalculo, BigDecimal avanceFinal) {
        DirectorInfo director = resolveDirectorExtraordinario(projectId, proyecto);

        Patrocinador patrocinador = proyecto.getPatrocinador();
        String patrocinadorEntidad = patrocinador != null ? patrocinador.getEntidad() : null;

        List<ObjetivoEspecifico> objetivosEspecificos = ordenarObjetivos(proyecto);
        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = construirEntregablesActa(proyecto, corteCalculo);
        SnapshotFields snapshotFields = formatSnapshotFields(snapshot);

        return new ActaCierrePdfGenerator.ActaCierrePdfData(
                proyecto.getId(),
                proyecto.getNombre(),
                patrocinador != null ? patrocinador.getNombre() : null,
                patrocinador != null ? patrocinador.getCargo() : null,
                patrocinadorEntidad,
                director.nombre(),
                director.cargo(),
                director.entidad(),
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
                formatDate(corteCalculo),
                request.transferenciaUbicacionEvidencia(),
                formatPercent(avanceFinal),
                snapshotFields.progresoProgramado(),
                snapshotFields.progresoEjecutado(),
                snapshotFields.diferencia(),
                snapshotFields.eficacia(),
                snapshotFields.estado(),
                entregables,
                List.of(),
                List.of()
        );
    }

    @SuppressWarnings("java:S107")
    private ActaCierrePdfGenerator.ActaCierrePdfData buildActaDataBorrador(Proyecto proyecto, CierreProyectoRequest request,
            Patrocinador patrocinador, String patrocinadorEntidad, DirectorInfo director,
            List<ObjetivoEspecifico> objetivosEspecificos, List<ActaCierrePdfGenerator.EntregableActaItem> entregables,
            ProyectoAvanceResponseDTO snapshot, LocalDate corteCalculo, LocalDate transferenciaFecha) {
        BigDecimal avanceTotal = proyecto.getAvanceTotal() != null ? proyecto.getAvanceTotal() : BigDecimal.ZERO;
        SnapshotFields snapshotFields = formatSnapshotFields(snapshot);
        RequestFields requestFields = nullSafeRequestFields(request);

        return new ActaCierrePdfGenerator.ActaCierrePdfData(
                proyecto.getId(),
                proyecto.getNombre(),
                patrocinador != null ? patrocinador.getNombre() : null,
                patrocinador != null ? patrocinador.getCargo() : null,
                patrocinadorEntidad,
                director.nombre(),
                director.cargo(),
                director.entidad(),
                formatDate(proyecto.getFechaInicio()),
                formatDate(corteCalculo),
                calculateDurationMonths(proyecto.getFechaInicio(), corteCalculo),
                proyecto.getObjetivoGeneral(),
                objetivosEspecificos.stream()
                        .map(ObjetivoEspecifico::getDescripcion)
                        .filter(value -> value != null && !value.isBlank())
                        .toList(),
                requestFields.resumenEjecutivo(),
                requestFields.leccionesPositivas(),
                requestFields.leccionesMejorar(),
                requestFields.recomendaciones(),
                requestFields.transferenciaActividad(),
                formatDate(transferenciaFecha),
                requestFields.transferenciaUbicacion(),
                formatPercent(avanceTotal),
                snapshotFields.progresoProgramado(),
                snapshotFields.progresoEjecutado(),
                snapshotFields.diferencia(),
                snapshotFields.eficacia(),
                snapshotFields.estado(),
                entregables,
                List.of(),
                List.of()
        );
    }

    private LocalDate resolveTransferenciaFecha(CierreProyectoRequest request, LocalDate corteCalculo) {
        return (request != null && request.transferenciaFecha() != null)
                ? request.transferenciaFecha() : corteCalculo;
    }

    private ProyectoAvanceResponseDTO construirSnapshotBorrador(Proyecto proyecto, LocalDate corteCalculo) {
        try {
            return metricsService.construir(proyecto, corteCalculo);
        } catch (Exception e) {
            log.warn("Error construyendo metricas en borrador: {}", e.getMessage());
            return null;
        }
    }

    private Resource buildResourceBorrador(ActaCierrePdfGenerator.ActaCierrePdfData actaData, String projectId) {
        try {
            byte[] docxBytes = docxGenerator.build(actaData);
            return new org.springframework.core.io.ByteArrayResource(docxBytes) {
                @Override
                public String getFilename() {
                    return "acta_cierre_" + projectId + EXT_DOCX;
                }
            };
        } catch (Exception ex) {
            log.error("Error generando DOCX borrador: {}", ex.getMessage());
            return null;
        }
    }

    private <T> List<T> ordenarNoNulos(List<T> items, Comparator<T> comparator) {
        List<T> source = items == null ? List.of() : items;
        return source.stream()
                .filter(Objects::nonNull)
                .sorted(comparator)
                .toList();
    }

    private String construirUrlEvidencia(Entregable entregable) {
        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            return "";
        }
        try {
            String token = publicEvidenceAccessService.getOrCreateToken(entregable, "system");
            return buildPublicEvidenceUrl(token);
        } catch (Exception ex) {
            log.warn("No se pudo generar token de evidencia para entregable {}: {}", entregable.getId(), ex.getMessage());
            return "";
        }
    }

    private ActaCierrePdfGenerator.EntregableActaItem construirItemEntregableActa(Entregable entregable, LocalDate corteCalculo) {
        String evidenciaUrl = construirUrlEvidencia(entregable);
        return new ActaCierrePdfGenerator.EntregableActaItem(
                entregable.getNombre(),
                formatDate(entregable.getFechaEntregaReal() != null ? entregable.getFechaEntregaReal() : entregable.getFechaLimite()),
                entregable.getArchivoPdf() != null ? "Si" : "No",
                evidenciaUrl,
                construirEstadoEntregable(entregable, corteCalculo),
                entregable.getDescripcion()
        );
    }

    private TransferenciaFields parseTransferencia(String transferenciaRaw, String projectId) {
        if (transferenciaRaw == null || !transferenciaRaw.startsWith("[")) {
            return new TransferenciaFields(transferenciaRaw, null, "Evidencia en el sistema");
        }

        String transferenciaActividad = transferenciaRaw;
        LocalDate transferenciaFecha = null;
        String transferenciaUbicacion = "Evidencia en el sistema";

        try {
            com.fasterxml.jackson.databind.JsonNode entries = objectMapper.readTree(transferenciaRaw);
            StringBuilder sb = new StringBuilder();
            StringBuilder fechas = new StringBuilder();
            StringBuilder evidencias = new StringBuilder();
            for (com.fasterxml.jackson.databind.JsonNode entry : entries) {
                TransferenciaEntry data = readTransferenciaEntry(entry);
                acumularActividadTransferencia(data.act(), sb);
                acumularFechaTransferencia(data.fecha(), fechas);
                acumularEvidenciaTransferencia(data.evidencia(), data.storedName(), evidencias);
            }
            if (!sb.isEmpty()) transferenciaActividad = sb.toString();
            if (!fechas.isEmpty()) {
                transferenciaFecha = parseFechaTransferencia(fechas.toString(), projectId);
            }
            if (!evidencias.isEmpty()) transferenciaUbicacion = evidencias.toString();
        } catch (Exception ex) {
            // CWE-390: sin respuestas de cierre no se puede armar el resumen;
            // el fallo se registra para que el borrador no quede vacio sin rastro.
            log.warn("No se pudieron leer las respuestas de cierre del proyecto {}: {}",
                    projectId, ex.toString());
        }

        return new TransferenciaFields(transferenciaActividad, transferenciaFecha, transferenciaUbicacion);
    }

    private TransferenciaEntry readTransferenciaEntry(com.fasterxml.jackson.databind.JsonNode entry) {
        String act = entry.has("actividad") ? entry.get("actividad").asText("") : "";
        String fecha = entry.has("fecha") ? entry.get("fecha").asText("") : "";
        String evidencia = entry.has("evidenciaNombre") ? entry.get("evidenciaNombre").asText("") : "";
        String storedName = entry.has("evidenciaStoredName") ? entry.get("evidenciaStoredName").asText("") : "";
        return new TransferenciaEntry(act, fecha, evidencia, storedName);
    }

    private void acumularActividadTransferencia(String act, StringBuilder sb) {
        if (act.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n---\n");
        }
        sb.append(act);
    }

    private void acumularFechaTransferencia(String fecha, StringBuilder fechas) {
        if (fecha.isBlank()) {
            return;
        }
        if (!fechas.isEmpty()) {
            fechas.append(";");
        }
        fechas.append(fecha);
    }

    private void acumularEvidenciaTransferencia(String evidencia, String storedName, StringBuilder evidencias) {
        if (evidencia.isBlank()) {
            return;
        }
        if (!evidencias.isEmpty()) {
            evidencias.append(";");
        }
        evidencias.append(storedName.isEmpty() ? evidencia : storedName);
    }

    private void registrarRespuestasVacias(String projectId, String resumenEjecutivo, String leccionesPositivas,
            String leccionesMejorar, String recomendaciones, String transferenciaActividad) {
        if (resumenEjecutivo.isBlank() || leccionesPositivas.isBlank() || leccionesMejorar.isBlank()
                || recomendaciones.isBlank() || (transferenciaActividad != null && transferenciaActividad.isBlank())) {
            log.warn("El acta de cierre del proyecto {} tiene respuestas vacias en uno o mas campos obligatorios.", projectId);
        }
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

    private CierreProyectoRequest buildRequestFromSavedAnswers(String projectId) {
        List<com.proyecta.api_gestion.domain.model.closure.ClosureAnswer> answers =
                closureAnswerRepositoryPort.findByProyectoIdOrderByQuestionOrdenAsc(projectId);

        String resumenEjecutivo = findAnswerByOrderOrKeyword(answers, 1, "resumen");
        String leccionesPositivas = findAnswerByOrderOrKeyword(answers, 2, "positivo");
        String leccionesMejorar = findAnswerByOrderOrKeyword(answers, 3, "mejor");
        String recomendaciones = findAnswerByOrderOrKeyword(answers, 4, "recomend");
        String transferenciaRaw = findAnswerByOrderOrKeyword(answers, 5, "transfer");

        TransferenciaFields transferencia = parseTransferencia(transferenciaRaw, projectId);

        registrarRespuestasVacias(projectId, resumenEjecutivo, leccionesPositivas, leccionesMejorar,
                recomendaciones, transferencia.actividad());

        return new CierreProyectoRequest(
                resumenEjecutivo,
                leccionesPositivas,
                leccionesMejorar,
                recomendaciones,
                transferencia.actividad(),
                transferencia.fecha(),
                transferencia.ubicacion(),
                null,
                null
        );
    }

    private LocalDate parseFechaTransferencia(String fechasConcatenadas, String projectId) {
        try {
            return LocalDate.parse(fechasConcatenadas.split(";")[0].trim());
        } catch (Exception ex) {
            // CWE-390: fecha de transferencia no parseable; se deja sin valor.
            log.warn("No se pudo parsear la fecha de transferencia del proyecto {}: {}",
                    projectId, ex.toString());
            return null;
        }
    }

    private CierreProyectoRequest mergeRequestWithSavedAnswers(String projectId, CierreProyectoRequest request) {
        CierreProyectoRequest saved = buildRequestFromSavedAnswers(projectId);
        return new CierreProyectoRequest(
                firstNonBlank(request.resumenEjecutivo(), saved.resumenEjecutivo()),
                firstNonBlank(request.leccionesPositivas(), saved.leccionesPositivas()),
                firstNonBlank(request.leccionesMejorar(), saved.leccionesMejorar()),
                firstNonBlank(request.recomendaciones(), saved.recomendaciones()),
                firstNonBlank(request.transferenciaActividad(), saved.transferenciaActividad()),
                request.transferenciaFecha() != null ? request.transferenciaFecha() : saved.transferenciaFecha(),
                firstNonBlank(request.transferenciaUbicacionEvidencia(), saved.transferenciaUbicacionEvidencia()),
                request.fechaCierre(),
                request.formData()
        );
    }

    private String findAnswerByOrderOrKeyword(List<com.proyecta.api_gestion.domain.model.closure.ClosureAnswer> answers, int order, String keyword) {
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        if (order > 0) {
            String byOrder = answers.stream()
                    .filter(a -> a != null && a.getQuestion() != null && Integer.valueOf(order).equals(a.getQuestion().getOrden()))
                    .map(this::safeAnswer)
                    .filter(v -> !v.isBlank())
                    .findFirst()
                    .orElse("");
            if (!byOrder.isBlank()) {
                return byOrder;
            }
        }
        return answers.stream()
                .filter(a -> a != null && a.getQuestion() != null && a.getQuestion().getTexto() != null)
                .filter(a -> a.getQuestion().getTexto().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::safeAnswer)
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElse("");
    }

    private String safeAnswer(com.proyecta.api_gestion.domain.model.closure.ClosureAnswer answer) {
        return answer == null || answer.getRespuesta() == null ? "" : answer.getRespuesta().trim();
    }

    private record DirectorInfo(String nombre, String cargo, String entidad) {
    }

    private record SnapshotFields(String progresoProgramado, String progresoEjecutado, String diferencia,
                                  String eficacia, String estado) {
    }

    private record RequestFields(String resumenEjecutivo, String leccionesPositivas, String leccionesMejorar,
                                 String recomendaciones, String transferenciaActividad, String transferenciaUbicacion) {
    }

    private record TransferenciaFields(String actividad, LocalDate fecha, String ubicacion) {
    }

    private record TransferenciaEntry(String act, String fecha, String evidencia, String storedName) {
    }
}

