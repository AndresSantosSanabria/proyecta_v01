package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import com.proyecta.api_gestion.domain.exception.ForbiddenException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.beneficioimpacto.ProyectoBeneficioImpacto;
import com.proyecta.api_gestion.domain.model.enums.EstadoBeneficioImpacto;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoBeneficioImpactoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import com.proyecta.api_gestion.service.support.TextSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProyectoBeneficioImpactoServiceImpl implements ProyectoBeneficioImpactoService {

    private static final Logger log = LoggerFactory.getLogger(ProyectoBeneficioImpactoServiceImpl.class);

    private static final Set<String> GESTOR_ROLES = Set.of("gestor_tic", "gestor_proyectos");
    private static final String ATTR_PROJECT_NAME = "projectName";
    private static final String ATTR_STATE = "state";
    private static final String PATH_BENEFIT_IMPACT = "/beneficio-impacto";
    private static final String ATTR_RECIPIENTS = "recipients";

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final ProyectoBeneficioImpactoRepositoryPort beneficioImpactoRepositoryPort;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final NotificationEventPublisherPort notificationPublisher;
    private final ObjectMapper objectMapper;

    public ProyectoBeneficioImpactoServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                                               ProyectoBeneficioImpactoRepositoryPort beneficioImpactoRepositoryPort,
                                               LocalUserAuthorizationService localUserAuthorizationService,
                                               KeycloakIdentityExtractor identityExtractor,
                                               NotificationEventPublisherPort notificationPublisher,
                                               ObjectMapper objectMapper) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.beneficioImpactoRepositoryPort = beneficioImpactoRepositoryPort;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.identityExtractor = identityExtractor;
        this.notificationPublisher = notificationPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoBeneficioImpactoResponseDTO obtener(String proyectoId, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto registro = beneficioImpactoRepositoryPort.findByProyectoId(proyecto.getId()).orElse(null);
        AuthInfo auth = authInfo(authentication);

        if (registro == null) {
            return toResponse(nuevoPlaceholder(proyecto), auth);
        }

        if (esGestor(auth) && registro.getEstado() == EstadoBeneficioImpacto.PENDIENTE) {
            return toResponse(nuevoPlaceholder(proyecto), auth);
        }

        return toResponse(registro, auth);
    }

    @Override
    @Transactional
    public ProyectoBeneficioImpactoResponseDTO guardar(String proyectoId, ProyectoBeneficioImpactoRequest request, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto registro = beneficioImpactoRepositoryPort.findByProyectoId(proyecto.getId())
                .orElseGet(ProyectoBeneficioImpacto::new);

        AuthInfo auth = authInfo(authentication);
        if (!puedeEditar(auth)) {
            throw new ForbiddenException("Solo el Director de Proyecto puede diligenciar esta informacion.");
        }
        if (registro.getEstado() == EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto ya fue aprobada y no puede modificarse.");
        }

        boolean veniaObservado = registro.getEstado() == EstadoBeneficioImpacto.OBSERVADO;

        registro.setProyecto(proyecto);
        registro.setEstado(EstadoBeneficioImpacto.DILIGENCIADO);
        registro.setPoblacionBeneficiadaDirecta(null);
        registro.setPoblacionBeneficiadaIndirecta(null);
        registro.setPoblacionObjetivo(null);
        registro.setTerritorioBeneficiado(null);
        registro.setBeneficioPrincipal(trim(request.beneficiosValorPublico()));
        registro.setImpactoSocial(trim(request.impactosValorPublico()));
        registro.setImpactoInstitucional(null);
        registro.setImpactoEconomico(null);
        registro.setAlineacionPlanDesarrollo(null);
        registro.setAlineacionPeti(null);
        registro.setMetasContribuidas(null);
        registro.setIndicadorBase(null);
        registro.setIndicadorMeta(null);
        registro.setIndicadorResultado(null);
        registro.setFuenteVerificacion(null);
        registro.setObservaciones(trimToNull(request.observaciones()));

        if (registro.getDiligenciadoEn() == null) {
            registro.setDiligenciadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        }
        registro.setDiligenciadoPor(auth.username());
        registro.setSnapshotJson(buildSnapshotJson(registro));

        ProyectoBeneficioImpacto saved = beneficioImpactoRepositoryPort.save(registro);
        notificarSubmision(proyecto, saved, auth.username(), veniaObservado);
        return toResponse(saved, auth);
    }

    @Override
    @Transactional
    public ProyectoBeneficioImpactoResponseDTO revisar(String proyectoId, boolean aprobado, String observaciones, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto registro = beneficioImpactoRepositoryPort.findByProyectoId(proyecto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe informacion de beneficio e impacto para este proyecto."));

        AuthInfo auth = authInfo(authentication);
        if (!esGestor(auth) && !auth.admin) {
            throw new ForbiddenException("Solo el Gestor puede revisar esta informacion.");
        }
        if (registro.getEstado() == EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto ya fue aprobada y no puede volver a revisarse.");
        }

        if (registro.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO &&
            registro.getEstado() != EstadoBeneficioImpacto.OBSERVADO &&
            registro.getEstado() != EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("Solo se puede revisar informacion en estado DILIGENCIADO.");
        }

        if (!aprobado && (observaciones == null || observaciones.isBlank())) {
            throw new UnprocessableEntityException("Debe ingresar el motivo del rechazo para continuar.");
        }

        EstadoBeneficioImpacto nuevoEstado = aprobado ? EstadoBeneficioImpacto.APROBADO : EstadoBeneficioImpacto.OBSERVADO;
        registro.setEstado(nuevoEstado);
        registro.setRevisadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        registro.setRevisadoPor(auth.username());
        if (!aprobado) {
            registro.setObservaciones(observaciones.trim());
        }

        ProyectoBeneficioImpacto saved = beneficioImpactoRepositoryPort.save(registro);
        notificarRevision(proyecto, saved, auth.username(), aprobado, observaciones);
        return toResponse(saved, auth);
    }

    @Override
    @Transactional
    public void exigirSiCorresponde(String proyectoId, ProyectoAvanceResponseDTO avance, String actorUsername) {
        if (avance == null || avance.entregablesTotal() <= 0 || avance.entregablesConformes() < avance.entregablesTotal()) {
            return;
        }

        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto registro = beneficioImpactoRepositoryPort.findByProyectoId(proyecto.getId()).orElse(null);
        if (registro != null) {
            if (registro.getEstado() != null && registro.getEstado() != EstadoBeneficioImpacto.PENDIENTE) {
                return;
            }
            if (registro.getRequeridoEn() != null) {
                return;
            }
        }

        if (registro == null) {
            registro = new ProyectoBeneficioImpacto();
            registro.setProyecto(proyecto);
            registro.setEstado(EstadoBeneficioImpacto.PENDIENTE);
            registro.setRequeridoEn(LocalDateTime.now(ZoneId.systemDefault()));
            registro.setRequeridoPor(trimToNull(actorUsername));
            beneficioImpactoRepositoryPort.save(registro);
            notificarRequerimiento(proyecto, trimToNull(actorUsername));
            return;
        }

        registro.setRequeridoEn(LocalDateTime.now(ZoneId.systemDefault()));
        registro.setRequeridoPor(trimToNull(actorUsername));
        beneficioImpactoRepositoryPort.save(registro);
        notificarRequerimiento(proyecto, trimToNull(actorUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public void validarDiligenciado(String proyectoId) {
        if (proyectoId == null || proyectoId.isBlank()) {
            throw new UnprocessableEntityException("El proyecto debe registrar la informacion de beneficio e impacto antes del cierre.");
        }

        ProyectoBeneficioImpacto registro = beneficioImpactoRepositoryPort.findByProyectoId(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new UnprocessableEntityException("El proyecto debe registrar la informacion de beneficio e impacto antes del cierre."));

        if (registro.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO
                && registro.getEstado() != EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto es obligatoria y debe estar en estado DILIGENCIADO o APROBADO antes del cierre.");
        }
    }

    private Proyecto cargarProyecto(String proyectoId) {
        if (proyectoId == null || proyectoId.isBlank()) {
            throw new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId);
        }
        return proyectoRepositoryPort.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
    }

    private ProyectoBeneficioImpacto nuevoPlaceholder(Proyecto proyecto) {
        ProyectoBeneficioImpacto registro = new ProyectoBeneficioImpacto();
        registro.setProyecto(proyecto);
        registro.setEstado(EstadoBeneficioImpacto.PENDIENTE);
        return registro;
    }

    private ProyectoBeneficioImpactoResponseDTO toResponse(ProyectoBeneficioImpacto registro, AuthInfo auth) {
        boolean visibleParaGestor = registro.getEstado() != null && registro.getEstado() != EstadoBeneficioImpacto.PENDIENTE;
        boolean editable = registro.getEstado() == null
                || registro.getEstado() == EstadoBeneficioImpacto.PENDIENTE
                || registro.getEstado() == EstadoBeneficioImpacto.OBSERVADO
                || (registro.getEstado() == EstadoBeneficioImpacto.DILIGENCIADO && puedeEditar(auth));

        return new ProyectoBeneficioImpactoResponseDTO(
                registro.getId(),
                registro.getProyecto() != null ? registro.getProyecto().getId() : null,
                registro.getProyecto() != null ? registro.getProyecto().getNombre() : null,
                registro.getEstado() != null ? registro.getEstado().name() : EstadoBeneficioImpacto.PENDIENTE.name(),
                registro.getRequeridoEn(),
                registro.getRequeridoPor(),
                registro.getDiligenciadoEn(),
                registro.getDiligenciadoPor(),
                registro.getRevisadoEn(),
                registro.getRevisadoPor(),
                registro.getBeneficioPrincipal(),
                registro.getImpactoSocial(),
                registro.getObservaciones(),
                registro.getSnapshotJson(),
                registro.getCreatedAt(),
                registro.getUpdatedAt(),
                registro.getId() != null,
                editable,
                visibleParaGestor,
                registro.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO && registro.getEstado() != EstadoBeneficioImpacto.APROBADO
        );
    }

    private boolean puedeEditar(AuthInfo auth) {
        return auth.admin || auth.director;
    }

    private boolean esGestor(AuthInfo auth) {
        return auth.roleCodes.stream().anyMatch(GESTOR_ROLES::contains);
    }

    private AuthInfo authInfo(Authentication authentication) {
        boolean admin = false;
        boolean director = false;
        Set<String> roles = new LinkedHashSet<>();

        try {
            var usuario = localUserAuthorizationService.requireLocalUser(authentication);
            admin = LocalUserAuthorizationService.esAdministrador(usuario);
            String localRole = SecurityRoleCatalog.normalize(usuario.getRolCodigo());
            if (localRole != null) {
                roles.add(localRole);
            }
        } catch (RuntimeException ex) {
            // CWE-390: sin usuario local valido se asume rol no administrador (deny-by-default),
            // pero el fallo del chequeo no debe quedar sin registrar.
            log.warn("No se pudo resolver el usuario local para beneficios/impacto; "
                    + "se mantiene alcance no administrador. causa={}", ex.toString());
        }

        if (authentication != null) {
            authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(SecurityRoleCatalog::normalize)
                    .filter(Objects::nonNull)
                    .forEach(roles::add);
        }

        director = roles.contains("director_proyecto") && roles.stream().noneMatch(SecurityRoleCatalog::isTransversal);

        String username = firstNonBlank(
                identityExtractor.resolveEmail(authentication),
                identityExtractor.resolveUsername(authentication),
                identityExtractor.resolveSub(authentication),
                "sistema"
        );

        return new AuthInfo(username, roles, admin, director);
    }

    private void notificarRequerimiento(Proyecto proyecto, String actorUsername) {
        List<String> recipients = resolverRecipientesProyecto(proyecto);

        if (recipients.isEmpty()) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_BENEFIT_IMPACT_REQUIRED,
                proyecto.getId(),
                actorUsername,
                Map.of(
                        ATTR_PROJECT_NAME, proyecto.getNombre(),
                        ATTR_STATE, EstadoBeneficioImpacto.PENDIENTE.name(),
                        "benefitImpactUrl", "/api/v1/proyectos/" + proyecto.getId() + PATH_BENEFIT_IMPACT,
                        ATTR_RECIPIENTS, recipients
                )));
    }

    private void notificarSubmision(Proyecto proyecto, ProyectoBeneficioImpacto registro, String actorUsername, boolean resubmitted) {
        List<String> recipients = resolverRecipientesProyecto(proyecto);
        if (recipients.isEmpty()) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                resubmitted
                        ? NotificationEventType.PROJECT_BENEFIT_IMPACT_RESUBMITTED
                        : NotificationEventType.PROJECT_BENEFIT_IMPACT_SUBMITTED,
                proyecto.getId(),
                actorUsername,
                Map.of(
                        ATTR_PROJECT_NAME, proyecto.getNombre(),
                        ATTR_STATE, registro.getEstado() != null ? registro.getEstado().name() : EstadoBeneficioImpacto.DILIGENCIADO.name(),
                        "reviewUrl", "/proyectos/" + proyecto.getId() + PATH_BENEFIT_IMPACT,
                        "previousState", resubmitted ? EstadoBeneficioImpacto.OBSERVADO.name() : EstadoBeneficioImpacto.PENDIENTE.name(),
                        ATTR_RECIPIENTS, recipients
                )));
    }

    private void notificarRevision(Proyecto proyecto, ProyectoBeneficioImpacto registro, String actorUsername, boolean aprobado, String observaciones) {
        List<String> recipients = resolverRecipientesProyecto(proyecto);
        if (recipients.isEmpty()) return;

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(ATTR_PROJECT_NAME, proyecto.getNombre());
        attrs.put(ATTR_STATE, registro.getEstado().name());
        attrs.put("aprobado", aprobado ? "APROBADO" : "OBSERVADO");
        attrs.put("observaciones", observaciones != null ? observaciones : "");
        attrs.put("reviewUrl", "/proyectos/" + proyecto.getId() + PATH_BENEFIT_IMPACT);
        attrs.put(ATTR_RECIPIENTS, recipients);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_BENEFIT_IMPACT_REVIEWED,
                proyecto.getId(),
                actorUsername,
                attrs
        ));
    }

    private List<String> resolverRecipientesProyecto(Proyecto proyecto) {
        return ProjectNotificationRecipients.resolve(proyecto);
    }

    private String buildSnapshotJson(ProyectoBeneficioImpacto registro) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("proyectoId", registro.getProyecto() != null ? registro.getProyecto().getId() : null);
            snapshot.put("estado", registro.getEstado() != null ? registro.getEstado().name() : null);
            snapshot.put("poblacionBeneficiadaDirecta", registro.getPoblacionBeneficiadaDirecta());
            snapshot.put("poblacionBeneficiadaIndirecta", registro.getPoblacionBeneficiadaIndirecta());
            snapshot.put("poblacionObjetivo", registro.getPoblacionObjetivo());
            snapshot.put("territorioBeneficiado", registro.getTerritorioBeneficiado());
            snapshot.put("beneficioPrincipal", registro.getBeneficioPrincipal());
            snapshot.put("impactoSocial", registro.getImpactoSocial());
            snapshot.put("impactoInstitucional", registro.getImpactoInstitucional());
            snapshot.put("impactoEconomico", registro.getImpactoEconomico());
            snapshot.put("alineacionPlanDesarrollo", registro.getAlineacionPlanDesarrollo());
            snapshot.put("alineacionPeti", registro.getAlineacionPeti());
            snapshot.put("metasContribuidas", registro.getMetasContribuidas());
            snapshot.put("indicadorBase", registro.getIndicadorBase());
            snapshot.put("indicadorMeta", registro.getIndicadorMeta());
            snapshot.put("indicadorResultado", registro.getIndicadorResultado());
            snapshot.put("fuenteVerificacion", registro.getFuenteVerificacion());
            snapshot.put("observaciones", registro.getObservaciones());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible serializar el snapshot de beneficio e impacto.", ex);
        }
    }

    private String normalizeProjectId(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return TextSupport.trimToNull(value);
    }

    private String firstNonBlank(String... values) {
        return TextSupport.firstNonBlank(values);
    }

    private record AuthInfo(String username, Set<String> roleCodes, boolean admin, boolean director) {}
}
