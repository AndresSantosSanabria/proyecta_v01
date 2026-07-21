package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.exception.UnprocessableEntityException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.beneficioimpacto.ProyectoBeneficioImpacto;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.model.enums.EstadoBeneficioImpacto;
import com.proyecta.api_gestion.repository.ProyectoBeneficioImpactoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProyectoBeneficioImpactoServiceImpl implements ProyectoBeneficioImpactoService {

    private static final Set<String> GESTOR_ROLES = Set.of("gestor_tic", "gestor_proyectos");

    private final ProyectoRepository proyectoRepository;
    private final ProyectoBeneficioImpactoRepository beneficioImpactoRepository;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final NotificationEventPublisherPort notificationPublisher;
    private final ObjectMapper objectMapper;

    public ProyectoBeneficioImpactoServiceImpl(ProyectoRepository proyectoRepository,
                                               ProyectoBeneficioImpactoRepository beneficioImpactoRepository,
                                               SeguridadUsuarioRepository usuarioRepository,
                                               SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
                                               LocalUserAuthorizationService localUserAuthorizationService,
                                               KeycloakIdentityExtractor identityExtractor,
                                               NotificationEventPublisherPort notificationPublisher,
                                               ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.beneficioImpactoRepository = beneficioImpactoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.identityExtractor = identityExtractor;
        this.notificationPublisher = notificationPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoBeneficioImpactoResponseDTO obtener(String proyectoId, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto record = beneficioImpactoRepository.findByProyecto_Id(proyecto.getId()).orElse(null);
        AuthInfo auth = authInfo(authentication);

        if (record == null) {
            if (esGestor(auth)) {
                throw new ForbiddenException("La informacion de beneficio e impacto aun no ha sido diligenciada.");
            }
            return toResponse(nuevoPlaceholder(proyecto), auth);
        }

        if (esGestor(auth) && record.getEstado() == EstadoBeneficioImpacto.PENDIENTE) {
            throw new ForbiddenException("La informacion de beneficio e impacto aun no esta disponible para consulta.");
        }

        return toResponse(record, auth);
    }

    @Override
    @Transactional
    public ProyectoBeneficioImpactoResponseDTO guardar(String proyectoId, ProyectoBeneficioImpactoRequest request, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto record = beneficioImpactoRepository.findByProyecto_Id(proyecto.getId())
                .orElseGet(ProyectoBeneficioImpacto::new);

        AuthInfo auth = authInfo(authentication);
        if (!puedeEditar(auth)) {
            throw new ForbiddenException("Solo el Director de Proyecto puede diligenciar esta informacion.");
        }
        if (record.getEstado() == EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto ya fue aprobada y no puede modificarse.");
        }

        boolean veniaObservado = record.getEstado() == EstadoBeneficioImpacto.OBSERVADO;

        record.setProyecto(proyecto);
        record.setEstado(EstadoBeneficioImpacto.DILIGENCIADO);
        record.setPoblacionBeneficiadaDirecta(null);
        record.setPoblacionBeneficiadaIndirecta(null);
        record.setPoblacionObjetivo(null);
        record.setTerritorioBeneficiado(null);
        record.setBeneficioPrincipal(trim(request.beneficiosValorPublico()));
        record.setImpactoSocial(trim(request.impactosValorPublico()));
        record.setImpactoInstitucional(null);
        record.setImpactoEconomico(null);
        record.setAlineacionPlanDesarrollo(null);
        record.setAlineacionPeti(null);
        record.setMetasContribuidas(null);
        record.setIndicadorBase(null);
        record.setIndicadorMeta(null);
        record.setIndicadorResultado(null);
        record.setFuenteVerificacion(null);
        record.setObservaciones(trimToNull(request.observaciones()));

        if (record.getDiligenciadoEn() == null) {
            record.setDiligenciadoEn(LocalDateTime.now());
        }
        record.setDiligenciadoPor(auth.username());
        record.setSnapshotJson(buildSnapshotJson(record));

        ProyectoBeneficioImpacto saved = beneficioImpactoRepository.save(record);
        notificarSubmision(proyecto, saved, auth.username(), veniaObservado);
        return toResponse(saved, auth);
    }

    @Override
    @Transactional
    public ProyectoBeneficioImpactoResponseDTO revisar(String proyectoId, boolean aprobado, String observaciones, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        ProyectoBeneficioImpacto record = beneficioImpactoRepository.findByProyecto_Id(proyecto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe informacion de beneficio e impacto para este proyecto."));

        AuthInfo auth = authInfo(authentication);
        if (!esGestor(auth) && !auth.admin) {
            throw new ForbiddenException("Solo el Gestor puede revisar esta informacion.");
        }
        if (record.getEstado() == EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto ya fue aprobada y no puede volver a revisarse.");
        }

        if (record.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO &&
            record.getEstado() != EstadoBeneficioImpacto.OBSERVADO &&
            record.getEstado() != EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("Solo se puede revisar informacion en estado DILIGENCIADO.");
        }

        if (!aprobado && (observaciones == null || observaciones.isBlank())) {
            throw new UnprocessableEntityException("Debe ingresar el motivo del rechazo para continuar.");
        }

        EstadoBeneficioImpacto nuevoEstado = aprobado ? EstadoBeneficioImpacto.APROBADO : EstadoBeneficioImpacto.OBSERVADO;
        record.setEstado(nuevoEstado);
        record.setRevisadoEn(LocalDateTime.now());
        record.setRevisadoPor(auth.username());
        if (!aprobado && observaciones != null && !observaciones.isBlank()) {
            record.setObservaciones(observaciones.trim());
        }

        ProyectoBeneficioImpacto saved = beneficioImpactoRepository.save(record);
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
        ProyectoBeneficioImpacto record = beneficioImpactoRepository.findByProyecto_Id(proyecto.getId()).orElse(null);
        if (record != null) {
            if (record.getEstado() != null && record.getEstado() != EstadoBeneficioImpacto.PENDIENTE) {
                return;
            }
            if (record.getRequeridoEn() != null) {
                return;
            }
        }

        if (record == null) {
            record = new ProyectoBeneficioImpacto();
            record.setProyecto(proyecto);
            record.setEstado(EstadoBeneficioImpacto.PENDIENTE);
            record.setRequeridoEn(LocalDateTime.now());
            record.setRequeridoPor(trimToNull(actorUsername));
            beneficioImpactoRepository.save(record);
            notificarRequerimiento(proyecto, trimToNull(actorUsername));
            return;
        }

        record.setRequeridoEn(LocalDateTime.now());
        record.setRequeridoPor(trimToNull(actorUsername));
        beneficioImpactoRepository.save(record);
        notificarRequerimiento(proyecto, trimToNull(actorUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public void validarDiligenciado(String proyectoId) {
        if (proyectoId == null || proyectoId.isBlank()) {
            throw new UnprocessableEntityException("El proyecto debe registrar la informacion de beneficio e impacto antes del cierre.");
        }

        ProyectoBeneficioImpacto record = beneficioImpactoRepository.findByProyecto_Id(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new UnprocessableEntityException("El proyecto debe registrar la informacion de beneficio e impacto antes del cierre."));

        if (record.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO
                && record.getEstado() != EstadoBeneficioImpacto.APROBADO) {
            throw new UnprocessableEntityException("La informacion de beneficio e impacto es obligatoria y debe estar en estado DILIGENCIADO o APROBADO antes del cierre.");
        }
    }

    private Proyecto cargarProyecto(String proyectoId) {
        if (proyectoId == null || proyectoId.isBlank()) {
            throw new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId);
        }
        return proyectoRepository.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
    }

    private ProyectoBeneficioImpacto nuevoPlaceholder(Proyecto proyecto) {
        ProyectoBeneficioImpacto record = new ProyectoBeneficioImpacto();
        record.setProyecto(proyecto);
        record.setEstado(EstadoBeneficioImpacto.PENDIENTE);
        return record;
    }

    private ProyectoBeneficioImpactoResponseDTO toResponse(ProyectoBeneficioImpacto record, AuthInfo auth) {
        boolean visibleParaGestor = record.getEstado() != null && record.getEstado() != EstadoBeneficioImpacto.PENDIENTE;
        boolean editable = puedeEditar(auth) && (record.getEstado() == null
                || record.getEstado() == EstadoBeneficioImpacto.PENDIENTE
                || record.getEstado() == EstadoBeneficioImpacto.OBSERVADO);

        return new ProyectoBeneficioImpactoResponseDTO(
                record.getId(),
                record.getProyecto() != null ? record.getProyecto().getId() : null,
                record.getProyecto() != null ? record.getProyecto().getNombre() : null,
                record.getEstado() != null ? record.getEstado().name() : EstadoBeneficioImpacto.PENDIENTE.name(),
                record.getRequeridoEn(),
                record.getRequeridoPor(),
                record.getDiligenciadoEn(),
                record.getDiligenciadoPor(),
                record.getRevisadoEn(),
                record.getRevisadoPor(),
                record.getBeneficioPrincipal(),
                record.getImpactoSocial(),
                record.getObservaciones(),
                record.getSnapshotJson(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getEstado() != null,
                editable,
                visibleParaGestor,
                record.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO && record.getEstado() != EstadoBeneficioImpacto.APROBADO
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
            admin = usuario.esAdministrador();
            String localRole = SecurityRoleCatalog.normalize(usuario.getRolCodigo());
            if (localRole != null) {
                roles.add(localRole);
            }
        } catch (RuntimeException ignored) {
        }

        if (authentication != null) {
            authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(SecurityRoleCatalog::normalize)
                    .filter(value -> value != null)
                    .forEach(roles::add);
        }

        director = roles.contains("director_proyecto") && !roles.stream().anyMatch(SecurityRoleCatalog::isTransversal);

        String username = firstNonBlank(
                identityExtractor.resolveEmail(authentication),
                identityExtractor.resolveUsername(authentication),
                identityExtractor.resolveSub(authentication),
                "sistema"
        );

        return new AuthInfo(username, roles, admin, director);
    }

    private void notificarRequerimiento(Proyecto proyecto, String actorUsername) {
        List<String> recipients = resolverRecipientesProyecto(proyecto, actorUsername);

        if (recipients.isEmpty()) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_BENEFIT_IMPACT_REQUIRED,
                proyecto.getId(),
                actorUsername,
                Map.of(
                        "projectName", proyecto.getNombre(),
                        "state", EstadoBeneficioImpacto.PENDIENTE.name(),
                        "benefitImpactUrl", "/api/v1/proyectos/" + proyecto.getId() + "/beneficio-impacto",
                        "recipients", recipients
                )));
    }

    private void notificarSubmision(Proyecto proyecto, ProyectoBeneficioImpacto record, String actorUsername, boolean resubmitted) {
        List<String> recipients = resolverRecipientesProyecto(proyecto, actorUsername);
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
                        "projectName", proyecto.getNombre(),
                        "state", record.getEstado() != null ? record.getEstado().name() : EstadoBeneficioImpacto.DILIGENCIADO.name(),
                        "reviewUrl", "/proyectos/" + proyecto.getId() + "/beneficio-impacto",
                        "previousState", resubmitted ? EstadoBeneficioImpacto.OBSERVADO.name() : EstadoBeneficioImpacto.PENDIENTE.name(),
                        "recipients", recipients
                )));
    }

    private void notificarRevision(Proyecto proyecto, ProyectoBeneficioImpacto record, String actorUsername, boolean aprobado, String observaciones) {
        List<String> recipients = resolverRecipientesProyecto(proyecto, actorUsername);
        if (recipients.isEmpty()) return;

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put("projectName", proyecto.getNombre());
        attrs.put("state", record.getEstado().name());
        attrs.put("aprobado", aprobado ? "APROBADO" : "OBSERVADO");
        attrs.put("observaciones", observaciones != null ? observaciones : "");
        attrs.put("reviewUrl", "/proyectos/" + proyecto.getId() + "/beneficio-impacto");
        attrs.put("recipients", recipients);

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_BENEFIT_IMPACT_REVIEWED,
                proyecto.getId(),
                actorUsername,
                attrs
        ));
    }

    private List<String> resolverRecipientesProyecto(Proyecto proyecto, String actorUsername) {
        Set<String> recipients = new LinkedHashSet<>();

        if (proyecto != null) {
            addRecipient(recipients, proyecto.getCorreoDirector());
            if (proyecto.getPatrocinador() != null) {
                addRecipient(recipients, proyecto.getPatrocinador().getEntidad());
            }

            List<SeguridadUsuarioProyecto> asignaciones = usuarioProyectoRepository.findActivasByProyectoIdAndCargoIn(
                    proyecto.getId(),
                    List.of("director_proyecto", "gestor_tic", "gestor_proyectos", "lider_tecnico", "lider tecnico", "director_tecnico", "director tecnico")
            );
            for (SeguridadUsuarioProyecto asignacion : asignaciones) {
                if (asignacion != null && asignacion.getUsuario() != null) {
                    addRecipient(recipients, asignacion.getUsuario().getCorreo());
                }
            }
        }

        usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getActivo() == null || Boolean.TRUE.equals(usuario.getActivo()))
                .filter(usuario -> {
                    String rol = normalize(usuario.getRolCodigo());
                    return rol != null && GESTOR_ROLES.contains(rol);
                })
                .map(usuario -> usuario.getCorreo())
                .forEach(email -> addRecipient(recipients, email));

        if (actorUsername != null) {
            String normalizedActor = trimToNull(actorUsername);
            if (normalizedActor != null) {
                recipients.removeIf(value -> value.equalsIgnoreCase(normalizedActor));
            }
        }

        return recipients.stream().toList();
    }

    private void addRecipient(Set<String> recipients, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.contains("@")) {
            recipients.add(trimmed);
        }
    }

    private String buildSnapshotJson(ProyectoBeneficioImpacto record) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("proyectoId", record.getProyecto() != null ? record.getProyecto().getId() : null);
            snapshot.put("estado", record.getEstado() != null ? record.getEstado().name() : null);
            snapshot.put("poblacionBeneficiadaDirecta", record.getPoblacionBeneficiadaDirecta());
            snapshot.put("poblacionBeneficiadaIndirecta", record.getPoblacionBeneficiadaIndirecta());
            snapshot.put("poblacionObjetivo", record.getPoblacionObjetivo());
            snapshot.put("territorioBeneficiado", record.getTerritorioBeneficiado());
            snapshot.put("beneficioPrincipal", record.getBeneficioPrincipal());
            snapshot.put("impactoSocial", record.getImpactoSocial());
            snapshot.put("impactoInstitucional", record.getImpactoInstitucional());
            snapshot.put("impactoEconomico", record.getImpactoEconomico());
            snapshot.put("alineacionPlanDesarrollo", record.getAlineacionPlanDesarrollo());
            snapshot.put("alineacionPeti", record.getAlineacionPeti());
            snapshot.put("metasContribuidas", record.getMetasContribuidas());
            snapshot.put("indicadorBase", record.getIndicadorBase());
            snapshot.put("indicadorMeta", record.getIndicadorMeta());
            snapshot.put("indicadorResultado", record.getIndicadorResultado());
            snapshot.put("fuenteVerificacion", record.getFuenteVerificacion());
            snapshot.put("observaciones", record.getObservaciones());
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

    private String normalize(String value) {
        return SecurityRoleCatalog.normalize(value);
    }

    private record AuthInfo(String username, Set<String> roleCodes, boolean admin, boolean director) {}
}
