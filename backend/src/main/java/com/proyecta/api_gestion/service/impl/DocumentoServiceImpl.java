package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.document.*;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnauthorizedException;
import com.proyecta.api_gestion.model.Documento;
import com.proyecta.api_gestion.model.DocumentoPreWizardRevision;
import com.proyecta.api_gestion.model.DocumentoProyectoVersion;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.model.enums.DocumentoPreWizardEstado;
import com.proyecta.api_gestion.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import com.proyecta.api_gestion.model.enums.ViabilidadEstado;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.config.TipoDocumentoConfigRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
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
    private static final List<String> PRE_WIZARD_TYPES = List.of(
            "VIABILIZACION", "PLAN_COMUNICACIONES", "MATRIZ_RIESGOS_VIABILIDAD");

    private final DocumentoRepository documentoRepository;
    private final DocumentoProyectoVersionRepository versionRepository;
    private final ProyectoRepository proyectoRepository;
    private final DocumentoDinamicoRepository documentoDinamicoRepository;
    private final TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final DocumentoPreWizardRevisionRepository preWizardRevisionRepository;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final IStorageProvider storageProvider;

    public DocumentoServiceImpl(
            DocumentoRepository documentoRepository,
            DocumentoProyectoVersionRepository versionRepository,
            ProyectoRepository proyectoRepository,
            DocumentoDinamicoRepository documentoDinamicoRepository,
            TipoDocumentoConfigRepository tipoDocumentoConfigRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
            DocumentoPreWizardRevisionRepository preWizardRevisionRepository,
            NotificationEventPublisherPort notificationPublisher,
            KeycloakIdentityExtractor identityExtractor,
            LocalUserAuthorizationService localUserAuthorizationService,
            IStorageProvider storageProvider) {
        this.documentoRepository = documentoRepository;
        this.versionRepository = versionRepository;
        this.proyectoRepository = proyectoRepository;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.tipoDocumentoConfigRepository = tipoDocumentoConfigRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.preWizardRevisionRepository = preWizardRevisionRepository;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.storageProvider = storageProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoListadoResponseDTO listarDocumentos(String proyectoId) {
        validarExistenciaProyecto(proyectoId);

        List<Documento> documentos = documentoRepository.findByProyectoIdOrderByFechaCargaDesc(proyectoId);
        Map<String, DocumentoProyectoVersion> versionesActuales = versionRepository
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

        DocumentoProyectoVersion versionActual = versionRepository
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipoDocumento, DocumentoProyectoVersionEstado.ACTUAL)
                .orElse(null);
        boolean esReemplazo = versionActual != null;

        if (esReemplazo) {
            if (observacion == null || observacion.isBlank()) {
                throw new BadRequestException("La observacion es obligatoria al reemplazar un documento.");
            }
            observacion = observacion.trim();
            versionActual.setEstado(DocumentoProyectoVersionEstado.HISTORICA);
            versionRepository.save(versionActual);
        }

        Integer maxVersion = versionRepository.findMaxNumeroVersion(proyectoId, tipoDocumento);
        Integer nuevaVersion = maxVersion + 1;

        String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = generarNombreUnico(tipoDocumento, extension);
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

        DocumentoProyectoVersion guardada = versionRepository.save(version);

        sincronizarDocumentoPrincipal(proyectoId, tipoDocumento, guardada);

        boolean esPreWizard = PRE_WIZARD_TYPES.contains(tipoDocumento);
        if (!esPreWizard) {
            Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
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

        DocumentoProyectoVersion version = versionRepository
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

        List<DocumentoProyectoVersion> versiones = versionRepository
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

        DocumentoProyectoVersion version = versionRepository
                .findByProyectoIdAndTipoDocumentoAndNumeroVersion(proyectoId, tipoDocumento, numeroVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + numeroVersion + " del documento " + tipoDocumento + " no encontrada."));

        return storageProvider.loadFileAsResource(version.getRutaAlmacenamiento(), version.getNombreAlmacenado());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoPreWizardRevisionDTO.Listado listarRevisionesPreWizard(String proyectoId) {
        validarExistenciaProyecto(proyectoId);

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepository
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

        DocumentoProyectoVersion version = versionRepository
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipo, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new BadRequestException(
                        "No hay un documento cargado del tipo " + tipo + " para aprobar."));

        DocumentoPreWizardRevision revision = preWizardRevisionRepository
                .findByProyectoIdAndTipoDocumento(proyectoId, tipo)
                .orElseGet(() -> nuevaRevision(proyectoId, tipo, version));

        revision.setEstado(DocumentoPreWizardEstado.APROBADO);
        revision.setObservacion(null);
        revision.setRevisadoPor(actor.username());
        revision.setRevisadoEn(LocalDateTime.now());
        DocumentoPreWizardRevision guardada = preWizardRevisionRepository.save(revision);

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

        DocumentoProyectoVersion version = versionRepository
                .findByProyectoIdAndTipoDocumentoAndEstado(proyectoId, tipo, DocumentoProyectoVersionEstado.ACTUAL)
                .orElseThrow(() -> new BadRequestException(
                        "No hay un documento cargado del tipo " + tipo + " para devolver."));

        DocumentoPreWizardRevision revision = preWizardRevisionRepository
                .findByProyectoIdAndTipoDocumento(proyectoId, tipo)
                .orElseGet(() -> nuevaRevision(proyectoId, tipo, version));

        revision.setEstado(DocumentoPreWizardEstado.DEVUELTO);
        revision.setObservacion(observaciones.trim());
        revision.setRevisadoPor(actor.username());
        revision.setRevisadoEn(LocalDateTime.now());
        DocumentoPreWizardRevision guardada = preWizardRevisionRepository.save(revision);

        sincronizarEstadoViabilidadAggregate(proyectoId, actor.username());

        return toRevisionDTO(guardada);
    }

    private void marcarRevisionPendiente(String proyectoId, String tipoDocumento) {
        DocumentoPreWizardRevision revision = preWizardRevisionRepository
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
        preWizardRevisionRepository.save(revision);
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
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }

        verificarTodosDocumentosCargados(proyecto);

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepository
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
            proyectoRepository.save(proyecto);
            return;
        }

        if (hayDevuelto) {
            String observaciones = porTipo.values().stream()
                    .filter(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO && r.getObservacion() != null)
                    .map(DocumentoPreWizardRevision::getObservacion)
                    .collect(Collectors.joining("\n"));
            if (!eraDevuelta || !Boolean.TRUE.equals(proyecto.getDocumentosVerificados())) {
                proyecto.setViabilidadEstado(ViabilidadEstado.DEVUELTA);
                proyecto.setViabilidadObservaciones(observaciones.isBlank() ? null : observaciones);
                proyecto.setViabilidadRevisadoPor(actorUsername);
                proyecto.setViabilidadRevisadoEn(LocalDateTime.now());
                proyecto.setDocumentosVerificados(false);
                proyecto.setFechaVerificacionDocumentos(null);
                proyecto.setFechaLimiteCompletar(null);
                proyectoRepository.save(proyecto);
            } else {
                proyecto.setViabilidadObservaciones(observaciones.isBlank() ? null : observaciones);
                proyectoRepository.save(proyecto);
            }
            return;
        }

        if (cargados) {
            if (proyecto.getViabilidadEstado() != ViabilidadEstado.CARGADA) {
                proyecto.marcarViabilidadCargada();
            }
            proyecto.setDocumentosVerificados(false);
            proyecto.setFechaVerificacionDocumentos(null);
            proyecto.setFechaLimiteCompletar(null);
            proyectoRepository.save(proyecto);
            return;
        }

        if (proyecto.getViabilidadEstado() != ViabilidadEstado.PENDIENTE) {
            proyecto.setViabilidadEstado(ViabilidadEstado.PENDIENTE);
            proyecto.setViabilidadObservaciones(null);
        }
        proyectoRepository.save(proyecto);
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

        Map<String, DocumentoPreWizardRevision> porTipo = preWizardRevisionRepository
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

        Proyecto proyecto = proyectoRepository.findById(proyectoId)
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
        proyectoRepository.save(proyecto);
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
        TipoDocumentoConfig tipoDocumentoConfig = tipoDocumentoConfigRepository.findByCodigo(tipoDocumento)
                .orElse(null);

        Documento documentoExistente = documentoRepository
                .findByProyectoIdAndTipoDocumentoConfigCodigo(proyectoId, tipoDocumento)
                .orElse(null);

        if (documentoExistente != null) {
            // ELIMINADO: storageProvider.deleteFile(STORAGE_SUBDIR, documentoExistente.getNombreAlmacenado());
            // No podemos borrar el archivo físico porque la version anterior (histórica) en `DocumentoProyectoVersion`
            // lo sigue referenciando y es necesario para que funcione el visor de historial.
            documentoExistente.setNombreOriginal(version.getNombreArchivoOriginal());
            documentoExistente.setNombreAlmacenado(version.getNombreAlmacenado());
            documentoExistente.setRutaAlmacenamiento(version.getRutaAlmacenamiento());
            documentoExistente.setMimeType(version.getMimeType());
            documentoExistente.setTamanoBytes(version.getTamanoBytes());
            documentoExistente.setUrlDescarga(construirUrlDescarga(proyectoId, tipoDocumento));
            documentoExistente.setFechaCarga(version.getSubidoEn());
            documentoRepository.save(documentoExistente);
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
            documentoRepository.save(documento);
        }
    }

    private void validarExistenciaProyecto(String proyectoId) {
        proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + proyectoId));
    }

    private String extraerExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex).toLowerCase() : "";
    }

    private String generarNombreUnico(String tipoDocumento, String extension) {
        return tipoDocumento.toLowerCase() + "_" + UUID.randomUUID().toString();
    }

    private String resolverMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            return contentType.toLowerCase();
        }
        String ext = extraerExtension(file.getOriginalFilename()).toLowerCase();
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
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
        } catch (IllegalArgumentException ex) {
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
        String urlDescarga = construirUrlDescarga(v.getProyectoId(), v.getTipoDocumento()) + "?version=" + v.getNumeroVersion();
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
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
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
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return;

        switch (tipoDocumento) {
            case "VIABILIZACION" -> proyecto.setViabilizacionPdf(version.getNombreAlmacenado());
            case "CRONOGRAMA" -> proyecto.setCronogramaPdf(version.getNombreAlmacenado());
            case "ACTA_CONSTITUCION" -> {
                proyecto.setActaConstitucionPdf(version.getNombreAlmacenado());
                proyecto.marcarActaConstitucionCargada();
            }
            case "PLAN_COMUNICACIONES" -> {
                proyecto.setPlanComunicacionesPdf(version.getNombreAlmacenado());
                proyecto.setTienePlanComunicaciones(true);
            }
            default -> { }
        }

        boolean esPreWizard = PRE_WIZARD_TYPES.contains(tipoDocumento);
        boolean habiaDevueltos = false;
        if (esPreWizard) {
            habiaDevueltos = preWizardRevisionRepository.findByProyectoId(proyectoId).stream()
                    .anyMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
            marcarRevisionPendiente(proyectoId, tipoDocumento);
        }

        boolean eraDevuelta = proyecto.viabilidadDevuelta();
        if (esPreWizard && (proyecto.viabilidadPendiente() || eraDevuelta)) {
            proyecto.marcarViabilidadCargada();
        }

        boolean documentosCompletosAntes = Boolean.TRUE.equals(proyecto.getDocumentosCargados());
        verificarTodosDocumentosCargados(proyecto);
        proyectoRepository.save(proyecto);

        boolean ahoraCompletos = Boolean.TRUE.equals(proyecto.getDocumentosCargados());
        boolean recienCompletado = !documentosCompletosAntes && ahoraCompletos;
        boolean resubmissionCompleta = false;
        if (esPreWizard && habiaDevueltos) {
            resubmissionCompleta = preWizardRevisionRepository.findByProyectoId(proyectoId).stream()
                    .noneMatch(r -> r.getEstado() == DocumentoPreWizardEstado.DEVUELTO);
        }
        if (esPreWizard && (recienCompletado || resubmissionCompleta)) {
            notificarCargaDocumentos(proyectoId, actorUsername, proyecto);
        }
    }

    private void verificarTodosDocumentosCargados(Proyecto proyecto) {
        boolean tieneViabilidad = versionRepository
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), "VIABILIZACION")
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());
        boolean tienePlanComunicaciones = versionRepository
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), "PLAN_COMUNICACIONES")
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());
        boolean tieneMatrizRiesgos = versionRepository
                .findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(proyecto.getId(), "MATRIZ_RIESGOS_VIABILIDAD")
                .stream().anyMatch(v -> v.getRutaAlmacenamiento() != null && !v.getRutaAlmacenamiento().isBlank());

        proyecto.setDocumentosCargados(tieneViabilidad && tienePlanComunicaciones && tieneMatrizRiesgos);
    }

    private void notificarCargaDocumentos(String proyectoId, String directorUsername, Proyecto proyecto) {
        publicarNotificacionProyecto(proyecto, directorUsername, NotificationEventType.VIABILIDAD_UPLOADED, payload ->
                payload.put("documentStatuses", buildDocumentStatusSummary(proyectoId)));
    }

    private String buildDocumentStatusSummary(String proyectoId) {
        Map<String, DocumentoPreWizardRevision> revisiones = preWizardRevisionRepository
                .findByProyectoId(proyectoId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        Function.identity(),
                        (a, b) -> a
                ));

        StringBuilder sb = new StringBuilder();
        for (String tipo : PRE_WIZARD_TYPES) {
            boolean cargado = versionRepository
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
            } else if ("DEVUELTO".equals(estadoRevision)) {
                estado = "DEVUELTO";
            } else {
                estado = estadoRevision;
            }
            sb.append("- ").append(nombreDocumentoPreWizard(tipo)).append(": ").append(estado);
            if ("DEVUELTO".equals(estadoRevision) && rev.getObservacion() != null && !rev.getObservacion().isBlank()) {
                sb.append(" — ").append(rev.getObservacion().trim());
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String nombreDocumentoPreWizard(String tipo) {
        return switch (tipo) {
            case "VIABILIZACION" -> "Documento de Viabilidad";
            case "PLAN_COMUNICACIONES" -> "Plan de Comunicaciones";
            case "MATRIZ_RIESGOS_VIABILIDAD" -> "Matriz de Riesgos de Viabilidad";
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
