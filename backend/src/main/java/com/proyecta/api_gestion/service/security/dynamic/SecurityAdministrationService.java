package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.dto.security.SeguridadAutorizacionMeDTO;
import com.proyecta.api_gestion.dto.security.SeguridadMatrizPermisosUpdateRequest;
import com.proyecta.api_gestion.dto.security.SeguridadPermisoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioUpdateRequest;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ForbiddenException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.domain.model.security.SeguridadRol;
import com.proyecta.api_gestion.domain.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadRolPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadRolRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.config.ListaParametricaConfigRepositoryPort;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.UserProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.domain.value.SortOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class SecurityAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAdministrationService.class);
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_GESTOR_TIC = "gestor_tic";
    private static final String ROLE_DIRECTOR_PROYECTO = "director_proyecto";
    private static final String ROLE_GESTOR = "gestor";
    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    private final SeguridadRolRepositoryPort rolRepositoryPort;
    private final SeguridadPermisoRepositoryPort permisoRepositoryPort;
    private final SeguridadRolPermisoRepositoryPort rolPermisoRepositoryPort;
    private final SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort;
    private final SeguridadUsuarioPermisoRepositoryPort usuarioPermisoRepositoryPort;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final ListaParametricaConfigRepositoryPort listaParametricaRepositoryPort;
    private final SecurityCatalogCacheService catalogCacheService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final NotificationEventPublisherPort notificationPublisher;
    private final UserProvisioningService userProvisioningService;

    public SecurityAdministrationService(
            SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
            SeguridadRolRepositoryPort rolRepositoryPort,
            SeguridadPermisoRepositoryPort permisoRepositoryPort,
            SeguridadRolPermisoRepositoryPort rolPermisoRepositoryPort,
            SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort,
            SeguridadUsuarioPermisoRepositoryPort usuarioPermisoRepositoryPort,
            ProyectoRepositoryPort proyectoRepositoryPort,
            ListaParametricaConfigRepositoryPort listaParametricaRepositoryPort,
            SecurityCatalogCacheService catalogCacheService,
            KeycloakIdentityExtractor identityExtractor,
            NotificationEventPublisherPort notificationPublisher,
            UserProvisioningService userProvisioningService) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.rolRepositoryPort = rolRepositoryPort;
        this.permisoRepositoryPort = permisoRepositoryPort;
        this.rolPermisoRepositoryPort = rolPermisoRepositoryPort;
        this.usuarioProyectoRepositoryPort = usuarioProyectoRepositoryPort;
        this.usuarioPermisoRepositoryPort = usuarioPermisoRepositoryPort;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.listaParametricaRepositoryPort = listaParametricaRepositoryPort;
        this.catalogCacheService = catalogCacheService;
        this.identityExtractor = identityExtractor;
        this.notificationPublisher = notificationPublisher;
        this.userProvisioningService = userProvisioningService;
    }

    @Transactional
    public PageResult<SeguridadUsuarioDTO> listarUsuarios(String search, String rol, PageQuery query) {
        backfillUsuariosSinRol();
        return usuarioRepositoryPort.search(search, rol, query).map(this::toUsuarioDTO);
    }

    @Transactional
    public SeguridadUsuario sincronizarUsuarioAutenticado(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        String email = normalizeText(identityExtractor.resolveEmail(authentication));
        String displayName = normalizeText(identityExtractor.resolveDisplayName(authentication));
        String keycloakSub = normalizeText(identityExtractor.resolveSub(authentication));

        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        SeguridadUsuario usuario = findExistingUser(keycloakSub, email, username);

        boolean isNewUser = usuario == null;
        if (isNewUser) {
            usuario = createNewUsuario(username, email, displayName, keycloakSub, authentication);
        } else {
            boolean huboCambios = applyExistingUsuarioUpdates(usuario, authentication, keycloakSub, displayName, email, username);
            if (!huboCambios) {
                return usuario;
            }
        }

        applyPendingInitialRole(usuario, authentication, isNewUser);
        return usuarioRepositoryPort.save(usuario);
    }

    private SeguridadUsuario createNewUsuario(String username, String email, String displayName, String keycloakSub, Authentication authentication) {
        SeguridadUsuario nuevo = new SeguridadUsuario();
        nuevo.setUsername(username);
        nuevo.setKeycloakSub(keycloakSub != null ? keycloakSub : username);
        nuevo.setNombre(displayName != null ? displayName : username);
        nuevo.setCorreo(email != null ? email : username);
        nuevo.setDependencia(identityExtractor.resolveDependencia(authentication));
        nuevo.setActivo(true);
        return nuevo;
    }

    private boolean applyExistingUsuarioUpdates(SeguridadUsuario usuario, Authentication authentication, String keycloakSub, String displayName, String email, String username) {
        boolean changed = false;
        changed |= applyTextUpdateIfDifferent(keycloakSub, usuario.getKeycloakSub(), usuario::setKeycloakSub);
        changed |= applyTextUpdateIfDifferent(displayName, usuario.getNombre(), usuario::setNombre);
        changed |= applyTextUpdateIfDifferent(email, usuario.getCorreo(), usuario::setCorreo);
        changed |= applyTextUpdateIfDifferent(username, usuario.getUsername(), usuario::setUsername);

        String dependencia = normalizeText(identityExtractor.resolveDependencia(authentication));
        changed |= applyTextUpdateIfDifferent(dependencia, usuario.getDependencia(), usuario::setDependencia);

        if (usuario.getActivo() == null) {
            usuario.setActivo(true);
            changed = true;
        }
        return changed;
    }

    private boolean applyTextUpdateIfDifferent(String incoming, String current, Consumer<String> setter) {
        if (incoming != null && !incoming.equalsIgnoreCase(normalizeText(current))) {
            setter.accept(incoming);
            return true;
        }
        return false;
    }

    private boolean applyPendingInitialRole(SeguridadUsuario usuario, Authentication authentication, boolean isNewUser) {
        if (usuario == null) {
            return false;
        }
        if (!isNewUser && usuario.getRolCodigo() != null && !usuario.getRolCodigo().isBlank()) {
            return false;
        }

        String resolvedCode = resolveInitialRoleForNewUser(usuario, authentication);
        Optional<SeguridadRol> rol = rolRepositoryPort.findByCodigoIgnoreCase(resolvedCode);

        if (rol.isEmpty()) {
            return false;
        }

        SeguridadRol r = rol.get();
        if (r.getCodigo().equalsIgnoreCase(normalizeText(usuario.getRolCodigo()))) {
            return false;
        }

        usuario.setRolCodigo(r.getCodigo());
        usuario.setRolNombre(r.getNombre());
        return true;
    }

    private SeguridadUsuario findExistingUser(String keycloakSub, String email, String username) {
        if (keycloakSub != null && !keycloakSub.isBlank()) {
            SeguridadUsuario bySub = usuarioRepositoryPort.findByKeycloakSubIgnoreCase(keycloakSub).orElse(null);
            if (bySub != null) return bySub;
        }

        if (email != null && !email.isBlank()) {
            SeguridadUsuario byEmail = usuarioRepositoryPort.findByCorreoIgnoreCase(email).orElse(null);
            if (byEmail != null) return byEmail;
        }

        if (username != null && !username.isBlank()) {
            SeguridadUsuario byUsername = usuarioRepositoryPort.findByUsernameIgnoreCase(username).orElse(null);
            if (byUsername != null) return byUsername;
        }

        return null;
    }

    @Transactional
    public SeguridadUsuarioDTO actualizarUsuario(SeguridadUsuarioUpdateRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new BadRequestException("El username del usuario es obligatorio.");
        }

        String username = normalizeText(request.username());
        SeguridadUsuario usuario = usuarioRepositoryPort.findByUsernameIgnoreCase(username)
                .orElseGet(() -> createUsuarioFromRequest(request, username));

        applyUsuarioUpdateRequest(usuario, request);

        SeguridadUsuario saved = usuarioRepositoryPort.save(usuario);
        catalogCacheService.evictAll();
        return toUsuarioDTO(saved);
    }

    private SeguridadUsuario createUsuarioFromRequest(SeguridadUsuarioUpdateRequest request, String username) {
        SeguridadUsuario nuevo = new SeguridadUsuario();
        nuevo.setUsername(username);
        String correo = normalizeText(request.correo());
        String subKeycloakInicial = correo != null ? correo : username;
        nuevo.setKeycloakSub(normalizeText(request.keycloakSub()) != null
                ? normalizeText(request.keycloakSub())
                : subKeycloakInicial);
        nuevo.setNombre(normalizeText(request.nombre()) != null ? normalizeText(request.nombre()) : username);
        nuevo.setCorreo(correo != null ? correo : username);
        nuevo.setDependencia(normalizeText(request.dependencia()));
        nuevo.setActivo(request.activo() == null || request.activo());
        return nuevo;
    }

    private void applyUsuarioUpdateRequest(SeguridadUsuario usuario, SeguridadUsuarioUpdateRequest request) {
        String correo = normalizeText(request.correo());
        String keycloakSub = normalizeText(request.keycloakSub());
        if (keycloakSub != null) {
            usuario.setKeycloakSub(keycloakSub);
        } else if (correo != null) {
            usuario.setKeycloakSub(correo);
        }
        if (normalizeText(request.nombre()) != null) {
            usuario.setNombre(normalizeText(request.nombre()));
        }
        if (correo != null) {
            usuario.setCorreo(correo);
        }
        usuario.setDependencia(normalizeText(request.dependencia()));
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        String rolCodigo = normalizeText(request.rol());
        if (rolCodigo != null) {
            SeguridadRol rol = rolRepositoryPort.findByCodigoIgnoreCase(rolCodigo).orElse(null);
            usuario.setRolCodigo(rolCodigo);
            usuario.setRolNombre(rol != null ? rol.getNombre() : rolCodigo);
        }
    }

    public List<SeguridadPermisoDTO> listarPermisos() {
        return permisoRepositoryPort.findAllByActivoTrueOrderByCodigoAsc().stream()
                .map(this::toPermisoDTO)
                .toList();
    }

    public List<SeguridadRolDTO> listarRolesConPermisos(boolean includeInactive) {
        List<SeguridadRol> roles = includeInactive
                ? rolRepositoryPort.findAll(List.of(new SortOrder("codigo", true)))
                : rolRepositoryPort.findAllByActivoTrueOrderByCodigoAsc();
        List<SeguridadRolPermiso> relaciones = rolPermisoRepositoryPort.findAllActiveWithRelations();

        Map<Long, List<SeguridadPermisoDTO>> permisosPorRol = relaciones.stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getRol().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(relation -> toPermisoDTO(relation.getPermiso()), Collectors.toList())));

        return roles.stream()
                .map(rol -> new SeguridadRolDTO(
                        rol.getId(),
                        rol.getCodigo(),
                        rol.getNombre(),
                        rol.getDescripcion(),
                        rol.getTransversal(),
                        rol.getActivo(),
                        permisosPorRol.getOrDefault(rol.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SeguridadRolDTO guardarRol(SeguridadRolRequest request) {
        if (request == null || normalizeText(request.codigo()) == null || normalizeText(request.nombre()) == null) {
            throw new BadRequestException("codigo y nombre del rol son obligatorios.");
        }

        String codigo = normalizeRole(request.codigo());
        String nombre = normalizeText(request.nombre());
        String descripcion = normalizeText(request.descripcion());
        boolean transversal = Boolean.TRUE.equals(request.transversal());
        boolean activo = request.activo() == null || request.activo();

        SeguridadRol rol = rolRepositoryPort.findByCodigoIgnoreCase(codigo).orElseGet(SeguridadRol::new);
        if (rol.getId() != null && SecurityRoleCatalog.isProtected(rol.getCodigo()) && !activo) {
            throw new ForbiddenException("No se puede desactivar un rol base del sistema.");
        }

        rol.setCodigo(codigo);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setTransversal(transversal);
        rol.setActivo(activo);

        SeguridadRol saved = rolRepositoryPort.save(rol);
        catalogCacheService.evictAll();
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.SECURITY_ROLE_UPDATED,
                null,
                "system",
                java.util.Map.of(
                        "roleCode", saved.getCodigo(),
                        "recipients", List.of()
                )));
        return toRolDTO(saved, new LinkedHashMap<>());
    }

    @Transactional
    public SeguridadRolDTO eliminarRol(String codigo) {
        String normalized = normalizeRole(codigo);
        if (normalized == null) {
            throw new BadRequestException("El codigo del rol es obligatorio.");
        }

        SeguridadRol rol = rolRepositoryPort.findByCodigoIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + normalized));

        if (SecurityRoleCatalog.isProtected(rol.getCodigo())) {
            throw new ForbiddenException("No se puede eliminar o desactivar un rol base del sistema.");
        }

        rol.setActivo(false);
        SeguridadRol saved = rolRepositoryPort.save(rol);
        catalogCacheService.evictAll();
        return toRolDTO(saved, new LinkedHashMap<>());
    }

    @Transactional
    public void actualizarMatriz(SeguridadMatrizPermisosUpdateRequest request) {
        if (request == null || request.matriz() == null) {
            throw new BadRequestException("La matriz de permisos es obligatoria.");
        }

        Set<String> roleCodes = request.matriz().keySet().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (roleCodes.isEmpty()) {
            throw new BadRequestException("Debe enviar al menos un rol para actualizar la matriz.");
        }

        Map<String, SeguridadRol> roles = rolRepositoryPort.findAllByActivoTrueOrderByCodigoAsc().stream()
                .filter(rol -> rol.getCodigo() != null)
                .filter(rol -> roleCodes.contains(rol.getCodigo().trim().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(rol -> rol.getCodigo().trim().toLowerCase(Locale.ROOT), rol -> rol));

        List<SeguridadRolPermiso> nuevasRelaciones = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : request.matriz().entrySet()) {
            String roleCode = normalizeRole(entry.getKey());
            SeguridadRol rol = roles.get(roleCode);
            if (rol == null) {
                throw new ResourceNotFoundException("Rol no encontrado: " + entry.getKey());
            }

            List<String> permissionCodes = entry.getValue() == null ? List.of() : entry.getValue();
            for (String permissionCode : permissionCodes) {
                String normalizedPermission = normalizePermission(permissionCode);
                if (normalizedPermission == null) {
                    continue;
                }

                SeguridadPermiso permiso = permisoRepositoryPort.findByCodigoIgnoreCase(normalizedPermission)
                        .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado: " + normalizedPermission));

                SeguridadRolPermiso relation = new SeguridadRolPermiso();
                relation.setRol(rol);
                relation.setPermiso(permiso);
                relation.setActivo(true);
                nuevasRelaciones.add(relation);
            }
        }

        rolPermisoRepositoryPort.deleteAllInBatch(rolPermisoRepositoryPort.findAll());
        rolPermisoRepositoryPort.saveAll(nuevasRelaciones);
        catalogCacheService.evictAll();
    }

    @Transactional
    public SeguridadUsuarioProyectoDTO asignarUsuarioProyecto(SeguridadUsuarioProyectoRequest request) {
        if (request == null) {
            throw new BadRequestException("La solicitud de asignación es obligatoria.");
        }

        String username = normalizeText(request.username());
        String proyectoId = normalizeText(request.proyectoId());
        String cargo = normalizeText(request.cargo());

        if (username == null || proyectoId == null || cargo == null) {
            throw new BadRequestException("username, proyectoId y cargo son obligatorios.");
        }

        String cargoNormalizado = cargo.toUpperCase(Locale.ROOT);

        validarCargoAsignacion(cargoNormalizado);

        SeguridadUsuario usuario = usuarioRepositoryPort.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        String rolUsuario = normalizeRole(usuario.getRolCodigo());
        if (!isDirectorRole(rolUsuario)) {
            throw new BadRequestException("Solo los usuarios con rol Director de Proyecto pueden ser asignados a un proyecto.");
        }

        SeguridadUsuarioProyecto assignment = findAssignmentFor(username, proyectoId, cargoNormalizado);
        assignment.setUsuario(usuario);
        assignment.setProyectoId(proyectoId);
        assignment.setCargo(cargoNormalizado);
        assignment.setActivo(true);
        assignment.setFechaAsignacion(LocalDateTime.now(ZoneId.systemDefault()));
        SeguridadUsuarioProyecto saved = usuarioProyectoRepositoryPort.save(assignment);

        if (isDirectorCargo(cargoNormalizado)) {
            actualizarDirectorProyecto(proyectoId, usuario);
        }

        catalogCacheService.evictAll();
        publicarNotificacionAsignacion(usuario, proyectoId, username, cargoNormalizado);
        return toUsuarioProyectoDTO(saved);
    }

    private void validarCargoAsignacion(String cargoNormalizado) {
        List<String> cargosPermitidos = listarCargosAsignacion();
        if (cargosPermitidos.isEmpty()) {
            throw new BadRequestException("No hay cargos de asignacion configurados en el sistema.");
        }
        if (cargosPermitidos.stream().noneMatch(value -> value.equalsIgnoreCase(cargoNormalizado))) {
            throw new BadRequestException("El cargo enviado no esta permitido por la configuracion del sistema.");
        }
    }

    private SeguridadUsuarioProyecto findAssignmentFor(String username, String proyectoId, String cargoNormalizado) {
        if (isDirectorCargo(cargoNormalizado)) {
            return usuarioProyectoRepositoryPort
                    .findActiveDirectorAssignmentsByProyectoId(proyectoId)
                    .stream()
                    .findFirst()
                    .orElseGet(SeguridadUsuarioProyecto::new);
        }
        return usuarioProyectoRepositoryPort
                .findByUsuarioUsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(
                        username,
                        proyectoId,
                        cargoNormalizado)
                .orElseGet(SeguridadUsuarioProyecto::new);
    }

    private void actualizarDirectorProyecto(String proyectoId, SeguridadUsuario usuario) {
        proyectoRepositoryPort.findById(proyectoId).ifPresent(proyecto -> {
            proyecto.setDirector(usuario.getNombre());
            proyecto.setCorreoDirector(usuario.getCorreo());
            proyecto.setDirectorUsuario(usuario);
            proyectoRepositoryPort.save(proyecto);
        });
    }

    private void publicarNotificacionAsignacion(SeguridadUsuario usuario, String proyectoId, String username, String cargoNormalizado) {
        Proyecto proyectoAsign = proyectoRepositoryPort.findById(proyectoId).orElse(null);
        String projectNameAsign = proyectoAsign != null && proyectoAsign.getNombre() != null ? proyectoAsign.getNombre() : "";
        java.util.List<String> assignmentRecipients = new java.util.ArrayList<>();
        if (usuario.getCorreo() != null && !usuario.getCorreo().isBlank()) {
            assignmentRecipients.add(usuario.getCorreo().trim());
        }
        if (proyectoAsign != null) {
            assignmentRecipients.addAll(ProjectNotificationRecipients.resolve(proyectoAsign));
        }
        assignmentRecipients = assignmentRecipients.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (!assignmentRecipients.isEmpty()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.PROJECT_ASSIGNMENT_CREATED,
                    proyectoId,
                    username,
                    java.util.Map.of(
                            "assignedUsername", username,
                            "assignmentRole", cargoNormalizado,
                            "projectName", projectNameAsign,
                            "recipients", assignmentRecipients
                    )));
        }
    }

    public List<SeguridadUsuarioProyectoDTO> listarAsignaciones(String username) {
        return usuarioProyectoRepositoryPort.findByUsername(username).stream()
                .map(this::toUsuarioProyectoDTO)
                .toList();
    }

    public List<String> listarCargosAsignacion() {
        return listaParametricaRepositoryPort.findByListaClaveAndActivoTrueOrderByOrdenAsc("CARGO_ASIGNACION")
                .stream()
                .map(item -> item.getItemCodigo() == null ? null : item.getItemCodigo().trim())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    @Transactional
    public SeguridadAutorizacionMeDTO getAuthorizationFor(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        String nombre = identityExtractor.resolveDisplayName(authentication);

        boolean isAdminFromJwt = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));

        // Upsert: crea o actualiza el usuario con REQUIRES_NEW (commit garantizado).
        SeguridadUsuario usuario = userProvisioningService.upsert(authentication);

        // Fallback: si upsert retornó null (muy improbable), intentar carga directa.
        if (usuario == null) {
            usuario = usuarioRepositoryPort.findByUsernameIgnoreCase(username.trim())
                    .orElseThrow(() -> new ForbiddenException("Usuario no encontrado tras aprovisionamiento: " + username));
        }

        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        if (isAdminFromJwt) {
            roleCodes.add(ROLE_ADMIN);
            permissions.addAll(catalogCacheService.getPermissionsForAllRoles());
        } else {
            String dbRoleCode = usuario.getRolCodigo();
            if (dbRoleCode != null && !dbRoleCode.isBlank()) {
                roleCodes.add(dbRoleCode.trim().toLowerCase(Locale.ROOT));
                permissions.addAll(catalogCacheService.getPermissionsForRoles(roleCodes));
            }

            // Apply user-level overrides (granted add, denied remove)
            Set<String> grantedOverrides = usuarioPermisoRepositoryPort.findGrantedPermissionCodesByUsuarioId(usuario.getId());
            Set<String> deniedOverrides = usuarioPermisoRepositoryPort.findDeniedPermissionCodesByUsuarioId(usuario.getId());
            permissions.addAll(grantedOverrides);
            permissions.removeAll(deniedOverrides);
            log.info("[AuthzDebug] user={}, userId={}, dbRoleCode={}, finalCount={}, grantedOverrides={}, deniedOverrides={}",
                    username, usuario.getId(), dbRoleCode, permissions.size(), grantedOverrides.size(), deniedOverrides.size());
        }

        boolean transversal = roleCodes.stream().anyMatch(SecurityRoleCatalog::isTransversal);
        boolean administradorLocal = isAdminFromJwt;

        usuario.setUltimoAcceso(LocalDateTime.now(ZoneId.systemDefault()));
        usuarioRepositoryPort.save(usuario);

        List<String> projects = catalogCacheService.getProjectsForUser(username);
        administradorLocal = administradorLocal || LocalUserAuthorizationService.esAdministrador(usuario);

        if (log.isDebugEnabled()) {
            log.debug("[AuthzDebug] user={}, roles={}, permissionsCount={}, projectsCount={}",
                    com.proyecta.api_gestion.infrastructure.LogSanitizer.clean(username),
                    roleCodes, permissions.size(), projects.size());
        }

        return new SeguridadAutorizacionMeDTO(
                username,
                nombre != null ? nombre : username,
                roleCodes.stream().toList(),
                permissions.stream().toList(),
                administradorLocal,
                transversal,
                projects);
    }

    private SeguridadUsuarioDTO toUsuarioDTO(SeguridadUsuario usuario) {
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
                usuario.getUltimoAcceso());
    }

    private SeguridadPermisoDTO toPermisoDTO(SeguridadPermiso permiso) {
        return new SeguridadPermisoDTO(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getNombre(),
                permiso.getDescripcion(),
                permiso.getActivo());
    }

    private SeguridadRolDTO toRolDTO(SeguridadRol rol, Map<Long, List<SeguridadPermisoDTO>> permisosPorRol) {
        return new SeguridadRolDTO(
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getTransversal(),
                rol.getActivo(),
                permisosPorRol.getOrDefault(rol.getId(), List.of()));
    }

    private SeguridadUsuarioProyectoDTO toUsuarioProyectoDTO(SeguridadUsuarioProyecto item) {
        Proyecto proyecto = item.getProyectoId() != null ? proyectoRepositoryPort.findById(item.getProyectoId()).orElse(null) : null;
        return new SeguridadUsuarioProyectoDTO(
                item.getId(),
                item.getUsuario() != null ? item.getUsuario().getId() : null,
                item.getUsuario() != null ? item.getUsuario().getUsername() : null,
                item.getUsuario() != null ? item.getUsuario().getNombre() : null,
                item.getProyectoId(),
                proyecto != null ? proyecto.getId() : null,
                proyecto != null ? proyecto.getNombre() : null,
                item.getCargo(),
                item.getActivo(),
                item.getFechaAsignacion());
    }

    private String normalizeRole(String value) {
        String normalized = normalizeText(value);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizePermission(String value) {
        String normalized = normalizeText(value);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private boolean isDirectorCargo(String cargo) {
        String normalized = normalizeText(cargo);
        if (normalized == null) {
            return false;
        }

        return "DIRECTOR_PROYECTO".equalsIgnoreCase(normalized)
                || "LIDER_TECNICO".equalsIgnoreCase(normalized)
                || "DIRECTOR_TECNICO".equalsIgnoreCase(normalized);
    }

    private boolean isDirectorRole(String rolUsuario) {
        String normalized = normalizeRole(rolUsuario);
        if (normalized == null) {
            return false;
        }

        return ROLE_DIRECTOR_PROYECTO.equals(normalized)
                || "lider_tecnico".equals(normalized)
                || "director_tecnico".equals(normalized);
    }

    protected void backfillUsuariosSinRol() {
        List<SeguridadUsuario> usuarios = usuarioRepositoryPort.findAll();
        if (usuarios.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (SeguridadUsuario usuario : usuarios) {
            if (aplicarRolInicialPendiente(usuario)) {
                changed = true;
            }
        }

        if (changed) {
            usuarioRepositoryPort.saveAll(usuarios);
        }
    }

    private boolean aplicarRolInicialPendiente(SeguridadUsuario usuario) {
        String rolActual = normalizeText(usuario.getRolCodigo());
        if (rolActual != null && !rolActual.isBlank()) {
            return false;
        }

        String resolvedCode = resolveInitialRoleForNewUser(usuario, null);
        Optional<SeguridadRol> resolvedRole = rolRepositoryPort.findByCodigoIgnoreCase(resolvedCode);
        if (resolvedRole.isEmpty()) {
            return false;
        }

        SeguridadRol rol = resolvedRole.get();
        boolean localChange = false;

        if (!rol.getCodigo().equalsIgnoreCase(normalizeText(usuario.getRolCodigo()))) {
            usuario.setRolCodigo(rol.getCodigo());
            localChange = true;
        }

        if (!rol.getNombre().equalsIgnoreCase(normalizeText(usuario.getRolNombre()))) {
            usuario.setRolNombre(rol.getNombre());
            localChange = true;
        }

        return localChange;
    }

    private String resolveInitialRoleForNewUser(SeguridadUsuario usuario, Authentication authentication) {
        if (authentication != null) {
            boolean isGestor = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(value -> value.substring(5).toLowerCase(Locale.ROOT))
                    .anyMatch(role -> role.equals(ROLE_GESTOR_TIC)
                            || role.equals(ROLE_GESTOR)
                            || role.equals("gestor_proyectos")
                            || role.contains(ROLE_GESTOR));

            if (isGestor) {
                return ROLE_GESTOR_TIC;
            }
        }

        if (usuario != null) {
            String rolCodigo = normalizeText(usuario.getRolCodigo());
            if (rolCodigo != null && rolCodigo.toLowerCase(Locale.ROOT).contains(ROLE_GESTOR)) {
                return ROLE_GESTOR_TIC;
            }
        }

        return "visualizador";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
