package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.proyecta.api_gestion.repository.closure.ProjectClosureRecordRepository;
import com.proyecta.api_gestion.repository.closure.ClosureAnswerRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.report.ActaCierrePdfGenerator;
import com.proyecta.api_gestion.service.closure.DynamicClosurePdfService;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.closure.TemplateResolver;
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
    private final DynamicClosurePdfService dynamicPdfService;
    private final ClosureTemplateService templateService;
    private final ClosureAnswerRepository closureAnswerRepository;
    private final ProjectClosureRecordRepository closureRecordRepository;
    private final TemplateResolver templateResolver;

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
                                     KeycloakIdentityExtractor identityExtractor,
                                     DynamicClosurePdfService dynamicPdfService,
                                     ClosureTemplateService templateService,
                                     ClosureAnswerRepository closureAnswerRepository,
                                     ProjectClosureRecordRepository closureRecordRepository,
                                     TemplateResolver templateResolver) {
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
        this.dynamicPdfService = dynamicPdfService;
        this.templateService = templateService;
        this.closureAnswerRepository = closureAnswerRepository;
        this.closureRecordRepository = closureRecordRepository;
        this.templateResolver = templateResolver;
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

        byte[] pdfBytes;
        if (request.formData() != null && !request.formData().isBlank()) {
            String templateJson = templateService.getActiveTemplateJson();
            templateService.validarFormData(templateJson, request.formData());
            java.util.Map<Long, String> answerMap = closureAnswerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            a -> a.getQuestion().getId(),
                            a -> a.getRespuesta() != null ? a.getRespuesta() : "",
                            (a, b) -> b
                    ));
            String resolvedTemplateJson = templateResolver.resolveTemplateForProject(templateJson, proyecto, answerMap);
            String mergedFormData = mergeProjectValuesIntoFormData(proyecto, request.formData());
            pdfBytes = dynamicPdfService.generatePdf(resolvedTemplateJson, mergedFormData,
                    "A-GT-FR-004", 1, "Acta de Cierre del Proyecto");
        } else {
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
            pdfBytes = pdfGenerator.build(pdfData);
        }
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
    public CierreProyectoResponse solicitarCierre(String projectId, CierreProyectoRequest request, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (proyecto.esEstadoTerminal()) {
            return CierreProyectoResponse.error("El proyecto ya se encuentra cerrado o finalizado.");
        }

        if (Boolean.TRUE.equals(proyecto.getCierreSolicitado())) {
            return CierreProyectoResponse.error("Ya existe una solicitud de cierre activa para este proyecto.");
        }

        if (request.formData() != null && !request.formData().isBlank()) {
            String templateJson = templateService.getActiveTemplateJson();
            templateService.validarFormData(templateJson, request.formData());
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now();
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
    @Transactional
    public CierreProyectoResponse aprobarCierre(String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (!proyecto.cierrePendienteRevision()) {
            return CierreProyectoResponse.error("No hay una solicitud de cierre pendiente de revision para este proyecto.");
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now();
        proyecto.setCierreEstado("APROBADO");
        proyecto.setCierreObservaciones(null);
        proyectoRepository.save(proyecto);

        String directorEmail = proyecto.getCorreoDirector();
        List<String> recipients = directorEmail != null ? List.of(directorEmail) : List.of("sistema@proyecta.com");

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_APPROVED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "approver", actorUsername,
                        "recipients", recipients
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
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (!proyecto.cierrePendienteRevision()) {
            return CierreProyectoResponse.error("No hay una solicitud de cierre pendiente de revision para este proyecto.");
        }

        if (observaciones == null || observaciones.isBlank()) {
            return CierreProyectoResponse.error("Debe ingresar una observacion o motivo del rechazo.");
        }

        String actorUsername = identityExtractor.resolveUsername(authentication);
        LocalDateTime now = LocalDateTime.now();
        proyecto.setCierreEstado("RECHAZADO");
        proyecto.setCierreObservaciones(observaciones.trim());
        proyecto.setCierreSolicitado(false);
        proyectoRepository.save(proyecto);

        String directorEmail = proyecto.getCorreoDirector();
        List<String> recipients = directorEmail != null ? List.of(directorEmail) : List.of("sistema@proyecta.com");

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.CLOSURE_REJECTED,
                projectId,
                actorUsername,
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "rejector", actorUsername,
                        "observaciones", observaciones.trim(),
                        "recipients", recipients
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
    @Transactional(readOnly = true)
    public Resource descargarActaCierre(String projectId) {
        var editablePdf = generarPdfEditableSiExiste(projectId);
        if (editablePdf != null) {
            return editablePdf;
        }

        ActaCierre acta = actaCierreRepository.findByProyectoId(projectId)
                .orElse(null);

        if (acta != null && acta.getArchivoPdf() != null && acta.getRutaArchivoPdf() != null) {
            return storageProvider.loadFileAsResource(acta.getRutaArchivoPdf(), acta.getArchivoPdf());
        }

        throw new ResourceNotFoundException(
                "No se pudo generar el acta de cierre. Asegurese de que la plantilla este configurada y el borrador guardado antes de descargar.");
    }

    private Resource generarPdfEditableSiExiste(String projectId) {
        var recordOpt = closureRecordRepository.findByProyectoId(projectId);
        if (recordOpt.isEmpty()) {
            return null;
        }

        var record = recordOpt.get();
        if (record.getTemplateSnapshot() == null || record.getFormData() == null) {
            return null;
        }

        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        try {
            java.util.Map<Long, String> answerMap = closureAnswerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            a -> a.getQuestion().getId(),
                            a -> a.getRespuesta() != null ? a.getRespuesta() : "",
                            (a, b) -> b
                    ));

            String resolvedTemplateJson = templateResolver.resolveTemplateForProject(
                    record.getTemplateSnapshot(),
                    proyecto,
                    answerMap
            );
            String mergedFormData = mergeProjectValuesIntoFormData(proyecto, record.getFormData());
            byte[] pdfBytes = dynamicPdfService.generatePdf(
                    resolvedTemplateJson,
                    mergedFormData,
                    "A-GT-FR-004",
                    1,
                    "Acta de Cierre del Proyecto"
            );

            return new org.springframework.core.io.ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return "acta-cierre-" + projectId + ".pdf";
                }
            };
        } catch (Exception ex) {
            log.warn("No se pudo generar PDF dinamico para proyecto {}: {}", projectId, ex.getMessage(), ex);
            return null;
        }
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

    private String mergeProjectValuesIntoFormData(Proyecto proyecto, String formDataJson) {
        try {
            JsonNode parsed = (formDataJson == null || formDataJson.isBlank())
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(formDataJson);
            ObjectNode root = parsed.isObject() ? (ObjectNode) parsed : objectMapper.createObjectNode();
            ObjectNode fields = root.has("fields") && root.get("fields").isObject()
                    ? (ObjectNode) root.get("fields")
                    : objectMapper.createObjectNode();

            putIfMissing(root, "codigo_proyecto", proyecto.getId());
            putIfMissing(root, "nombre_proyecto", proyecto.getNombre());
            putIfMissing(root, "patrocinador", proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : null);
            putIfMissing(root, "director", proyecto.getDirector());
            putIfMissing(root, "fecha_inicio", proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toString() : null);
            putIfMissing(root, "objetivo_general", proyecto.getObjetivoGeneral());

            copyToFields(fields, "codigo_proyecto", proyecto.getId());
            copyToFields(fields, "nombre_proyecto", proyecto.getNombre());
            copyToFields(fields, "patrocinador", proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : null);
            copyToFields(fields, "director", proyecto.getDirector());
            copyToFields(fields, "fecha_inicio", proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toString() : null);
            copyToFields(fields, "objetivo_general", proyecto.getObjetivoGeneral());

            root.set("fields", fields);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return formDataJson;
        }
    }

    private void putIfMissing(ObjectNode root, String key, String value) {
        if (value == null || value.isBlank() || root.hasNonNull(key)) {
            return;
        }
        root.put(key, value);
    }

    private void copyToFields(ObjectNode fields, String key, String value) {
        if (value == null || value.isBlank() || fields.hasNonNull(key)) {
            return;
        }
        fields.put(key, value);
    }
}
