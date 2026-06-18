package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.DocumentoArchivoDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoObservacionDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoVersionDTO;
import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.model.ActaCierre;
import com.proyecta.api_gestion.model.DocumentoAuditoria;
import com.proyecta.api_gestion.model.DocumentoObservacion;
import com.proyecta.api_gestion.model.DocumentoVersion;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.enums.DocumentoObservacionEstado;
import com.proyecta.api_gestion.model.enums.DocumentoVersionEstado;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.repository.ActaCierreRepository;
import com.proyecta.api_gestion.repository.DocumentoAuditoriaRepository;
import com.proyecta.api_gestion.repository.DocumentoObservacionRepository;
import com.proyecta.api_gestion.repository.DocumentoVersionRepository;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
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
    private final DocumentoVersionRepository documentoVersionRepository;
    private final DocumentoObservacionRepository documentoObservacionRepository;
    private final DocumentoAuditoriaRepository documentoAuditoriaRepository;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final NotificationEventPublisherPort notificationPublisher;
    private final ProyectoBeneficioImpactoService beneficioImpactoService;

    public ProjectAdvanceServiceImpl(ProyectoRepository proyectoRepository,
                                     EntregableRepository entregableRepository,
                                     IProgressCalculator progressCalculator,
                                     IStorageProvider storageProvider,
                                     EntityManager entityManager,
                                     ActaCierreRepository actaCierreRepository,
                                     ProjectProgressMetricsService metricsService,
                                     DocumentoVersionRepository documentoVersionRepository,
                                     DocumentoObservacionRepository documentoObservacionRepository,
                                     DocumentoAuditoriaRepository documentoAuditoriaRepository,
                                     LocalUserAuthorizationService localUserAuthorizationService,
                                     KeycloakIdentityExtractor identityExtractor,
                                     NotificationEventPublisherPort notificationPublisher,
                                     ProyectoBeneficioImpactoService beneficioImpactoService) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.progressCalculator = progressCalculator;
        this.storageProvider = storageProvider;
        this.entityManager = entityManager;
        this.actaCierreRepository = actaCierreRepository;
        this.metricsService = metricsService;
        this.documentoVersionRepository = documentoVersionRepository;
        this.documentoObservacionRepository = documentoObservacionRepository;
        this.documentoAuditoriaRepository = documentoAuditoriaRepository;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.identityExtractor = identityExtractor;
        this.notificationPublisher = notificationPublisher;
        this.beneficioImpactoService = beneficioImpactoService;
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
    public EntregableConformidadResponseDTO registrarEvidencia(String proyectoId, Integer entregableId, LocalDate fechaEntrega, MultipartFile evidencia, Authentication authentication) {
        if (evidencia == null || evidencia.isEmpty()) {
            throw new UnprocessableEntityException("El archivo de evidencia es requerido");
        }

        if (!"application/pdf".equals(evidencia.getContentType())) {
            throw new UnprocessableEntityException("El archivo debe ser un PDF");
        }

        validarPdfReal(evidencia);

        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        try {
            entregable.asegurarModificable();
        } catch (IllegalStateException ex) {
            throw new UnprocessableEntityException(ex.getMessage());
        }

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        if (entregable.getHito().getFase().getProyecto().esEstadoTerminal()) {
            throw new ForbiddenException("No se pueden modificar entregables de un proyecto cerrado.");
        }

        ActorContext actor = actorContext(authentication);
        boolean subsanaObservaciones = !documentoObservacionRepository
                .findByEntregableIdAndEstadoIn(entregableId, List.of(DocumentoObservacionEstado.ABIERTA))
                .isEmpty();
        String fileName = "evidencia_" + proyectoId + "_" + entregableId + "_" + System.currentTimeMillis();
        String storedName = null;

        try {
            storedName = storageProvider.storeFile(evidencia, "evidencias", fileName);
            DocumentoVersion nuevaVersion = registrarNuevaVersion(entregable, evidencia, storedName, fechaEntrega, actor);

            entregable.completar(storedName, fechaEntrega);
            entregableRepository.save(entregable);
            if (subsanaObservaciones) {
                marcarObservacionesSubsanadas(entregable, actor, "Nueva version cargada para subsanar observaciones.");
            }
            auditar(entregable, nuevaVersion, null, "UPLOAD", actor, "version=" + nuevaVersion.getNumeroVersion());
            entityManager.flush();

            Integer hitoId = entregable.getHito().getId();
            progressCalculator.calcularYActualizarAvanceHito(hitoId);
            progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

            entityManager.flush();
            entityManager.refresh(entregable);

            Proyecto proyectoActualizado = cargarProyecto(proyectoId);
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.DELIVERABLE_EVIDENCE_UPLOADED,
                    proyectoId,
                    actor.username(),
                    java.util.Map.of(
                            "deliverableName", entregable.getNombre(),
                            "projectName", proyectoActualizado.getNombre(),
                            "recipients", List.of(proyectoActualizado.getCorreoDirector())
                    )));
            ProyectoAvanceResponseDTO avance = metricsService.construir(proyectoActualizado, LocalDate.now());
            beneficioImpactoService.exigirSiCorresponde(proyectoId, avance, actor.username());
            String evidenciaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregable.getId() + "/evidencia";

            return new EntregableConformidadResponseDTO(
                    entregable.getId(),
                    entregable.getEstadoCodigo(),
                    entregable.getFechaEntregaReal(),
                    evidenciaUrl,
                    avance
            );
        } catch (RuntimeException ex) {
            if (storedName != null) {
                storageProvider.deleteFile("evidencias", storedName);
            }
            if (ex instanceof IllegalStateException) {
                throw new UnprocessableEntityException(ex.getMessage());
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public EntregableConformidadResponseDTO aprobarEntregable(String proyectoId, Integer entregableId, Authentication authentication) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            throw new UnprocessableEntityException("No se puede aprobar un entregable sin evidencia cargada.");
        }

        if (entregable.getHito().getFase().getProyecto().esEstadoTerminal()) {
            throw new ForbiddenException("No se pueden aprobar entregables de un proyecto cerrado.");
        }

        try {
            entregable.aprobarEvidencia();
        } catch (IllegalStateException ex) {
            throw new UnprocessableEntityException(ex.getMessage());
        }
        ActorContext actor = actorContext(authentication);
        cerrarObservaciones(entregable, actor);
        auditar(entregable, versionActual(entregable).orElse(null), null, "APROBAR", actor, "estado=A_CONFORMIDAD");
        entregableRepository.save(entregable);
        entityManager.flush();

        Integer hitoId = entregable.getHito().getId();
        progressCalculator.calcularYActualizarAvanceHito(hitoId);
        progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

        entityManager.flush();
        entityManager.refresh(entregable);

        Proyecto proyectoActualizado = cargarProyecto(proyectoId);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.DELIVERABLE_APPROVED,
                proyectoId,
                actor.username(),
                java.util.Map.of(
                        "deliverableName", entregable.getNombre(),
                        "projectName", proyectoActualizado.getNombre(),
                        "recipients", List.of(proyectoActualizado.getCorreoDirector())
                )));
        ProyectoAvanceResponseDTO avance = metricsService.construir(proyectoActualizado, LocalDate.now());
        beneficioImpactoService.exigirSiCorresponde(proyectoId, avance, actor.username());
        String evidenciaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregable.getId() + "/evidencia";

        return new EntregableConformidadResponseDTO(
                entregable.getId(),
                entregable.getEstadoCodigo(),
                entregable.getFechaEntregaReal(),
                evidenciaUrl,
                avance
        );
    }

    @Override
    @Transactional
    public EntregableConformidadResponseDTO rechazarEntregable(String proyectoId, Integer entregableId, String observacion, Authentication authentication) {
        if (observacion == null || observacion.isBlank()) {
            throw new UnprocessableEntityException("La observacion de rechazo es obligatoria.");
        }

        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        if (!entregable.getHito().getFase().getProyecto().getId().equals(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }

        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            throw new UnprocessableEntityException("No se puede rechazar un entregable sin evidencia cargada.");
        }

        if (entregable.getHito().getFase().getProyecto().esEstadoTerminal()) {
            throw new ForbiddenException("No se pueden rechazar entregables de un proyecto cerrado.");
        }

        try {
            entregable.rechazarEvidencia(observacion.trim());
        } catch (IllegalStateException ex) {
            throw new UnprocessableEntityException(ex.getMessage());
        }
        ActorContext actor = actorContext(authentication);
        DocumentoVersion version = asegurarVersionActual(entregable, actor);
        DocumentoObservacion observacionCreada = crearObservacion(entregable, version, observacion.trim(), actor);
        auditar(entregable, version, observacionCreada, "OBSERVAR", actor, "observacionId=" + observacionCreada.getId());
        entregableRepository.save(entregable);
        entityManager.flush();

        Integer hitoId = entregable.getHito().getId();
        progressCalculator.calcularYActualizarAvanceHito(hitoId);
        progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

        entityManager.flush();
        entityManager.refresh(entregable);

        Proyecto proyectoActualizado = cargarProyecto(proyectoId);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.DELIVERABLE_REJECTED,
                proyectoId,
                actor.username(),
                java.util.Map.of(
                        "deliverableName", entregable.getNombre(),
                        "projectName", proyectoActualizado.getNombre(),
                        "observation", observacion.trim(),
                        "recipients", List.of(proyectoActualizado.getCorreoDirector())
                )));
        ProyectoAvanceResponseDTO avance = metricsService.construir(proyectoActualizado, LocalDate.now());
        beneficioImpactoService.exigirSiCorresponde(proyectoId, avance, actor.username());
        String evidenciaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregable.getId() + "/evidencia";

        return new EntregableConformidadResponseDTO(
                entregable.getId(),
                entregable.getEstadoCodigo(),
                entregable.getFechaEntregaReal(),
                evidenciaUrl,
                avance
        );
    }

    @Override
    @Transactional
    public List<DocumentoVersionDTO> listarVersiones(String proyectoId, Integer entregableId, Authentication authentication) {
        Entregable entregable = cargarEntregableDelProyecto(proyectoId, entregableId);
        asegurarVersionActual(entregable, actorContext(authentication));
        return documentoVersionRepository.findByEntregableIdOrderByNumeroVersionDesc(entregableId).stream()
                .map(this::toVersionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoObservacionDTO> listarObservaciones(String proyectoId, Integer entregableId, Authentication authentication) {
        cargarEntregableDelProyecto(proyectoId, entregableId);
        return documentoObservacionRepository.findByEntregableIdOrderByCreadaEnDesc(entregableId).stream()
                .map(this::toObservacionDto)
                .toList();
    }

    @Override
    @Transactional
    public DocumentoObservacionDTO marcarObservacionSubsanada(String proyectoId, Integer entregableId, Long observacionId, String comentario, Authentication authentication) {
        cargarEntregableDelProyecto(proyectoId, entregableId);
        DocumentoObservacion observacion = documentoObservacionRepository.findByIdAndEntregableId(observacionId, entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Observacion no encontrada: " + observacionId));

        if (!DocumentoObservacionEstado.ABIERTA.equals(observacion.getEstado())) {
            throw new UnprocessableEntityException("Solo se pueden subsanar observaciones abiertas.");
        }

        ActorContext actor = actorContext(authentication);
        observacion.setEstado(DocumentoObservacionEstado.SUBSANADA);
        observacion.setSubsanadaPor(actor.username());
        observacion.setSubsanadaEn(LocalDateTime.now());
        observacion.setComentarioSubsanacion(trimToNull(comentario));
        DocumentoObservacion guardada = documentoObservacionRepository.save(observacion);
        auditar(guardada.getEntregable(), guardada.getVersion(), guardada, "SUBSANAR", actor, "observacionId=" + guardada.getId());
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.OBSERVATION_SUBSANATED,
                proyectoId,
                actor.username(),
                java.util.Map.of(
                        "observationId", guardada.getId(),
                        "deliverableName", guardada.getEntregable() != null ? guardada.getEntregable().getNombre() : "",
                        "recipients", List.of(
                                guardada.getEntregable() != null
                                        && guardada.getEntregable().getHito() != null
                                        && guardada.getEntregable().getHito().getFase() != null
                                        && guardada.getEntregable().getHito().getFase().getProyecto() != null
                                        ? guardada.getEntregable().getHito().getFase().getProyecto().getCorreoDirector()
                                        : null
                        )
                )));
        return toObservacionDto(guardada);
    }

    @Override
    @Transactional
    public EntregableConformidadResponseDTO revertirVersion(String proyectoId, Integer entregableId, Long versionId, String motivo, Authentication authentication) {
        if (motivo == null || motivo.isBlank()) {
            throw new UnprocessableEntityException("El motivo de reversion es obligatorio.");
        }

        Entregable entregable = cargarEntregableDelProyecto(proyectoId, entregableId);
        if (entregable.getHito().getFase().getProyecto().esEstadoTerminal()) {
            throw new ForbiddenException("No se pueden revertir documentos de un proyecto cerrado.");
        }

        DocumentoVersion version = documentoVersionRepository.findByIdAndEntregableId(versionId, entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Version documental no encontrada: " + versionId));

        if (!storageProvider.fileExists("evidencias", version.getArchivoStorage())) {
            throw new ResourceNotFoundException("El archivo fisico de la version seleccionada no esta disponible.");
        }

        ActorContext actor = actorContext(authentication);
        documentoVersionRepository.findByEntregableIdAndEstado(entregableId, DocumentoVersionEstado.ACTUAL)
                .forEach(actual -> {
                    actual.setEstado(DocumentoVersionEstado.HISTORICA);
                    documentoVersionRepository.save(actual);
                });

        version.setEstado(DocumentoVersionEstado.ACTUAL);
        documentoVersionRepository.save(version);

        entregable.setArchivoPdf(version.getArchivoStorage());
        entregable.setFechaEntregaReal(version.getFechaEntrega());
        entregable.setConforme(false);
        entregable.setEstadoConfig(null);
        entregable.setEstado(EstadoEntregable.EN_PROCESO);
        entregable.setObservacionRevision(null);
        entregableRepository.save(entregable);

        auditar(entregable, version, null, "REVERTIR", actor, "motivo=" + motivo.trim());

        Integer hitoId = entregable.getHito().getId();
        progressCalculator.calcularYActualizarAvanceHito(hitoId);
        progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);
        entityManager.flush();
        entityManager.refresh(entregable);

        return responseConAvance(proyectoId, entregable);
    }

    @Override
    @Transactional
    public DocumentoArchivoDTO obtenerArchivoVersion(String proyectoId, Integer entregableId, Long versionId, Authentication authentication) {
        cargarEntregableDelProyecto(proyectoId, entregableId);
        DocumentoVersion version = documentoVersionRepository.findByIdAndEntregableId(versionId, entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Version documental no encontrada: " + versionId));

        if (!storageProvider.fileExists("evidencias", version.getArchivoStorage())) {
            throw new ResourceNotFoundException("El archivo fisico de la version seleccionada no esta disponible.");
        }

        return new DocumentoArchivoDTO(version.getArchivoStorage(), version.getNombreArchivoOriginal(), version.getMimeType());
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

    private DocumentoVersion registrarNuevaVersion(Entregable entregable, MultipartFile evidencia, String storedName, LocalDate fechaEntrega, ActorContext actor) {
        asegurarVersionActual(entregable, actor);

        documentoVersionRepository.findByEntregableIdAndEstado(entregable.getId(), DocumentoVersionEstado.ACTUAL)
                .forEach(actual -> {
                    actual.setEstado(DocumentoVersionEstado.HISTORICA);
                    documentoVersionRepository.save(actual);
                });

        DocumentoVersion version = new DocumentoVersion();
        version.setEntregable(entregable);
        version.setNumeroVersion(documentoVersionRepository.findMaxNumeroVersionByEntregableId(entregable.getId()) + 1);
        version.setNombreArchivoOriginal(nombreOriginalSeguro(evidencia));
        version.setArchivoStorage(storedName);
        version.setMimeType(firstNonBlank(evidencia.getContentType(), "application/pdf"));
        version.setSizeBytes(evidencia.getSize());
        version.setChecksumSha256(sha256(evidencia));
        version.setFechaEntrega(fechaEntrega);
        version.setComentarioCarga(null);
        version.setEstado(DocumentoVersionEstado.ACTUAL);
        version.setSubidoPor(actor.username());
        version.setSubidoRol(actor.role());
        return documentoVersionRepository.save(version);
    }

    private DocumentoVersion asegurarVersionActual(Entregable entregable, ActorContext actor) {
        return versionActual(entregable).orElseGet(() -> {
            if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
                return null;
            }

            DocumentoVersion version = new DocumentoVersion();
            version.setEntregable(entregable);
            version.setNumeroVersion(documentoVersionRepository.findMaxNumeroVersionByEntregableId(entregable.getId()) + 1);
            version.setNombreArchivoOriginal(entregable.getArchivoPdf());
            version.setArchivoStorage(entregable.getArchivoPdf());
            version.setMimeType("application/pdf");
            version.setFechaEntrega(entregable.getFechaEntregaReal());
            version.setComentarioCarga("Version inicial registrada automaticamente desde evidencia existente.");
            version.setEstado(DocumentoVersionEstado.ACTUAL);
            version.setSubidoPor(actor != null ? actor.username() : "sistema");
            version.setSubidoRol(actor != null ? actor.role() : "sistema");
            return documentoVersionRepository.save(version);
        });
    }

    private java.util.Optional<DocumentoVersion> versionActual(Entregable entregable) {
        return documentoVersionRepository.findFirstByEntregableIdAndEstadoOrderByNumeroVersionDesc(
                entregable.getId(), DocumentoVersionEstado.ACTUAL);
    }

    private DocumentoObservacion crearObservacion(Entregable entregable, DocumentoVersion version, String observacionTexto, ActorContext actor) {
        DocumentoObservacion observacion = new DocumentoObservacion();
        observacion.setEntregable(entregable);
        observacion.setVersion(version);
        observacion.setObservacion(observacionTexto);
        observacion.setEstado(DocumentoObservacionEstado.ABIERTA);
        observacion.setCreadaPor(actor.username());
        observacion.setCreadaRol(actor.role());
        return documentoObservacionRepository.save(observacion);
    }

    private void marcarObservacionesSubsanadas(Entregable entregable, ActorContext actor, String comentario) {
        List<DocumentoObservacion> abiertas = documentoObservacionRepository.findByEntregableIdAndEstadoIn(
                entregable.getId(), List.of(DocumentoObservacionEstado.ABIERTA));
        for (DocumentoObservacion observacion : abiertas) {
            observacion.setEstado(DocumentoObservacionEstado.SUBSANADA);
            observacion.setSubsanadaPor(actor.username());
            observacion.setSubsanadaEn(LocalDateTime.now());
            observacion.setComentarioSubsanacion(comentario);
            documentoObservacionRepository.save(observacion);
            auditar(entregable, observacion.getVersion(), observacion, "SUBSANAR", actor, "observacionId=" + observacion.getId());
        }
    }

    private void cerrarObservaciones(Entregable entregable, ActorContext actor) {
        List<DocumentoObservacion> pendientes = documentoObservacionRepository.findByEntregableIdAndEstadoIn(
                entregable.getId(), List.of(DocumentoObservacionEstado.ABIERTA, DocumentoObservacionEstado.SUBSANADA));
        for (DocumentoObservacion observacion : pendientes) {
            observacion.setEstado(DocumentoObservacionEstado.CERRADA);
            observacion.setCerradaPor(actor.username());
            observacion.setCerradaEn(LocalDateTime.now());
            documentoObservacionRepository.save(observacion);
        }
    }

    private void auditar(Entregable entregable, DocumentoVersion version, DocumentoObservacion observacion, String accion, ActorContext actor, String metadata) {
        DocumentoAuditoria auditoria = new DocumentoAuditoria();
        auditoria.setEntregable(entregable);
        auditoria.setVersion(version);
        auditoria.setObservacion(observacion);
        auditoria.setAccion(accion);
        auditoria.setActor(actor.username());
        auditoria.setActorRol(actor.role());
        auditoria.setMetadata(metadata);
        documentoAuditoriaRepository.save(auditoria);
    }

    private Entregable cargarEntregableDelProyecto(String proyectoId, Integer entregableId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado: " + entregableId));

        if (entregable.getHito() == null
                || entregable.getHito().getFase() == null
                || entregable.getHito().getFase().getProyecto() == null
                || !entregable.getHito().getFase().getProyecto().getId().equalsIgnoreCase(proyectoId)) {
            throw new UnprocessableEntityException("El entregable no pertenece al proyecto especificado");
        }
        return entregable;
    }

    private EntregableConformidadResponseDTO responseConAvance(String proyectoId, Entregable entregable) {
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

    private DocumentoVersionDTO toVersionDto(DocumentoVersion version) {
        String proyectoId = version.getEntregable().getHito().getFase().getProyecto().getId();
        Integer entregableId = version.getEntregable().getId();
        String descargaUrl = "/api/v1/proyectos/" + proyectoId + "/avance/entregables/" + entregableId
                + "/versiones/" + version.getId() + "/archivo";

        return new DocumentoVersionDTO(
                version.getId(),
                entregableId,
                version.getNumeroVersion(),
                version.getNombreArchivoOriginal(),
                version.getEstado().name(),
                version.getMimeType(),
                version.getSizeBytes(),
                version.getChecksumSha256(),
                version.getFechaEntrega(),
                version.getSubidoPor(),
                version.getSubidoRol(),
                version.getSubidoEn(),
                version.getComentarioCarga(),
                DocumentoVersionEstado.ACTUAL.equals(version.getEstado()),
                descargaUrl
        );
    }

    private DocumentoObservacionDTO toObservacionDto(DocumentoObservacion observacion) {
        DocumentoVersion version = observacion.getVersion();
        return new DocumentoObservacionDTO(
                observacion.getId(),
                observacion.getEntregable().getId(),
                version != null ? version.getId() : null,
                version != null ? version.getNumeroVersion() : null,
                observacion.getObservacion(),
                observacion.getEstado().name(),
                observacion.getCreadaPor(),
                observacion.getCreadaRol(),
                observacion.getCreadaEn(),
                observacion.getSubsanadaPor(),
                observacion.getSubsanadaEn(),
                observacion.getComentarioSubsanacion(),
                observacion.getCerradaPor(),
                observacion.getCerradaEn()
        );
    }

    private ActorContext actorContext(Authentication authentication) {
        try {
            Usuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
            String username = firstNonBlank(usuario.getCorreo(), usuario.getNombre(), identityExtractor.resolveUsername(authentication), "sistema");
            String role = firstNonBlank(SecurityRoleCatalog.normalize(usuario.getRolCodigo()), "sin_rol");
            return new ActorContext(username, role);
        } catch (RuntimeException ex) {
            return new ActorContext(firstNonBlank(identityExtractor.resolveUsername(authentication), "sistema"), "sin_rol");
        }
    }

    private String nombreOriginalSeguro(MultipartFile evidencia) {
        String original = firstNonBlank(evidencia.getOriginalFilename(), "evidencia.pdf");
        return storageProvider.sanitizeFileName(original);
    }

    private String sha256(MultipartFile evidencia) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(evidencia.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (IOException ex) {
            throw new UnprocessableEntityException("No fue posible calcular la huella del documento cargado.");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible en el entorno de ejecucion.", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Proyecto cargarProyecto(String proyectoId) {
        return proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
    }

    private record ActorContext(String username, String role) {}
}
