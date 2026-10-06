package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.dto.document.DocumentoPreWizardRevisionDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.*;
import com.proyecta.api_gestion.domain.model.config.EstrategiaPetiConfig;
import com.proyecta.api_gestion.domain.model.config.EstadoProyectoConfig;
import com.proyecta.api_gestion.domain.model.enums.*;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.DocumentoProyectoVersionRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.DocumentoPreWizardRevisionRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.config.EstadoProyectoConfigRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.domain.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.config.PetiCatalogService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import com.proyecta.api_gestion.service.security.dynamic.SecurityCatalogCacheService;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.support.FuragSupport;
import com.proyecta.api_gestion.service.support.ProjectStructureSupport;
import com.proyecta.api_gestion.service.support.RiesgoInicialSupport;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.hibernate.Hibernate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    private static final String PROJECT_CODE_PREFIX = "PROY-CUN-";
    private static final String PROJECT_CODE_REGEX = "^PROY-CUN-\\d{4}-\\d+$";
    private static final String MSG_PROYECTO_NO_ENCONTRADO = "Proyecto no encontrado con id: ";
    private static final String KEY_PROJECT_NAME = "projectName";
    private static final String KEY_STATE = "state";
    private static final String KEY_RECIPIENTS = "recipients";

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final FuragSupport furagSupport;
    private final SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort;
    private final SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort;
    private final SecurityCatalogCacheService securityCatalogCacheService;
    private final PetiCatalogService petiCatalogService;
    private final IProgressCalculator progressCalculator;
    private final NotificationEventPublisherPort notificationPublisher;
    private final KeycloakIdentityExtractor identityExtractor;
    private final DocumentoProyectoVersionRepositoryPort documentoVersionRepositoryPort;
    private final DocumentoPreWizardRevisionRepositoryPort preWizardRevisionRepositoryPort;
    private final RiesgoInicialSupport riesgoInicialSupport;
    private final EstadoProyectoConfigRepositoryPort estadoProyectoConfigRepositoryPort;
    private final ProjectStructureSupport projectStructureSupport;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public ProyectoServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                               FuragSupport furagSupport,
                               SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort,
                               SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort,
                               SecurityCatalogCacheService securityCatalogCacheService,
                               PetiCatalogService petiCatalogService,
                               IProgressCalculator progressCalculator,
                               NotificationEventPublisherPort notificationPublisher,
                               KeycloakIdentityExtractor identityExtractor,
                               DocumentoProyectoVersionRepositoryPort documentoVersionRepositoryPort,
                               DocumentoPreWizardRevisionRepositoryPort preWizardRevisionRepositoryPort,
                               RiesgoInicialSupport riesgoInicialSupport,
                               EstadoProyectoConfigRepositoryPort estadoProyectoConfigRepositoryPort,
                               ProjectStructureSupport projectStructureSupport,
                               com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.furagSupport = furagSupport;
        this.usuarioProyectoRepositoryPort = usuarioProyectoRepositoryPort;
        this.seguridadUsuarioRepositoryPort = seguridadUsuarioRepositoryPort;
        this.securityCatalogCacheService = securityCatalogCacheService;
        this.petiCatalogService = petiCatalogService;
        this.progressCalculator = progressCalculator;
        this.notificationPublisher = notificationPublisher;
        this.identityExtractor = identityExtractor;
        this.documentoVersionRepositoryPort = documentoVersionRepositoryPort;
        this.preWizardRevisionRepositoryPort = preWizardRevisionRepositoryPort;
        this.riesgoInicialSupport = riesgoInicialSupport;
        this.estadoProyectoConfigRepositoryPort = estadoProyectoConfigRepositoryPort;
        this.projectStructureSupport = projectStructureSupport;
        this.objectMapper = objectMapper;
    }

    private EstadoProyectoConfig estadoProyectoConfig(String codigo) {
        return estadoProyectoConfigRepositoryPort.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Estado de proyecto no configurado: " + codigo));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProyectoListDTO> listarProyectos(String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti, PageQuery query) {
        return proyectoRepositoryPort.listarProyectos(nombre, normalizeProjectId(codigo), dependencia, estado, peti, query)
                .map(this::mapToListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoListDTO> listarProyectosAsignados(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<String> proyectoIds = usuarioProyectoRepositoryPort.findProyectoIdsByUsername(username).stream()
                .map(this::normalizeProjectId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        if (proyectoIds.isEmpty()) {
            return List.of();
        }

        return proyectoRepositoryPort.findByIdsOrdenadoPorIdDesc(proyectoIds).stream()
                .map(this::mapToListDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeguridadUsuarioDTO> listarDirectoresAsignables() {
        return seguridadUsuarioRepositoryPort.findAssignableProjectDirectors().stream()
                .map(this::mapToSeguridadUsuarioDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoResponseDTO obtenerPorId(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));
        initializeLazyCollections(proyecto);
        return mapToResponseDto(proyecto);
    }

    @Override
    @Transactional
    public ProyectoCreatedDTO registrarProyectoInicial(ProyectoRegistroInicialDTO dto, String gestorUsername) {
        String codigoSolicitado = trimToNull(dto.codigoProyecto());
        String codigo = codigoSolicitado == null ? generarCodigo() : normalizeProjectId(codigoSolicitado);
        if (!codigo.matches("[A-Z0-9][A-Z0-9_-]*")) {
            throw new BadRequestException("El codigo del proyecto solo puede contener letras, numeros, guiones y guiones bajos");
        }
        if (proyectoRepositoryPort.existsById(codigo)) {
            if (esCodigoAutomatico(codigo)) {
                codigo = generarCodigo();
            } else {
                throw new BadRequestException("Ya existe un proyecto con el codigo: " + codigo);
            }
        }

        SeguridadUsuario director = seguridadUsuarioRepositoryPort.findById(dto.directorUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario Director no encontrado con id: " + dto.directorUsuarioId()));
        validarUsuarioDirector(director);

        Proyecto proyecto = new Proyecto();
        proyecto.setId(codigo);
        proyecto.setNombre(dto.nombre().trim());
        proyecto.setObjetivoGeneral(dto.objetivoGeneral().trim());
        proyecto.setDirector(firstNonBlank(director.getNombre(), director.getUsername()));
        proyecto.setCorreoDirector(director.getCorreo());
        proyecto.setDirectorUsuario(director);
        proyecto.setAvanceTotal(BigDecimal.ZERO);
        proyecto.marcarRegistroInicialPendiente(gestorUsername, estadoProyectoConfig("PENDIENTE_COMPLETAR"));

        Proyecto guardado = proyectoRepositoryPort.save(proyecto);
        asignarDirectorProyecto(guardado, director);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_INITIAL_REGISTERED,
                guardado.getId(),
                gestorUsername,
                Map.of(
                        KEY_PROJECT_NAME, guardado.getNombre(),
                        KEY_STATE, guardado.getEstadoCodigo(),
                        KEY_RECIPIENTS, List.of(director.getCorreo(), gestorUsername)
                )));

        return new ProyectoCreatedDTO(
                guardado.getId(),
                guardado.getId(),
                guardado.getNombre(),
                guardado.getEstado(),
                "Proyecto registrado. Pendiente de completar por el Director asignado."
        );
    }

    @Override
    @Transactional
    public ProyectoCompletionStatusDTO obtenerEstadoCompletitud(String id, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        boolean directorAsignado = esDirectorAsignado(proyecto, username);
        if (proyecto.requiereCompletitudDirector() && directorAsignado) {
            proyecto.registrarPrimerIngresoDirector();
            proyecto = proyectoRepositoryPort.save(proyecto);
        }

        boolean requiereCompletitud = proyecto.requiereCompletitudDirector();
        boolean documentosCargados = Boolean.TRUE.equals(proyecto.getDocumentosCargados());

        final List<String> preWizardTypes = List.of(
                "VIABILIZACION", "PLAN_COMUNICACIONES", "MATRIZ_RIESGOS_VIABILIDAD");
        PreWizardResumen preWizard = construirResumenPreWizard(normalizedId, preWizardTypes);

        boolean confirmada = proyecto.viabilidadAprobada()
                && Boolean.TRUE.equals(proyecto.getDocumentosVerificados());
        String viabilidadEstado = resolverViabilidadEstado(
                confirmada, preWizard.todosAprobado(), documentosCargados, preWizard.algunoDevuelto());

        FlagsCompletitud flags = resolverFlagsCompletitud(
                requiereCompletitud, directorAsignado, confirmada, preWizard.todosAprobado(),
                documentosCargados, viabilidadEstado);
        boolean puedeCompletar = flags.puedeCompletarWizard();

        boolean plazoVencido = proyecto.plazoCompletarVencido();

        String mensaje = resolverMensajeCompletitud(
                proyecto, requiereCompletitud, viabilidadEstado, flags.puedeCargarViabilidad(),
                documentosCargados, flags.documentosVerificados(), plazoVencido);

        return new ProyectoCompletionStatusDTO(
                proyecto.getId(),
                proyecto.getEstadoCodigo(),
                requiereCompletitud,
                proyecto.getPrimerIngresoDirectorAt() != null,
                puedeCompletar,
                proyecto.getPrimerIngresoDirectorAt(),
                proyecto.getCompletadoPorDirectorAt(),
                viabilidadEstado,
                proyecto.getViabilidadObservaciones(),
                ViabilidadEstado.APROBADA.name().equals(viabilidadEstado),
                documentosCargados,
                flags.documentosVerificados(),
                flags.puedeCargarViabilidad(),
                flags.puedeCompletarWizard(),
                proyecto.getFechaLimiteCompletar(),
                plazoVencido,
                Boolean.TRUE.equals(proyecto.getCierreForzoso()),
                mensaje,
                preWizard.documentos()
        );
    }

    private record PreWizardResumen(List<DocumentoPreWizardRevisionDTO> documentos, boolean todosAprobado, boolean algunoDevuelto) {
    }

    private record FlagsCompletitud(boolean documentosVerificados, boolean puedeCargarViabilidad, boolean puedeCompletarWizard) {
    }

    private PreWizardResumen construirResumenPreWizard(String normalizedId, List<String> preWizardTypes) {
        Map<String, DocumentoPreWizardRevision> revisionesPorTipo = preWizardRevisionRepositoryPort
                .findByProyectoId(normalizedId)
                .stream()
                .collect(Collectors.toMap(
                        DocumentoPreWizardRevision::getTipoDocumento,
                        r -> r,
                        (a, b) -> a
                ));

        List<DocumentoPreWizardRevisionDTO> documentosPreWizard = new ArrayList<>();
        boolean todosAprobado = true;
        boolean algunoDevuelto = false;
        for (String tipo : preWizardTypes) {
            DocumentoPreWizardRevision revision = revisionesPorTipo.get(tipo);
            if (revision == null) {
                todosAprobado = false;
                documentosPreWizard.add(new DocumentoPreWizardRevisionDTO(
                        tipo,
                        DocumentoPreWizardEstado.PENDIENTE.name(),
                        null,
                        null,
                        null
                ));
                continue;
            }
            documentosPreWizard.add(new DocumentoPreWizardRevisionDTO(
                    revision.getTipoDocumento(),
                    revision.getEstado().name(),
                    revision.getObservacion(),
                    revision.getRevisadoPor(),
                    revision.getRevisadoEn()
            ));
            if (revision.getEstado() != DocumentoPreWizardEstado.APROBADO) {
                todosAprobado = false;
            }
            if (revision.getEstado() == DocumentoPreWizardEstado.DEVUELTO) {
                algunoDevuelto = true;
            }
        }
        return new PreWizardResumen(documentosPreWizard, todosAprobado, algunoDevuelto);
    }

    private String resolverViabilidadEstado(boolean confirmada, boolean todosAprobado, boolean documentosCargados, boolean algunoDevuelto) {
        if (confirmada && todosAprobado && documentosCargados) {
            return ViabilidadEstado.APROBADA.name();
        }
        if (algunoDevuelto) {
            return ViabilidadEstado.DEVUELTA.name();
        }
        if (documentosCargados) {
            return ViabilidadEstado.CARGADA.name();
        }
        return ViabilidadEstado.PENDIENTE.name();
    }

    private FlagsCompletitud resolverFlagsCompletitud(boolean requiereCompletitud, boolean directorAsignado, boolean confirmada, boolean todosAprobado, boolean documentosCargados, String viabilidadEstado) {
        boolean documentosVerificados = confirmada && todosAprobado && documentosCargados;
        boolean puedeCargarViabilidad = requiereCompletitud && directorAsignado
                && (ViabilidadEstado.PENDIENTE.name().equals(viabilidadEstado)
                    || ViabilidadEstado.DEVUELTA.name().equals(viabilidadEstado));
        boolean puedeCompletarWizard = requiereCompletitud && directorAsignado && documentosVerificados;
        return new FlagsCompletitud(documentosVerificados, puedeCargarViabilidad, puedeCompletarWizard);
    }

    private String resolverMensajeCompletitud(Proyecto proyecto, boolean requiereCompletitud, String viabilidadEstado, boolean puedeCargarViabilidad, boolean documentosCargados, boolean documentosVerificados, boolean plazoVencido) {
        if (!requiereCompletitud) {
            return "El proyecto ya tiene su informacion inicial completa.";
        }
        if (Boolean.TRUE.equals(proyecto.getCierreForzoso())) {
            return "El proyecto fue cerrado forzosamente por el Gestor.";
        }
        if (ViabilidadEstado.DEVUELTA.name().equals(viabilidadEstado)) {
            return "Algunos documentos fueron devueltos con observaciones. El Director debe subsanar.";
        }
        if (puedeCargarViabilidad) {
            return "El Director debe cargar los 3 documentos (Viabilidad, Plan de Comunicaciones y Matriz de Riesgos de Viabilidad).";
        }
        if (documentosCargados && !documentosVerificados) {
            return "Los documentos estan cargados y pendientes de verificacion por parte del Gestor.";
        }
        if (documentosVerificados) {
            if (plazoVencido) {
                return "El plazo de 30 dias para completar el proyecto ha vencido. El Gestor puede realizar el cierre forzoso.";
            }
            return "Documentos verificados. El Director debe completar la informacion del proyecto dentro de 30 dias.";
        }
        return "El Director asignado debe completar la informacion inicial antes de acceder a los modulos operativos.";
    }

    @Override
    @Transactional
    public ProyectoResponseDTO completarInformacionInicial(String id, ProyectoCompletarInformacionDTO dto, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        if (!proyecto.requiereCompletitudDirector()) {
            throw new BadRequestException("El proyecto no esta pendiente de completar.");
        }
        if (!tieneAccesoCompletitud(proyecto, username)) {
            throw new BadRequestException("Solo el Director asignado o un Gestor puede completar la informacion inicial del proyecto.");
        }
        boolean existePlan = documentoVersionRepositoryPort
                .findByProyectoIdAndTipoDocumentoAndEstado(
                        normalizedId, "PLAN_COMUNICACIONES", DocumentoProyectoVersionEstado.ACTUAL)
                .isPresent();
        if (!existePlan) {
            throw new BadRequestException(
                    "Debe cargar el documento del Plan de Comunicaciones en la gestion documental antes de completar el proyecto.");
        }

        aplicarInformacionComplementaria(proyecto, dto);
        proyecto.completarInformacionInicialPorDirector(
                estadoProyectoConfig(proyecto.calcularEstadoCompletitudInicial().name()));

        Proyecto guardado = proyectoRepositoryPort.save(proyecto);
        furagSupport.sincronizarRespuestasFurag(guardado);
        riesgoInicialSupport.crearRiesgosIniciales(guardado, dto.riesgosIniciales());
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_INITIAL_COMPLETED,
                guardado.getId(),
                username,
                Map.of(
                        KEY_PROJECT_NAME, guardado.getNombre(),
                        KEY_STATE, guardado.getEstadoCodigo(),
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(guardado)
                )));
        return proyectoRepositoryPort.findById(guardado.getId()).map(p -> {
            initializeLazyCollections(p);
            return mapToResponseDto(p);
        }).orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado tras completar información: " + guardado.getId()));
    }

    @Override
    @Transactional
    public ProyectoResponseDTO actualizarProyecto(String id, ProyectoUpdateDTO dto, Authentication authentication) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));
        String actorUsername = identityExtractor.resolveUsername(authentication);

        aplicarCamposPrincipales(proyecto, dto);
        aplicarObjetivosEspecificos(proyecto, dto);
        aplicarCamposDetalle(proyecto, dto);
        aplicarCamposPeti(proyecto, dto);
        aplicarFuragYListas(proyecto, dto);

        Proyecto actualizado = proyectoRepositoryPort.save(proyecto);
        furagSupport.sincronizarRespuestasFurag(actualizado);
        notificarCambioProyecto(actualizado, actorUsername, NotificationEventType.PROJECT_UPDATED, Map.of(
                KEY_PROJECT_NAME, actualizado.getNombre(),
                KEY_STATE, actualizado.getEstadoCodigo()
        ));
        return proyectoRepositoryPort.findById(actualizado.getId()).map(p -> {
            initializeLazyCollections(p);
            return mapToResponseDto(p);
        }).orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado tras actualización: " + actualizado.getId()));
    }

    private void aplicarCamposPrincipales(Proyecto proyecto, ProyectoUpdateDTO dto) {
        if (dto.nombre() != null) proyecto.setNombre(dto.nombre());
        if (dto.dependencia() != null) proyecto.setDependencia(dto.dependencia());
        if (dto.director() != null) proyecto.setDirector(dto.director());
        if (dto.correoDirector() != null) proyecto.setCorreoDirector(dto.correoDirector());
        if (dto.objetivoGeneral() != null) proyecto.setObjetivoGeneral(dto.objetivoGeneral());
    }

    private void aplicarObjetivosEspecificos(Proyecto proyecto, ProyectoUpdateDTO dto) {
        if (dto.objetivosEspecificos() == null) {
            return;
        }
        proyecto.getObjetivosEspecificos().clear();
        dto.objetivosEspecificos().stream()
                .map(this::trimToNull)
                .filter(value -> value != null && !value.isBlank())
                .map(desc -> {
                    ObjetivoEspecifico obj = new ObjetivoEspecifico();
                    obj.setDescripcion(desc);
                    obj.setProyecto(proyecto);
                    return obj;
                })
                .forEach(proyecto.getObjetivosEspecificos()::add);
    }

    private void aplicarCamposDetalle(Proyecto proyecto, ProyectoUpdateDTO dto) {
        if (dto.alcanceDetallado() != null) proyecto.setAlcanceDetallado(dto.alcanceDetallado());
        if (dto.presupuestoEstimado() != null) proyecto.setPresupuestoEstimado(dto.presupuestoEstimado());
        if (dto.fechaInicio() != null) proyecto.setFechaInicio(dto.fechaInicio());
    }

    private void aplicarCamposPeti(Proyecto proyecto, ProyectoUpdateDTO dto) {
        if (dto.peti() != null) proyecto.setPeti(dto.peti());
        if (dto.vigenciaPeti() != null) proyecto.setVigenciaPeti(dto.vigenciaPeti());
        if (dto.estrategiaPeti() != null) aplicarEstrategiaPeti(proyecto, dto.estrategiaPeti());
        if (dto.tienePlanComunicaciones() != null) proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());
    }

    private void aplicarFuragYListas(Proyecto proyecto, ProyectoUpdateDTO dto) {
        if (dto.furag() != null) proyecto.setFurag(furagSupport.buildFurag(dto.furag()));
        if (dto.equipoTrabajo() != null) {
            proyecto.getEquipoTrabajo().clear();
            dto.equipoTrabajo().stream()
                    .map(m -> new MiembroEquipo(m.nombre(), m.cargo(), m.rol(), m.dependencia(), m.telefono(), m.correo()))
                    .forEach(proyecto.getEquipoTrabajo()::add);
        }
        if (dto.patrocinador() != null) {
            String dependenciaEquipo = proyecto.getEquipoTrabajo().stream()
                    .map(MiembroEquipo::getDependencia)
                    .filter(d -> d != null && !d.isBlank())
                    .findFirst()
                    .orElse(null);
            proyecto.setPatrocinador(buildPatrocinador(dto.patrocinador(), dependenciaEquipo));
        }
        if (dto.stakeholders() != null) {
            proyecto.getStakeholders().clear();
            dto.stakeholders().stream()
                    .map(s -> new Stakeholder(s.rol(), s.descripcion(), s.interes(), s.impacto()))
                    .forEach(proyecto.getStakeholders()::add);
        }
        if (dto.fases() != null) {
            proyecto.getFases().clear();
            int[] hitoIdx = {0};
            int[] entIdx = {0};
            int[] faseIdx = {0};
            dto.fases().stream()
                    .map(faseDto -> { faseIdx[0]++; return projectStructureSupport.buildFase(faseDto, proyecto, faseIdx[0], hitoIdx, entIdx); })
                    .forEach(proyecto.getFases()::add);
        }
    }

    @Override
    @Transactional
    public void eliminarProyecto(String id, Authentication authentication) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));
        String actorUsername = identityExtractor.resolveUsername(authentication);
        proyectoRepositoryPort.delete(proyecto);
        notificarCambioProyecto(proyecto, actorUsername, NotificationEventType.PROJECT_UPDATED, Map.of(
                KEY_PROJECT_NAME, proyecto.getNombre(),
                KEY_STATE, proyecto.getEstadoCodigo(),
                "action", "deleted"
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoResumenDTO obtenerResumen(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto p = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        long totalFases = p.getFases().size();
        boolean puedeCerrar = !p.esEstadoTerminal();
        PatrocinadorInfo patrocinador = resolverPatrocinadorInfo(p);
        DirectorInfo director = resolverDirectorInfo(p);
        ResumenFases resumenFases = contarResumenFases(p);

        if (resumenFases.totalEntregables() == 0 || resumenFases.entregablesConformes() < resumenFases.totalEntregables()) {
            puedeCerrar = false;
        }

        return new ProyectoResumenDTO(
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                director.nombre(),
                director.cargo(),
                director.entidad(),
                patrocinador.nombre(),
                patrocinador.cargo(),
                patrocinador.entidad(),
                p.getFechaInicio(),
                p.getObjetivoGeneral(),
                extraerObjetivosEspecificos(p),
                p.getAvanceTotal(),
                p.getEstadoCodigo(),
                totalFases,
                resumenFases.totalHitos(),
                resumenFases.entregablesConformes(),
                resumenFases.totalEntregables(),
                puedeCerrar,
                resumenFases.entregables(),
                Boolean.TRUE.equals(p.getCierreSolicitado()),
                p.getCierreEstado(),
                p.getCierreObservaciones(),
                p.getCierreBorradorJson()
        );
    }

    private record PatrocinadorInfo(String nombre, String cargo, String entidad) {
    }

    private record DirectorInfo(String nombre, String cargo, String entidad) {
    }

    private record ResumenFases(long totalHitos, long totalEntregables, long entregablesConformes, List<String> entregables) {
    }

    private PatrocinadorInfo resolverPatrocinadorInfo(Proyecto p) {
        return new PatrocinadorInfo(
                p.getPatrocinador() != null ? p.getPatrocinador().getNombre() : null,
                p.getPatrocinador() != null ? p.getPatrocinador().getCargo() : null,
                p.getPatrocinador() != null ? p.getPatrocinador().getEntidad() : null);
    }

    private DirectorInfo resolverDirectorInfo(Proyecto p) {
        SeguridadUsuarioProyecto directorAsignado = findDirectorAsignado(p);
        String directorNombre = directorNameFromAssignment(directorAsignado);
        String directorCargo = directorNombre != null ? directorAsignado.getCargo() : null;
        String directorEntidad = directorNombre != null && directorAsignado.getUsuario() != null
                ? directorAsignado.getUsuario().getDependencia()
                : null;
        return new DirectorInfo(directorNombre, directorCargo, directorEntidad);
    }

    private ResumenFases contarResumenFases(Proyecto p) {
        long totalHitos = 0;
        long totalEntregables = 0;
        long entregablesConformes = 0;
        List<String> entregables = new ArrayList<>();
        for (Fase f : p.getFases()) {
            totalHitos += f.getHitos().size();
            for (Hito h : f.getHitos()) {
                totalEntregables += h.getEntregables().size();
                entregablesConformes += h.getEntregables().stream()
                        .filter(Entregable::esConforme)
                        .count();
                for (Entregable e : h.getEntregables()) {
                    entregables.add(e.getNombre());
                }
            }
        }
        return new ResumenFases(totalHitos, totalEntregables, entregablesConformes, entregables);
    }

    private List<String> extraerObjetivosEspecificos(Proyecto p) {
        return p.getObjetivosEspecificos() == null ? List.of() : p.getObjetivosEspecificos().stream()
                .map(ObjetivoEspecifico::getDescripcion)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    @Override
    @Transactional
    public void cerrarProyecto(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        BigDecimal avanceActual = progressCalculator.calcularYActualizarAvanceProyecto(normalizedId);
        proyecto.setAvanceTotal(avanceActual);

        // Uso de la lógica rica del dominio
        proyecto.cerrar(estadoProyectoConfig("CERRADO"));

        proyectoRepositoryPort.save(proyecto);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_CLOSED,
                proyecto.getId(),
                "system",
                Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        KEY_STATE, proyecto.getEstadoCodigo(),
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
                )));
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO obtenerDashboard() {
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        int diasUmbral = proyectoRepositoryPort.getDiasUmbralProximo().orElse(8);
        LocalDate umbral = hoy.plusDays(diasUmbral);

        return new DashboardDTO(
                proyectoRepositoryPort.countTotal(),
                proyectoRepositoryPort.countActivos(),
                proyectoRepositoryPort.countCerrados(),
                proyectoRepositoryPort.getAvancePromedio(),
                proyectoRepositoryPort.countEntregablesAtrasados(hoy),
                proyectoRepositoryPort.countEntregablesProximosAVencer(hoy, umbral),
                diasUmbral
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Furag obtenerFurag(String id) {
        final String normalizedId = normalizeProjectId(id);
        proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));
        Furag reconstructed = furagSupport.reconstruirFuragDesdeRespuestas(normalizedId);
        if (reconstructed != null) {
            return reconstructed;
        }
        return new Furag();
    }

    @Override
    @Transactional
    public void recalcularAvances() {
        List<Proyecto> proyectos = proyectoRepositoryPort.findAll();
        for (Proyecto proyecto : proyectos) {
            progressCalculator.calcularYActualizarAvanceProyecto(proyecto.getId());
        }
    }

    @Override
    @Transactional
    public void actualizarFurag(String id, Furag furag) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));
        furagSupport.validarFuragCompleto(furag, furag.getRespuestas());
        proyecto.setFurag(furag);
        Proyecto guardado = proyectoRepositoryPort.save(proyecto);
        furagSupport.sincronizarRespuestasFurag(guardado);
    }

    private void aplicarInformacionComplementaria(Proyecto proyecto, ProyectoCompletarInformacionDTO dto) {
        projectStructureSupport.validarPonderaciones(dto.fases());

        proyecto.setDependencia(dto.dependencia().trim());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setAlcanceDetallado(trimToNull(dto.alcanceDetallado()));
        proyecto.setPresupuestoEstimado(dto.presupuestoEstimado());
        proyecto.setPeti(dto.peti());
        proyecto.setVigenciaPeti(trimToNull(dto.vigenciaPeti()));
        aplicarEstrategiaPeti(proyecto, dto.estrategiaPeti());
        proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());
        proyecto.setPatrocinador(buildPatrocinador(dto.patrocinador(),
                dto.equipoTrabajo() != null ? dto.equipoTrabajo().stream()
                        .map(EquipoTrabajoDTO::dependencia)
                        .filter(d -> d != null && !d.isBlank())
                        .findFirst()
                        .orElse(null) : null));
        proyecto.setFurag(furagSupport.buildFurag(dto.furag()));

        proyecto.getObjetivosEspecificos().clear();
        if (dto.objetivosEspecificos() != null) {
            dto.objetivosEspecificos().stream()
                    .map(this::trimToNull)
                    .filter(value -> value != null && !value.isBlank())
                    .map(desc -> {
                        ObjetivoEspecifico obj = new ObjetivoEspecifico();
                        obj.setDescripcion(desc);
                        obj.setProyecto(proyecto);
                        return obj;
                    })
                    .forEach(proyecto.getObjetivosEspecificos()::add);
        }

        proyecto.getEquipoTrabajo().clear();
        if (dto.equipoTrabajo() != null) {
            dto.equipoTrabajo().stream()
                    .map(m -> new MiembroEquipo(m.nombre(), m.cargo(), m.rol(), m.dependencia(), m.telefono(), m.correo()))
                    .forEach(proyecto.getEquipoTrabajo()::add);
        }

        proyecto.getStakeholders().clear();
        if (dto.stakeholders() != null) {
            dto.stakeholders().stream()
                    .map(s -> new Stakeholder(s.rol(), s.descripcion(), s.interes(), s.impacto()))
                    .forEach(proyecto.getStakeholders()::add);
        }

        if (dto.fases() != null) {
            proyecto.getFases().clear();
            final int[] faseIdx = {0};
            final int[] hitoIdx = {0};
            final int[] entIdx = {0};
            dto.fases().stream()
                    .map(faseDto -> { faseIdx[0]++; return projectStructureSupport.buildFase(faseDto, proyecto, faseIdx[0], hitoIdx, entIdx); })
                    .forEach(proyecto.getFases()::add);
        }
    }

    private Patrocinador buildPatrocinador(PatrocinadorDTO dto, String dependenciaEquipo) {
        if (dto == null) {
            return null;
        }
        Patrocinador pat = new Patrocinador();
        pat.setNombre(dto.nombre());
        pat.setEntidad(dependenciaEquipo != null ? dependenciaEquipo.trim() : null);
        pat.setCargo(dto.cargo());
        pat.setProcesoSigc(dto.procesoSigc());
        pat.setProcedimiento(dto.procedimientoSigc());
        return pat;
    }

    private void validarUsuarioDirector(SeguridadUsuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("El usuario Director asignado no esta activo.");
        }
        if (!esRolDirector(usuario)) {
            throw new BadRequestException("El usuario asignado debe tener rol Director de Proyecto.");
        }
    }

    private boolean esRolDirector(SeguridadUsuario usuario) {
        Set<String> directorRoles = Set.of("director_proyecto", "director_pro");
        String rolCodigo = normalizeRole(usuario.getRolCodigo());
        String rolNombre = normalizeRole(usuario.getRolNombre());
        return directorRoles.contains(rolCodigo) || directorRoles.contains(rolNombre) || rolNombre.contains("director");
    }

    private void asignarDirectorProyecto(Proyecto proyecto, SeguridadUsuario director) {
        SeguridadUsuarioProyecto asignacion = usuarioProyectoRepositoryPort
                .findByUsuarioUsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(
                        director.getUsername(),
                        proyecto.getId(),
                        "DIRECTOR_PROYECTO"
                )
                .orElseGet(SeguridadUsuarioProyecto::new);
        asignacion.setUsuario(director);
        asignacion.setProyectoId(proyecto.getId());
        asignacion.setCargo("DIRECTOR_PROYECTO");
        asignacion.setActivo(true);
        usuarioProyectoRepositoryPort.save(asignacion);
        securityCatalogCacheService.evictAll();
    }

    private boolean esDirectorAsignado(Proyecto proyecto, String username) {
        if (proyecto == null || proyecto.getId() == null || username == null || username.isBlank()) {
            return false;
        }
        return usuarioProyectoRepositoryPort.findActiveDirectorAssignmentsByProyectoId(proyecto.getId()).stream()
                .map(SeguridadUsuarioProyecto::getUsuario)
                .filter(usuario -> usuario != null && usuario.getUsername() != null)
                .anyMatch(usuario -> usuario.getUsername().equalsIgnoreCase(username));
    }

    private boolean tieneAccesoCompletitud(Proyecto proyecto, String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        SeguridadUsuario usuario = seguridadUsuarioRepositoryPort.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null) {
            return false;
        }
        String rolNormalizado = SecurityRoleCatalog.normalize(usuario.getRolCodigo());
        if (rolNormalizado != null && SecurityRoleCatalog.isTransversal(rolNormalizado)) {
            return true;
        }
        return esDirectorAsignado(proyecto, username);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private SeguridadUsuarioDTO mapToSeguridadUsuarioDto(SeguridadUsuario usuario) {
        return new SeguridadUsuarioDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getDependencia(),
                usuario.getActivo(),
                usuario.getRolCodigo(),
                usuario.getRolNombre(),
                usuario.getFechaCreacion(),
                usuario.getUltimoAcceso()
        );
    }

    @Override
    public String obtenerSiguienteCodigo() {
        return generarCodigo();
    }

    private synchronized String generarCodigo() {
        String yearPrefix = PROJECT_CODE_PREFIX + java.time.Year.now(java.time.ZoneId.systemDefault()).getValue() + "-";
        int maxConsecutivo = proyectoRepositoryPort.findAll().stream()
                .map(Proyecto::getId)
                .filter(id -> id != null && id.startsWith(yearPrefix))
                .mapToInt(this::extractConsecutivoCodigoProyecto)
                .max()
                .orElse(0);

        String codigo;
        int siguiente = maxConsecutivo + 1;
        do {
            codigo = String.format("%s%03d", yearPrefix, siguiente++);
        } while (proyectoRepositoryPort.existsById(codigo));

        return codigo;
    }

    private int extractConsecutivoCodigoProyecto(String id) {
        String normalized = normalizeProjectId(id);
        if (normalized == null || !normalized.startsWith(PROJECT_CODE_PREFIX)) {
            return 0;
        }

        try {
            String afterPrefix = normalized.substring(PROJECT_CODE_PREFIX.length());
            int lastDash = afterPrefix.lastIndexOf('-');
            if (lastDash < 0) {
                return 0;
            }
            return Integer.parseInt(afterPrefix.substring(lastDash + 1));
        } catch (NumberFormatException _) {
            return 0;
        }
    }

    private boolean esCodigoAutomatico(String codigo) {
        return codigo != null && codigo.matches(PROJECT_CODE_REGEX);
    }

    private ProyectoListDTO mapToListDto(Proyecto p) {
        ConteoEntregables conteo = contarEntregables(p);

        return new ProyectoListDTO(
                p.getId(),
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                resolveDirectorAsignado(p),
                p.getPeti(),
                p.getAvanceTotal(),
                p.getEstadoCodigo(),
                p.getViabilidadEstado() != null ? p.getViabilidadEstado().name() : null,
                p.getDocumentosCargados(),
                (int) conteo.total(),
                (int) conteo.conformes(),
                (int) conteo.atrasados(),
                furagSupport.furagDetalleDe(p),
                Boolean.TRUE.equals(p.getCierreForzoso()),
                p.getCierreObservaciones(),
                p.getCierreForzosoPor(),
                p.getCierreForzosoEn()
        );
    }

    private record ConteoEntregables(long total, long conformes, long atrasados) {
    }

    private ConteoEntregables contarEntregables(Proyecto p) {
        long total = 0;
        long conformes = 0;
        long atrasados = 0;
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        for (Fase f : p.getFases()) {
            for (Hito h : f.getHitos()) {
                for (Entregable e : h.getEntregables()) {
                    total++;
                    if (e.esConforme()) conformes++;
                    if (!e.esConforme() && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy)) atrasados++;
                }
            }
        }
        return new ConteoEntregables(total, conformes, atrasados);
    }

    private String normalizeProjectId(String id) {
        return id == null ? null : id.trim().toUpperCase(Locale.ROOT);
    }

    private void aplicarEstrategiaPeti(Proyecto proyecto, String estrategiaPeti) {
        EstrategiaPetiConfig config = petiCatalogService.resolveEstrategiaConfig(estrategiaPeti);
        proyecto.setEstrategiaPetiConfig(config);
    }

    private String resolveEstrategiaPetiCodigo(Proyecto proyecto) {
        if (proyecto.getEstrategiaPetiConfig() != null) {
            return proyecto.getEstrategiaPetiConfig().getCodigo();
        }
        return null;
    }

    private void initializeLazyCollections(Proyecto p) {
        Hibernate.initialize(p.getObjetivosEspecificos());
        Hibernate.initialize(p.getEquipoTrabajo());
        Hibernate.initialize(p.getStakeholders());
        Hibernate.initialize(p.getPatrocinador());
        Hibernate.initialize(p.getFases());
        for (Fase f : p.getFases()) {
            Hibernate.initialize(f.getHitos());
            for (Hito h : f.getHitos()) {
                Hibernate.initialize(h.getEntregables());
            }
        }
    }

    private ProyectoResponseDTO mapToResponseDto(Proyecto p) {
        return new ProyectoResponseDTO(
                p.getId(),
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                resolveDirectorAsignado(p),
                resolveDirectorUsuarioId(p),
                resolveDirectorCorreoAsignado(p),
                p.getObjetivoGeneral(),
                p.getObjetivosEspecificos().stream().map(ObjetivoEspecifico::getDescripcion).toList(),
                p.getFechaInicio(),
                p.getEstadoCodigo(),
                p.getAvanceTotal(),
                p.getPresupuestoEstimado(),
                p.getAlcanceDetallado(),
                p.getPeti(),
                p.getVigenciaPeti(),
                resolveEstrategiaPetiCodigo(p),
                p.getTienePlanComunicaciones(),
                p.getPatrocinador() != null ? new PatrocinadorDTO(p.getPatrocinador().getNombre(), p.getPatrocinador().getCargo(), p.getPatrocinador().getProcesoSigc(), p.getPatrocinador().getProcedimiento()) : null,
                p.getEquipoTrabajo().stream().map(m -> new EquipoTrabajoDTO(m.getNombre(), m.getCargo(), m.getRol(), m.getDependencia(), m.getTelefono(), m.getCorreo())).toList(),
                p.getStakeholders().stream().map(s -> new StakeholderDTO(s.getRol(), s.getDescripcion(), s.getInteres(), s.getImpacto())).toList(),
                furagSupport.furagDtoDe(p),
                p.getFases().stream()
                        .sorted(ProjectHierarchyOrdering.FASES_BY_ORDEN)
                        .map(f -> new FaseResponseDTO(
                        f.getId(), f.getNombre(), f.getDescripcion(), f.getPonderacion(), f.getAvanceCalculado(),
                        f.getHitos().stream()
                                .sorted(ProjectHierarchyOrdering.HITOS_BY_ORDEN)
                                .map(h -> new HitoResponseDTO(
                                h.getId(), h.getNombre(), h.getDescripcion(), h.getPonderacion(), h.getAvanceCalculado(),
                                h.getEntregables().stream()
                                        .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_ORDEN)
                                        .map(e -> new EntregableResponseDTO(
                                        e.getId(), e.getNombre(), e.getDescripcion(), e.getPonderacion(), e.getEstadoCodigo(), e.esConforme(), e.getFechaInicio(), e.getFechaLimite()
                                )).toList()
                        )).toList()
                )).toList()
        );
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

    private SeguridadUsuarioProyecto findDirectorAsignado(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return null;
        }
        return usuarioProyectoRepositoryPort.findActiveDirectorAssignmentsByProyectoId(proyecto.getId()).stream()
                .filter(assignment -> assignment.getUsuario() != null)
                .findFirst()
                .orElse(null);
    }

    private String resolveDirectorAsignado(Proyecto proyecto) {
        return directorNameFromAssignment(findDirectorAsignado(proyecto));
    }

    private String resolveDirectorCorreoAsignado(Proyecto proyecto) {
        SeguridadUsuarioProyecto assignment = findDirectorAsignado(proyecto);
        return assignment != null && assignment.getUsuario() != null
                ? firstNonBlank(assignment.getUsuario().getCorreo())
                : null;
    }

    private String directorNameFromAssignment(SeguridadUsuarioProyecto assignment) {
        if (assignment == null || assignment.getUsuario() == null) {
            return null;
        }
        return firstNonBlank(
                assignment.getUsuario().getNombre(),
                assignment.getUsuario().getUsername()
        );
    }

    private String resolveDirectorUsuarioId(Proyecto proyecto) {
        SeguridadUsuarioProyecto assignment = findDirectorAsignado(proyecto);
        if (assignment == null || assignment.getUsuario() == null) {
            return null;
        }
        return String.valueOf(assignment.getUsuario().getId());
    }

    private void notificarCambioProyecto(Proyecto proyecto, String actorUsername, NotificationEventType eventType, Map<String, Object> extraAttributes) {
        if (proyecto == null || proyecto.getId() == null) {
            return;
        }
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            return;
        }

        Map<String, Object> attributes = new java.util.HashMap<>();
        if (extraAttributes != null) {
            attributes.putAll(extraAttributes);
        }
        attributes.put(KEY_RECIPIENTS, recipients);

        notificationPublisher.publish(new NotificationContext(
                eventType,
                proyecto.getId(),
                actorUsername,
                attributes
        ));
    }

    // ==================== COMPLETITUD POR FASES (BORRADOR) ====================

    @Override
    @Transactional
    public CompletitudBorradorDTO guardarBorradorCompletitud(String id, CompletitudBorradorDTO dto, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        if (!proyecto.requiereCompletitudDirector()) {
            throw new BadRequestException("El proyecto no esta pendiente de completar.");
        }
        if (!tieneAccesoCompletitud(proyecto, username)) {
            throw new BadRequestException("Solo el Director asignado o un Gestor puede guardar el borrador de completitud.");
        }

        try {
            String borradorJson = objectMapper.writeValueAsString(dto);
            proyecto.setCompletitudBorradorJson(borradorJson);
            proyectoRepositoryPort.save(proyecto);

            return dto;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Error al serializar el borrador: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompletitudBorradorDTO obtenerBorradorCompletitud(String id, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        if (!proyecto.requiereCompletitudDirector()) {
            throw new BadRequestException("El proyecto no esta pendiente de completar.");
        }
        if (!tieneAccesoCompletitud(proyecto, username)) {
            throw new BadRequestException("Solo el Director asignado o un Gestor puede obtener el borrador de completitud.");
        }

        String borradorJson = proyecto.getCompletitudBorradorJson();
        if (borradorJson == null || borradorJson.isBlank()) {
            return new CompletitudBorradorDTO(
                    1,
                    Map.of(),
                    null, null, null, null, null, null, null,
                    null
            );
        }

        try {
            return objectMapper.readValue(borradorJson, CompletitudBorradorDTO.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Error al deserializar el borrador: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProyectoResponseDTO completarFaseCompletitud(String id, Integer fase, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        if (!proyecto.requiereCompletitudDirector()) {
            throw new BadRequestException("El proyecto no esta pendiente de completar.");
        }
        if (!tieneAccesoCompletitud(proyecto, username)) {
            throw new BadRequestException("Solo el Director asignado o un Gestor puede completar fases de la completitud.");
        }
        if (fase < 1 || fase > 7) {
            throw new BadRequestException("Numero de fase invalido. Debe ser entre 1 y 7.");
        }

        try {
            String fasesCompletadasJson = proyecto.getCompletitudFasesCompletadas();
            java.util.Map<String, Boolean> fasesCompletadas;
            if (fasesCompletadasJson != null && !fasesCompletadasJson.isBlank()) {
                fasesCompletadas = objectMapper.readValue(fasesCompletadasJson,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Boolean>>() {});
            } else {
                fasesCompletadas = new java.util.HashMap<>();
            }
            fasesCompletadas.put(String.valueOf(fase), true);
            proyecto.setCompletitudFasesCompletadas(objectMapper.writeValueAsString(fasesCompletadas));

            proyectoRepositoryPort.save(proyecto);
            return proyectoRepositoryPort.findById(proyecto.getId()).map(p -> {
                initializeLazyCollections(p);
                return mapToResponseDto(p);
            }).orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado tras completar fase: " + proyecto.getId()));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Error al procesar las fases: " + e.getMessage());
        }
    }

// ==================== QUALITY GATE: CIERRE FORZOSO ====================

    @Override
    @Transactional
    public void cerrarForzoso(String id, String gestorUsername, String comentario) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepositoryPort.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROYECTO_NO_ENCONTRADO + normalizedId));

        if (proyecto.esEstadoTerminal()) {
            throw new BadRequestException("El proyecto ya se encuentra cerrado o finalizado.");
        }
        if (comentario == null || comentario.isBlank()) {
            throw new BadRequestException("El comentario es obligatorio para el cierre forzoso.");
        }

        proyecto.cerrarForzoso(gestorUsername, comentario, estadoProyectoConfig("CERRADO_FORZOSO"));
        proyectoRepositoryPort.save(proyecto);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_CLOSED,
                proyecto.getId(),
                gestorUsername,
                java.util.Map.of(
                        KEY_PROJECT_NAME, proyecto.getNombre(),
                        KEY_STATE, proyecto.getEstadoCodigo() != null ? proyecto.getEstadoCodigo() : proyecto.getEstado().name(),
                        "motivo", "Cierre forzoso / extraordinario",
                        "comentario", comentario.trim(),
                        KEY_RECIPIENTS, ProjectNotificationRecipients.resolve(proyecto)
                )));
    }
}


