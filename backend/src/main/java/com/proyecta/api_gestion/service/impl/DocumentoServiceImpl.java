package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.*;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.exception.UnauthorizedException;
import com.proyecta.api_gestion.domain.model.Documento;
import com.proyecta.api_gestion.domain.model.DocumentoPreWizardRevision;
import com.proyecta.api_gestion.domain.model.DocumentoProyectoVersion;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.domain.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.domain.model.enums.DocumentoPreWizardEstado;
import com.proyecta.api_gestion.domain.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.domain.model.enums.TipoDocumento;
import com.proyecta.api_gestion.domain.model.enums.ViabilidadEstado;
import com.proyecta.api_gestion.application.port.out.persistence.*;
import com.proyecta.api_gestion.application.port.out.persistence.config.TipoDocumentoConfigRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentoServiceImpl implements IDocumentoService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final String STORAGE_SUBDIR = "documentos";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TIPO_VIABILIZACION = "VIABILIZACION";
    private static final String TIPO_PLAN_COMUNICACIONES = "PLAN_COMUNICACIONES";
    private static final String TIPO_MATRIZ_RIESGOS_VIABILIDAD = "MATRIZ_RIESGOS_VIABILIDAD";
    private static final String ESTADO_DEVUELTO = "DEVUELTO";
    private static final List<String> PRE_WIZARD_TYPES = List.of(
            TIPO_VIABILIZACION, TIPO_PLAN_COMUNICACIONES, TIPO_MATRIZ_RIESGOS_VIABILIDAD);

    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final DocumentoProyectoVersionRepositoryPort versionRepositoryPort;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final TipoDocumentoConfigRepositoryPort tipoDocumentoConfigRepositoryPort;
    private final DocumentoPreWizardRevisionRepositoryPort preWizardRevisionRepositoryPort;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final IStorageProvider storageProvider;

    public DocumentoServiceImpl(
            DocumentoRepositoryPort documentoRepositoryPort,
            DocumentoProyectoVersionRepositoryPort versionRepositoryPort,
            ProyectoRepositoryPort proyectoRepositoryPort,
            TipoDocumentoConfigRepositoryPort tipoDocumentoConfigRepositoryPort,
            DocumentoPreWizardRevisionRepositoryPort preWizardRevisionRepositoryPort,
            NotificationEventPublisherPort notificationPublisher,
            KeycloakIdentityExtractor identityExtractor,
            LocalUserAuthorizationService localUserAuthorizationService,
            IStorageProvider storageProvider) {
        this.documentoRepositoryPort = documentoRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.tipoDocumentoConfigRepositoryPort = tipoDocumentoConfigRepositoryPort;
        this.preWizardRevisionRepositoryPort = preWizardRevisionRepositoryPort;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.storageProvider = storageProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoListadoResponseDTO listarDocumentos(String proyectoId) {
        validarExistenciaProyecto(proyectoId);

        List<Documento> documentos = documentoRepositoryPort.findByProyectoIdOrderByFechaCargaDesc(proyectoId);
        Map<String, DocumentoProyectoVersion> versionesActuales = versionRepositoryPort
                .findByProyectoIdAndEstado(proyectoId, DocumentoProyectoVersionEstado.ACTUAL)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoProyectoVersion::getTipoDocumento,
                        Function.identity(),
                        (actual, duplicada) -> actual.getSubidoEn().isAfter(duplicada.getSubidoEn()) ? actual : duplicada
                ));
        List<DocumentoDetailDTO> detalles = documentos.stream()
                .map(doc -> toDetailDTO(doc, versionesActuales.get(doc.getTipoDocumentoCodigo())))
                .toList();

        return new DocumentoListadoResponseDTO(proyectoId, detalles);
    }

    @Override
    @Transactional
    public DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo, String observacion, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio.");
        }
        tipoDocumento = tipoDocumento.trim().toUpperCase();
        storageProvider.validateFile(archivo, MAX_FILE_SIZE, ALLOWED_MIME_TYPES);
        ActorContext actor = actorContext(authentication);

        DocumentoProyectoVersion versionActual = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipoDocumento, DocumentoProyectoVersionEstado.ACTUAL)
                .orElse(null);
        boolean esReemplazo = versionActual != null;

        if (esReemplazo) {
            if (observacion == null || observacion.isBlank()) {
                throw new BadRequestException("La observacion es obligatoria al reemplazar un documento.");
            }
            observacion = observacion.trim();
            versionActual.setEstado(DocumentoProyectoVersionEstado.HISTORICA);
            versionRepositoryPort.save(versionActual);
        }

        Integer maxVersion = versionRepositoryPort.findMaxNumeroVersion(proyectoId, tipoDocumento);
        Integer nuevaVersion = maxVersion + 1;

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String nombreUnico = generarNombreUnico(tipoDocumento);
        String mimeType = resolverMimeType(archivo);

        String nombreAlmacenado = storageProvider.storeFile(archivo, STORAGE_SUBDIR, nombreUnico);

        DocumentoProyectoVersion version = new DocumentoProyectoVersion();
        version.setProyectoId(proyectoId);
        version.setTipoDocumento(tipoDocumento);
        version.setNumeroVersion(nuevaVersion);
        version.setNombreArchivoOriginal(nombreOriginal);
        version.setNombreAlmacenado(nombreAlmacenado);
        version.setRutaAlmacenamiento(STORAGE_SUBDIR);
        version.setMimeType(mimeType);
        version.setTamanoBytes(archivo.getSize());
        version.setObservacion(esReemplazo ? observacion : null);
        version.setEstado(DocumentoProyectoVersionEstado.ACTUAL);
        version.setSubidoPor(actor.username());
        version.setSubidoRol(actor.role());

        DocumentoProyectoVersion guardada = versionRepositoryPort.save(version);

        sincronizarDocumentoPrincipal(proyectoId, tipoDocumento, guardada);

        boolean esPreWizard = PRE_WIZARD_TYPES.contains(tipoDocumento);
        if (!esPreWizard) {
            Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId).orElse(null);
            boolean enAsistenteInicial = proyecto != null && proyecto.requiereCompletitudDirector();
            if (!enAsistenteInicial) {
                notificarCambioDocumento(proyectoId, actor.username(), NotificationEventType.PROJECT_DOCUMENT_UPLOADED, tipoDocumento, nombreOriginal, esReemplazo);
            }
        }

        sincronizarFlagsProyectoDocumentos(proyectoId, tipoDocumento, guardada, actor.username());

        return toUploadResultDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarDocumento(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        DocumentoProyectoVersion version = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipoDocumento, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado: " + tipoDocumento + " para proyecto " + proyectoId));

        return storageProvider.loadFileAsResource(version.getRutaAlmacenamiento(), version.getNombreAlmacenado());
    }

    @Override
    @Transactional
    public void eliminarDocumento(String proyectoId, String tipoDocumento, Authentication authentication) {
        throw new UnsupportedOperationException(
                "La eliminacion de documentos no esta habilitada. Use el reemplazo con observacion para crear nuevas versiones.");
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoVersionHistorialResponseDTO listarVersiones(String proyectoId, String tipoDocumento) {
        validarExistenciaProyecto(proyectoId);

        List<DocumentoProyectoVersion> versiones = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyectoId, tipoDocumento);

        List<DocumentoProyectoVersionDTO> dtos = versiones.stream()
                .map(this::toVersionDTO)
                .toList();

        return new DocumentoVersionHistorialResponseDTO(proyectoId, tipoDocumento, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarVersion(String proyectoId, String tipoDocumento, Integer numeroVersion) {
        validarExistenciaProyecto(proyectoId);

        DocumentoProyectoVersion version = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndNumeroVersion(proyectoId, tipoDocumento, numeroVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + numeroVersion + " del documento " + tipoDocumento + " no encontrada."));

        return storageProvider.loadFileAsResource(version.getRutaAlmacenamiento(), version.getNombreAlmacenado());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoPreWizardRevisionDTO.Listado listarRevisionesPreWizard(String proyectoId) {
        validarExistenciaProyecto(proyectoId);

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepositoryPort
                .findByProyectoId(proyectoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        Function.identity(),
                        (a, b) -> a
                ));

        List<DocumentoPreWizardRevisionDTO> documentos = new ArrayList<>();
        for (String tipo : PRE_WIZARD_TYPES) {
            DocumentoPreWizardRevision revision = porTipo.get(tipo);
            if (revision != null) {
                documentos.add(toRevisionDTO(revision));
            } else {
                documentos.add(new DocumentoPreWizardRevisionDTO(
                        tipo,
                        DocumentoPreWizardEstado.PENDIENTE.name(),
                        null,
                        null,
                        null
                ));
            }
        }
        return new DocumentoPreWizardRevisionDTO.Listado(proyectoId, documentos);
    }

    @Override
    @Transactional
    public DocumentoPreWizardRevisionDTO aprobarDocumentoPreWizard(String proyectoId, String tipoDocumento, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        String tipo = normalizarTipoPreWizard(tipoDocumento);
        ActorContext actor = actorContext(authentication);

        DocumentoProyectoVersion version = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipo, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new BadRequestException(
                        "No hay un documento cargado del tipo " + tipo + " para aprobar."));

        DocumentoPreWizardRevision revision = preWizardRevisionRepositoryPort
                .findByProyectoIdAndTipoDocumento(proyectoId, tipo)
                .orElseGet(() -> nuevaRevision(proyectoId, tipo, version));

        revision.setEstado(DocumentoPreWizardEstado.APROBADO);
        revision.setObservacion(null);
        revision.setRevisadoPor(actor.username());
        revision.setRevisadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        DocumentoPreWizardRevision guardada = preWizardRevisionRepositoryPort.save(revision);

        sincronizarEstadoViabilidadAggregate(proyectoId, actor.username());

        return toRevisionDTO(guardada);
    }

    @Override
    @Transactional
    public DocumentoPreWizardRevisionDTO devolverDocumentoPreWizard(String proyectoId, String tipoDocumento, String observaciones, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        String tipo = normalizarTipoPreWizard(tipoDocumento);
        if (observaciones == null || observaciones.isBlank()) {
            throw new BadRequestException("Las observaciones son obligatorias al devolver un documento.");
        }
        ActorContext actor = actorContext(authentication);

        DocumentoProyectoVersion version = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipo, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new BadRequestException(
                        "No hay un documento cargado del tipo " + tipo + " para devolver."));

        DocumentoPreWizardRevision revision = preWizardRevisionRepositoryPort
                .findByProyectoIdAndTipoDocumento(proyectoId, tipo)
                .orElseGet(() -> nuevaRevision(proyectoId, tipo, version));

        revision.setEstado(DocumentoPreWizardEstado.DEVUELTO);
        revision.setObservacion(observaciones.trim());
        revision.setRevisadoPor(actor.username());
        revision.setRevisadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        DocumentoPreWizardRevision guardada = preWizardRevisionRepositoryPort.save(revision);

        sincronizarEstadoViabilidadAggregate(proyectoId, actor.username());

        return toRevisionDTO(guardada);
    }

    private void marcarRevisionPendiente(String proyectoId, String tipoDocumento) {
        DocumentoPreWizardRevision revision = preWizardRevisionRepositoryPort
                .findByProyectoIdAndTipoDocumento(proyectoId, tipoDocumento)
                .orElseGet(() -> {
                    DocumentoPreWizardRevision nueva = new DocumentoPreWizardRevision();
                    nueva.setProyectoId(proyectoId);
                    nueva.setTipoDocumento(tipoDocumento);
                    return nueva;
                });
        revision.setEstado(DocumentoPreWizardEstado.PENDIENTE);
        revision.setObservacion(null);
        revision.setRevisadoPor(null);
        revision.setRevisadoEn(null);
        preWizardRevisionRepositoryPort.save(revision);
    }

    private DocumentoPreWizardRevision nuevaRevision(String proyectoId, String tipoDocumento, DocumentoProyectoVersion version) {
        DocumentoPreWizardRevision revision = new DocumentoPreWizardRevision();
        revision.setProyectoId(proyectoId);
        revision.setTipoDocumento(tipoDocumento);
        revision.setEstado(DocumentoPreWizardEstado.PENDIENTE);
        revision.setRevisadoPor(version.getSubidoPor());
        return revision;
    }

    private void sincronizarEstadoViabilidadAggregate(String proyectoId, String actorUsername) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }

        verificarTodosDocumentosCargados(proyecto);

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepositoryPort
                .findByProyectoId(proyectoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        Function.identity(),
                        (a, b) -> a
                ));

        boolean hayDevuelto = porTipo.values().stream()
                .anyMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
        boolean todosAprobado = PRE_WIZARD_TYPES.stream()
                .allMatch(tipo -> {
                    DocumentoPreWizardRevision r = porTipo.get(tipo);
                    return r != null && r.getEstado() == DocumentoPreWizardEstado.APROBADO;
                });
        boolean cargados = Boolean.TRUE.equals(proyecto.getDocumentosCargados());
        boolean eraDevuelta = proyecto.viabilidadDevuelta();

        if (todosAprobado && cargados) {
            // Aprobacion consolidada solo al confirmar (boton final del gestor).
            aplicarAprobacionConsolidada(proyecto, eraDevuelta);
            proyectoRepositoryPort.save(proyecto);
            return;
        }

        if (hayDevuelto) {
            aplicarDevolucion(proyecto, porTipo, actorUsername, eraDevuelta);
            return;
        }

        if (cargados) {
            aplicarCargaParcial(proyecto);
            return;
        }

        aplicarPendiente(proyecto);
    }

    private void aplicarAprobacionConsolidada(Proyecto proyecto, boolean eraDevuelta) {
        if (eraDevuelta || Boolean.TRUE.equals(proyecto.getDocumentosVerificados())) {
            proyecto.setViabilidadEstado(ViabilidadEstado.CARGADA);
            proyecto.setViabilidadObservaciones(null);
            proyecto.setViabilidadRevisadoPor(null);
            proyecto.setViabilidadRevisadoEn(null);
            proyecto.setDocumentosVerificados(false);
            proyecto.setFechaVerificacionDocumentos(null);
            proyecto.setFechaLimiteCompletar(null);
        } else if (proyecto.getViabilidadEstado() != ViabilidadEstado.CARGADA) {
            proyecto.setViabilidadEstado(ViabilidadEstado.CARGADA);
        }
    }

    private void aplicarDevolucion(Proyecto proyecto, Map<String, DocumentoPreWizardRevision> porTipo, String actorUsername, boolean eraDevuelta) {
        String observaciones = porTipo.values().stream()
                .filter(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO && r.getObservacion() != null)
                .map(DocumentoPreWizardRevision::getObservacion)
                .collect(Collectors.joining("\n"));
        String observacionesNormalizadas = observaciones.isBlank() ? null : observaciones;
        if (!eraDevuelta || !Boolean.TRUE.equals(proyecto.getDocumentosVerificados())) {
            proyecto.setViabilidadEstado(ViabilidadEstado.DEVUELTA);
            proyecto.setViabilidadObservaciones(observacionesNormalizadas);
            proyecto.setViabilidadRevisadoPor(actorUsername);
            proyecto.setViabilidadRevisadoEn(LocalDateTime.now(ZoneId.systemDefault()));
            proyecto.setDocumentosVerificados(false);
            proyecto.setFechaVerificacionDocumentos(null);
            proyecto.setFechaLimiteCompletar(null);
            proyectoRepositoryPort.save(proyecto);
        } else {
            proyecto.setViabilidadObservaciones(observacionesNormalizadas);
            proyectoRepositoryPort.save(proyecto);
        }
    }

    private void aplicarCargaParcial(Proyecto proyecto) {
        if (proyecto.getViabilidadEstado() != ViabilidadEstado.CARGADA) {
            proyecto.marcarViabilidadCargada();
        }
        proyecto.setDocumentosVerificados(false);
        proyecto.setFechaVerificacionDocumentos(null);
        proyecto.setFechaLimiteCompletar(null);
        proyectoRepositoryPort.save(proyecto);
    }

    private void aplicarPendiente(Proyecto proyecto) {
        if (proyecto.getViabilidadEstado() != ViabilidadEstado.PENDIENTE) {
            proyecto.setViabilidadEstado(ViabilidadEstado.PENDIENTE);
            proyecto.setViabilidadObservaciones(null);
        }
        proyectoRepositoryPort.save(proyecto);
    }

    private void notificarViabilidad(Proyecto proyecto, String actorUsername, NotificationEventType eventType, String observaciones) {
        publicarNotificacionProyecto(proyecto, actorUsername, eventType, payload -> {
            payload.put("documentStatuses", buildDocumentStatusSummary(proyecto.getId()));
            if (observaciones != null && !observaciones.isBlank()) {
                payload.put("observaciones", observaciones);
            }
        });
    }

    @Override
    @Transactional
    public DocumentoPreWizardConfirmacionDTO confirmarRevisionPreWizard(String proyectoId, Authentication authentication) {
        validarExistenciaProyecto(proyectoId);
        ActorContext actor = actorContext(authentication);

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepositoryPort
                .findByProyectoId(proyectoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        Function.identity(),
                        (a, b) -> a
                ));

        for (String tipo : PRE_WIZARD_TYPES) {
            DocumentoPreWizardRevision r = porTipo.get(tipo);
            boolean decidido = r != null
                    && (r.getEstado() == DocumentoPreWizardEstado.APROBADO
                        || r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
            if (!decidido) {
                throw new BadRequestException(
                        "Complete la revision de los 3 documentos antes de confirmar. Falta decidir: "
                                + nombreDocumentoPreWizard(tipo));
            }
        }

        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));

        boolean hayDevuelto = porTipo.values().stream()
                .anyMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);

        if (hayDevuelto) {
            String observaciones = porTipo.values().stream()
                    .filter(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO && r.getObservacion() != null)
                    .map(DocumentoPreWizardRevision::getObservacion)
                    .filter(obs -> !obs.isBlank())
                    .collect(Collectors.joining("\n"));
            notificarViabilidad(proyecto, actor.username(), NotificationEventType.VIABILIDAD_RETURNED, observaciones);
            return new DocumentoPreWizardConfirmacionDTO(
                    NotificationEventType.VIABILIDAD_RETURNED.name(),
                    "Observaciones consolidadas enviadas al Director y Gestor.",
                    buildDocumentStatusSummary(proyectoId));
        }

        proyecto.aprobarDocumentos(actor.username());
        proyectoRepositoryPort.save(proyecto);
        notificarViabilidad(proyecto, actor.username(), NotificationEventType.VIABILIDAD_APPROVED, null);
        return new DocumentoPreWizardConfirmacionDTO(
                NotificationEventType.VIABILIDAD_APPROVED.name(),
                "Verificacion consolidada enviada al Director y Gestor.",
                buildDocumentStatusSummary(proyectoId));
    }

    private void publicarNotificacionProyecto(
            Proyecto proyecto,
            String actorUsername,
            NotificationEventType eventType,
            java.util.function.Consumer<Map<String, Object>> extras) {
        var recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectName", proyecto.getNombre());
        payload.put("recipients", recipients);
        extras.accept(payload);
        notificationPublisher.publish(new NotificationContext(eventType, proyecto.getId(), actorUsername, payload));
    }

    private String normalizarTipoPreWizard(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio.");
        }
        String tipo = tipoDocumento.trim().toUpperCase();
        if (!PRE_WIZARD_TYPES.contains(tipo)) {
            throw new BadRequestException("Tipo de documento no valido para revision pre-wizard: " + tipoDocumento);
        }
        return tipo;
    }

    private DocumentoPreWizardRevisionDTO toRevisionDTO(DocumentoPreWizardRevision revision) {
        return new DocumentoPreWizardRevisionDTO(
                revision.getTipoDocumento(),
                revision.getEstado().name(),
                revision.getObservacion(),
                revision.getRevisadoPor(),
                revision.getRevisadoEn()
        );
    }

    private void sincronizarDocumentoPrincipal(String proyectoId, String tipoDocumento, DocumentoProyectoVersion version) {
        TipoDocumento tipoDocumentoEnum = resolverTipoDocumento(tipoDocumento);
        TipoDocumentoConfig tipoDocumentoConfig = tipoDocumentoConfigRepositoryPort.findByCodigo(tipoDocumento)
                .orElse(null);

        Documento documentoExistente = documentoRepositoryPort
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElse(null);

        if (documentoExistente != null) {
            // No podemos borrar el archivo físico porque la version anterior (histórica) en `DocumentoProyectoVersion`
            // lo sigue referenciando y es necesario para que funcione el visor de historial.
            documentoExistente.setNombreOriginal(version.getNombreArchivoOriginal());
            documentoExistente.setNombreAlmacenado(version.getNombreAlmacenado());
            documentoExistente.setRutaAlmacenamiento(version.getRutaAlmacenamiento());
            documentoExistente.setMimeType(version.getMimeType());
            documentoExistente.setTamanoBytes(version.getTamanoBytes());
            documentoExistente.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));
            documentoExistente.setFechaCarga(version.getSubidoEn());
            documentoRepositoryPort.save(documentoExistente);
        } else {
            Documento documento = new Documento();
            documento.setProyectoId(proyectoId);
            documento.setTipoDocumento(tipoDocumentoEnum);
            documento.setTipoDocumentoConfig(tipoDocumentoConfig);
            documento.setNombreOriginal(version.getNombreArchivoOriginal());
            documento.setNombreAlmacenado(version.getNombreAlmacenado());
            documento.setRutaAlmacenamiento(version.getRutaAlmacenamiento());
            documento.setMimeType(version.getMimeType());
            documento.setTamanoBytes(version.getTamanoBytes());
            documento.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));
            documento.setFechaCarga(version.getSubidoEn());
            documentoRepositoryPort.save(documento);
        }
    }

    private void validarExistenciaProyecto(String proyectoId) {
        proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));
    }

    private String generarNombreUnico(String tipoDocumento) {
        return tipoDocumento.toLowerCase() + "_" + UUID.randomUUID().toString();
    }

    private String resolverMimeType(MultipartFile file) {
        // CWE-434: el Content-Type lo declara el cliente; se deriva de la extension.
        return com.proyecta.api_gestion.infrastructure.UploadMimeSanitizer.resolverMimeType(file);
    }

    private String construirUrlDescarga(String proyectoId, String tipoDocumento) {
        return "/api/v1/proyectos/" + proyectoId + "/documentos/" + tipoDocumento + "/descargar";
    }

    private TipoDocumento resolverTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio.");
        }
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new BadRequestException("Tipo de documento no válido: " + tipoDocumento);
        }
    }

    private String formatearTamano(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private DocumentoDetailDTO toDetailDTO(Documento doc, DocumentoProyectoVersion version) {
        return new DocumentoDetailDTO(
                doc.getId(),
                doc.getTipoDocumentoCodigo(),
                doc.getNombreOriginal(),
                doc.getMimeType(),
                doc.getTamanoBytes(),
                formatearTamano(doc.getTamanoBytes()),
                doc.getUrlDescarga(),
                doc.getFechaCarga().format(DATE_FORMATTER),
                version != null ? version.getSubidoPor() : null,
                version != null ? version.getSubidoRol() : null
        );
    }

    private DocumentoUploadResultDTO toUploadResultDTO(DocumentoProyectoVersion version) {
        String urlDescarga = construirUrlDescarga(version.getProyectoId(), version.getTipoDocumento());
        return new DocumentoUploadResultDTO(
                version.getId(),
                version.getTipoDocumento(),
                version.getNombreArchivoOriginal(),
                version.getNombreAlmacenado(),
                version.getMimeType(),
                version.getTamanoBytes(),
                formatearTamano(version.getTamanoBytes()),
                urlDescarga,
                version.getSubidoEn().format(DATE_FORMATTER),
                version.getSubidoPor(),
                version.getSubidoRol()
        );
    }

    private DocumentoProyectoVersionDTO toVersionDTO(DocumentoProyectoVersion v) {
        return new DocumentoProyectoVersionDTO(
                v.getId(),
                v.getTipoDocumento(),
                v.getNumeroVersion(),
                v.getNombreArchivoOriginal(),
                v.getMimeType(),
                v.getTamanoBytes(),
                formatearTamano(v.getTamanoBytes()),
                v.getObservacion(),
                v.getEstado().name(),
                v.getSubidoPor(),
                v.getSubidoRol(),
                v.getSubidoEn().format(DATE_FORMATTER),
                v.getEstado() == DocumentoProyectoVersionEstado.ACTUAL
        );
    }

    private void notificarCambioDocumento(String proyectoId, String actorUsername, NotificationEventType eventType, String tipoDocumento, String nombreDocumento, boolean reemplazo) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }
        publicarNotificacionProyecto(proyecto, actorUsername, eventType, payload -> {
            payload.put("documentType", tipoDocumento);
            payload.put("documentName", nombreDocumento);
            payload.put("isReplacement", reemplazo);
        });
    }

    private void sincronizarFlagsProyectoDocumentos(String proyectoId, String tipoDocumento, DocumentoProyectoVersion version, String actorUsername) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId).orElse(null);
        if (proyecto == null) return;

        switch (tipoDocumento) {
            case TIPO_VIABILIZACION -> proyecto.setViabilizacionPdf(version.getNombreAlmacenado());
            case "CRONOGRAMA" -> proyecto.setCronogramaPdf(version.getNombreAlmacenado());
            case "ACTA_CONSTITUCION" -> {
                proyecto.setActaConstitucionPdf(version.getNombreAlmacenado());
                proyecto.marcarActaConstitucionCargada();
            }
            case TIPO_PLAN_COMUNICACIONES -> {
                proyecto.setPlanComunicacionesPdf(version.getNombreAlmacenado());
                proyecto.setTienePlanComunicaciones(true);
            }
            default -> {
                // Tipos sin flag propio: no requieren actualización adicional.
            }
        }

        boolean esPreWizard = PRE_WIZARD_TYPES.contains(tipoDocumento);
        boolean habiaDevueltos = false;
        if (esPreWizard) {
            habiaDevueltos = preWizardRevisionRepositoryPort.findByProyectoId(proyectoId).stream()
                    .anyMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
            marcarRevisionPendiente(proyectoId, tipoDocumento);
        }

        boolean eraDevuelta = proyecto.viabilidadDevuelta();
        if (esPreWizard && (proyecto.viabilidadPendiente() || eraDevuelta)) {
            proyecto.marcarViabilidadCargada();
        }

        boolean documentosCompletosAntes = Boolean.TRUE.equals(proyecto.getDocumentosCargados());
        verificarTodosDocumentosCargados(proyecto);
        proyectoRepositoryPort.save(proyecto);

        boolean ahoraCompletos = Boolean.TRUE.equals(proyecto.getDocumentosCargados());
        boolean recienCompletado = !documentosCompletosAntes && ahoraCompletos;
        boolean resubmissionCompleta = false;
        if (esPreWizard && habiaDevueltos) {
            resubmissionCompleta = preWizardRevisionRepositoryPort.findByProyectoId(proyectoId).stream()
                    .noneMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
        }
        if (esPreWizard && (recienCompletado || resubmissionCompleta)) {
            notificarCargaDocumentos(proyectoId, actorUsername, proyecto);
        }
    }

    private void verificarTodosDocumentosCargados(Proyecto proyecto) {
        boolean tieneViabilidad = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), TIPO_VIABILIZACION)
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());
        boolean tienePlanComunicaciones = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), TIPO_PLAN_COMUNICACIONES)
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());
        boolean tieneMatrizRiesgos = versionRepositoryPort
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), TIPO_MATRIZ_RIESGOS_VIABILIDAD)
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());

        proyecto.setDocumentosCargados(tieneViabilidad && tienePlanComunicaciones && tieneMatrizRiesgos);
    }

    private void notificarCargaDocumentos(String proyectoId, String directorUsername, Proyecto proyecto) {
        publicarNotificacionProyecto(proyecto, directorUsername, NotificationEventType.VIABILIDAD_UPLOADED, payload ->
                payload.put("documentStatuses", buildDocumentStatusSummary(proyectoId)));
    }

    private String buildDocumentStatusSummary(String proyectoId) {
        Map<String, DocumentoPreWizardRevision> revisiones = preWizardRevisionRepositoryPort
                .findByProyectoId(proyectoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        Function.identity(),
                        (a, b) -> a
                ));

        StringBuilder sb = new StringBuilder();
        for (String tipo : PRE_WIZARD_TYPES) {
            boolean cargado = versionRepositoryPort
                    .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyectoId, tipo)
                    .stream()
                    .anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());
            DocumentoPreWizardRevision rev = revisiones.get(tipo);
            String estadoRevision = rev == null ? null : rev.getEstado().name();
            String estado;
            if (!cargado) {
                estado = "PENDIENTE (sin cargar)";
            } else if (estadoRevision == null || "PENDIENTE".equals(estadoRevision)) {
                estado = "CARGADO (pendiente de revision)";
            } else if ("APROBADO".equals(estadoRevision)) {
                estado = "VERIFICADO";
            } else if (ESTADO_DEVUELTO.equals(estadoRevision)) {
                estado = ESTADO_DEVUELTO;
            } else {
                estado = estadoRevision;
            }
            sb.append("- ").append(nombreDocumentoPreWizard(tipo)).append(": ").append(estado);
            if (ESTADO_DEVUELTO.equals(estadoRevision) && rev.getObservacion() != null && !rev.getObservacion().isBlank()) {
                sb.append(" — ").append(rev.getObservacion().trim());
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String nombreDocumentoPreWizard(String tipo) {
        return switch (tipo) {
            case TIPO_VIABILIZACION -> "Documento de Viabilidad";
            case TIPO_PLAN_COMUNICACIONES -> "Plan de Comunicaciones";
            case TIPO_MATRIZ_RIESGOS_VIABILIDAD -> "Matriz de Riesgos de Viabilidad";
            default -> tipo;
        };
    }

    private ActorContext actorContext(Authentication authentication) {
        SeguridadUsuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
        String username = firstNonBlank(usuario.getNombre(), usuario.getCorreo(), identityExtractor.resolveUsername(authentication));
        if (username == null) {
            throw new UnauthorizedException("No fue posible identificar el usuario que carga el documento.");
        }
        String role = firstNonBlank(SecurityRoleCatalog.normalize(usuario.getRolCodigo()), "sin_rol");
        return new ActorContext(username, role);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record ActorContext(String username, String role) {}
}
