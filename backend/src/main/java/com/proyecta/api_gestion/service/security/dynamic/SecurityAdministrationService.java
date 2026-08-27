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
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.RolConfig;
import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.model.security.SeguridadRol;
import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.security.SeguridadPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.config.ListaParametricaConfigRepository;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.UserProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

@Service
public class SecurityAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAdministrationService.class);
    private final SeguridadUsuarioRepository usuarioRepository;
    private final SeguridadRolRepository rolRepository;
    private final SeguridadPermisoRepository permisoRepository;
    private final SeguridadRolPermisoRepository rolPermisoRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final SeguridadUsuarioPermisoRepository usuarioPermisoRepository;
    private final ProyectoRepository proyectoRepository;
    private final SystemParameterService systemParameterService;
    private final ListaParametricaConfigRepository listaParametricaRepository;
    private final SecurityCatalogCacheService catalogCacheService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final NotificationEventPublisherPort notificationPublisher;
    private final UserProvisioningService userProvisioningService;

    public SecurityAdministrationService(
            SeguridadUsuarioRepository usuarioRepository,
            SeguridadRolRepository rolRepository,
            SeguridadPermisoRepository permisoRepository,
            SeguridadRolPermisoRepository rolPermisoRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
            SeguridadUsuarioPermisoRepository usuarioPermisoRepository,
            ProyectoRepository proyectoRepository,
            SystemParameterService systemParameterService,
            ListaParametricaConfigRepository listaParametricaRepository,
            SecurityCatalogCacheService catalogCacheService,
            KeycloakIdentityExtractor identityExtractor,
            LocalUserAuthorizationService localUserAuthorizationService,
            NotificationEventPublisherPort notificationPublisher,
            UserProvisioningService userProvisioningService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.proyectoRepository = proyectoRepository;
        this.systemParameterService = systemParameterService;
        this.listaParametricaRepository = listaParametricaRepository;
        this.catalogCacheService = catalogCacheService;
        this.identityExtractor = identityExtractor;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.notificationPublisher = notificationPublisher;
        this.userProvisioningService = userProvisioningService;
    }

    public Page<SeguridadUsuarioDTO> listarUsuarios(String search, String rol, Pageable pageable) {
        backfillUsuariosSinRol();
        return usuarioRepository.search(search, rol, pageable).map(this::toUsuarioDTO);
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
        boolean shouldSave = false;

        boolean isNewUser = usuario == null;
        if (isNewUser) {
            usuario = new SeguridadUsuario();
            usuario.setUsername(username);
            usuario.setKeycloakSub(keycloakSub != null ? keycloakSub : username);
            usuario.setNombre(displayName != null ? displayName : username);
            usuario.setCorreo(email != null ? email : username);
            usuario.setDependencia(identityExtractor.resolveDependencia(authentication));
            usuario.setActivo(true);
            shouldSave = true;
        } else {
            if (keycloakSub != null && !keycloakSub.equalsIgnoreCase(normalizeText(usuario.getKeycloakSub()))) {
                usuario.setKeycloakSub(keycloakSub);
                shouldSave = true;
            }
            if (displayName != null && !displayName.equalsIgnoreCase(normalizeText(usuario.getNombre()))) {
                usuario.setNombre(displayName);
                shouldSave = true;
            }
            if (email != null && !email.equalsIgnoreCase(normalizeText(usuario.getCorreo()))) {
                usuario.setCorreo(email);
                shouldSave = true;
            }
            if (!username.equalsIgnoreCase(normalizeText(usuario.getUsername()))) {
                usuario.setUsername(username);
                shouldSave = true;
            }

            String dependencia = normalizeText(identityExtractor.resolveDependencia(authentication));
            if (dependencia != null && !dependencia.equalsIgnoreCase(normalizeText(usuario.getDependencia()))) {
                usuario.setDependencia(dependencia);
                shouldSave = true;
            }

            if (usuario.getActivo() == null) {
                usuario.setActivo(true);
                shouldSave = true;
            }

            if (!shouldSave) {
                return usuario;
            }
        }

        Optional<SeguridadRol> resolvedRole = resolveRoleForSecurityUser(usuario, authentication, true);
        if (resolvedRole.isEmpty() && (isNewUser || usuario.getRolCodigo() == null || usuario.getRolCodigo().isBlank())) {
            resolvedRole = rolRepository.findByCodigoIgnoreCase("consulta");
        }

        if (resolvedRole.isPresent()) {
            SeguridadRol rol = resolvedRole.get();
            if (!rol.getCodigo().equalsIgnoreCase(normalizeText(usuario.getRolCodigo()))) {
                usuario.setRolCodigo(rol.getCodigo());
                usuario.setRolNombre(rol.getNombre());
                shouldSave = true;
            }
        }

        return shouldSave ? usuarioRepository.save(usuario) : usuario;
    }

    private SeguridadUsuario findExistingUser(String keycloakSub, String email, String username) {
        if (keycloakSub != null && !keycloakSub.isBlank()) {
            SeguridadUsuario bySub = usuarioRepository.findByKeycloakSubIgnoreCase(keycloakSub).orElse(null);
            if (bySub != null) return bySub;
        }

        if (email != null && !email.isBlank()) {
            SeguridadUsuario byEmail = usuarioRepository.findByCorreoIgnoreCase(email).orElse(null);
            if (byEmail != null) return byEmail;
        }

        if (username != null && !username.isBlank()) {
            SeguridadUsuario byUsername = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
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
        SeguridadUsuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> {
                    SeguridadUsuario nuevo = new SeguridadUsuario();
                    nuevo.setUsername(username);
                    String correo = normalizeText(request.correo());
                    nuevo.setKeycloakSub(normalizeText(request.keycloakSub()) != null
                            ? normalizeText(request.keycloakSub())
                            : correo != null ? correo : username);
                    nuevo.setNombre(normalizeText(request.nombre()) != null ? normalizeText(request.nombre()) : username);
                    nuevo.setCorreo(correo != null ? correo : username);
                    nuevo.setDependencia(normalizeText(request.dependencia()));
                    nuevo.setActivo(request.activo() == null || request.activo());
                    return nuevo;
                });

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
            SeguridadRol rol = rolRepository.findByCodigoIgnoreCase(rolCodigo).orElse(null);
            usuario.setRolCodigo(rolCodigo);
            usuario.setRolNombre(rol != null ? rol.getNombre() : rolCodigo);
        }

        SeguridadUsuario saved = usuarioRepository.save(usuario);
        catalogCacheService.evictAll();
        return toUsuarioDTO(saved);
    }

    public List<SeguridadPermisoDTO> listarPermisos() {
        return permisoRepository.findAllByActivoTrueOrderByCodigoAsc().stream()
                .map(this::toPermisoDTO)
                .toList();
    }

    public List<SeguridadRolDTO> listarRolesConPermisos(boolean includeInactive) {
        List<SeguridadRol> roles = includeInactive
                ? rolRepository.findAll(Sort.by(Sort.Direction.ASC, "codigo"))
                : rolRepository.findAllByActivoTrueOrderByCodigoAsc();
        List<SeguridadRolPermiso> relaciones = rolPermisoRepository.findAllActiveWithRelations();

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

        SeguridadRol rol = rolRepository.findByCodigoIgnoreCase(codigo).orElseGet(SeguridadRol::new);
        boolean isNew = rol.getId() == null;
        if (rol.getId() != null && SecurityRoleCatalog.isProtected(rol.getCodigo()) && !activo) {
            throw new ForbiddenException("No se puede desactivar un rol base del sistema.");
        }

        rol.setCodigo(codigo);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setTransversal(transversal);
        rol.setActivo(activo);

        SeguridadRol saved = rolRepository.save(rol);
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

        SeguridadRol rol = rolRepository.findByCodigoIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + normalized));

        if (SecurityRoleCatalog.isProtected(rol.getCodigo())) {
            throw new ForbiddenException("No se puede eliminar o desactivar un rol base del sistema.");
        }

        rol.setActivo(false);
        SeguridadRol saved = rolRepository.save(rol);
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

        Map<String, SeguridadRol> roles = rolRepository.findAllByActivoTrueOrderByCodigoAsc().stream()
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

                SeguridadPermiso permiso = permisoRepository.findByCodigoIgnoreCase(normalizedPermission)
                        .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado: " + normalizedPermission));

                SeguridadRolPermiso relation = new SeguridadRolPermiso();
                relation.setRol(rol);
                relation.setPermiso(permiso);
                relation.setActivo(true);
                nuevasRelaciones.add(relation);
            }
        }

        rolPermisoRepository.deleteAllInBatch(rolPermisoRepository.findAll());
        rolPermisoRepository.saveAll(nuevasRelaciones);
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

        List<String> cargosPermitidos = listarCargosAsignacion();
        if (cargosPermitidos.isEmpty()) {
            throw new BadRequestException("No hay cargos de asignacion configurados en el sistema.");
        }
        if (cargosPermitidos.stream().noneMatch(value -> value.equalsIgnoreCase(cargoNormalizado))) {
            throw new BadRequestException("El cargo enviado no esta permitido por la configuracion del sistema.");
        }

        SeguridadUsuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        if (isDirectorCargo(cargoNormalizado)) {
            String rolUsuario = normalizeRole(usuario.getRolCodigo());
            if (!isDirectorRole(rolUsuario)) {
                throw new BadRequestException("El usuario seleccionado no tiene el rol Director de Proyecto.");
            }
        }

        SeguridadUsuarioProyecto assignment;
        if (isDirectorCargo(cargoNormalizado)) {
            assignment = usuarioProyectoRepository
                    .findActiveDirectorAssignmentsByProyectoId(proyectoId)
                    .stream()
                    .findFirst()
                    .orElseGet(SeguridadUsuarioProyecto::new);
        } else {
            assignment = usuarioProyectoRepository
                    .findByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(
                            username,
                            proyectoId,
                            cargoNormalizado)
                    .orElseGet(SeguridadUsuarioProyecto::new);
        }

        assignment.setUsuario(usuario);
        assignment.setProyectoId(proyectoId);
        assignment.setCargo(cargoNormalizado);
        assignment.setActivo(true);
        assignment.setFechaAsignacion(LocalDateTime.now());
        SeguridadUsuarioProyecto saved = usuarioProyectoRepository.save(assignment);

        if (isDirectorCargo(cargoNormalizado)) {
            proyectoRepository.findById(proyectoId).ifPresent(proyecto -> {
                proyecto.setDirector(usuario.getNombre());
                proyecto.setCorreoDirector(usuario.getCorreo());
                proyectoRepository.save(proyecto);
            });
        }

        catalogCacheService.evictAll();
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_ASSIGNMENT_CREATED,
                proyectoId,
                username,
                java.util.Map.of(
                        "assignedUsername", username,
                        "assignmentRole", cargoNormalizado,
                        "projectName", proyectoRepository.findById(proyectoId).map(Proyecto::getNombre).orElse(""),
                        "recipients", List.of(usuario.getCorreo(), proyectoRepository.findById(proyectoId).map(Proyecto::getCorreoDirector).orElse(null))
                )));
        return toUsuarioProyectoDTO(saved);
    }

    public List<SeguridadUsuarioProyectoDTO> listarAsignaciones(String username) {
        return usuarioProyectoRepository.findByUsername(username).stream()
                .map(this::toUsuarioProyectoDTO)
                .toList();
    }

    public List<String> listarCargosAsignacion() {
        return listaParametricaRepository.findByListaClaveAndActivoTrueOrderByOrdenAsc("CARGO_ASIGNACION")
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
            usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                    .orElseThrow(() -> new ForbiddenException("Usuario no encontrado tras aprovisionamiento: " + username));
        }

        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        if (isAdminFromJwt) {
            roleCodes.add("admin");
            permissions.addAll(catalogCacheService.getPermissionsForAllRoles());
        } else {
            String dbRoleCode = usuario.getRolCodigo();
            if (dbRoleCode != null && !dbRoleCode.isBlank()) {
                roleCodes.add(dbRoleCode.trim().toLowerCase(Locale.ROOT));
                permissions.addAll(catalogCacheService.getPermissionsForRoles(roleCodes));
            }

            // Apply user-level overrides (granted add, denied remove)
            Set<String> grantedOverrides = usuarioPermisoRepository.findGrantedPermissionCodesByUsuarioId(usuario.getId());
            Set<String> deniedOverrides = usuarioPermisoRepository.findDeniedPermissionCodesByUsuarioId(usuario.getId());
            permissions.addAll(grantedOverrides);
            permissions.removeAll(deniedOverrides);
            log.info("[AuthzDebug] user={}, userId={}, dbRoleCode={}, finalCount={}, grantedOverrides={}, deniedOverrides={}",
                    username, usuario.getId(), dbRoleCode, permissions.size(), grantedOverrides.size(), deniedOverrides.size());
        }

        boolean transversal = roleCodes.stream().anyMatch(SecurityRoleCatalog::isTransversal);
        boolean administradorLocal = isAdminFromJwt;

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        List<String> projects = catalogCacheService.getProjectsForUser(username);
        try {
            Usuario localUser = localUserAuthorizationService.requireLocalUser(authentication);
            administradorLocal = administradorLocal || localUser.esAdministrador();
        } catch (RuntimeException ignored) {
        }

        log.info("[AuthzDebug] user={}, roles={}, permissionsCount={}, projectsCount={}",
                username, roleCodes, permissions.size(), projects.size());
        log.info("[AuthzDebug] user={}, allPermissions={}", username, permissions);

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
        Proyecto proyecto = item.getProyectoId() != null ? proyectoRepository.findById(item.getProyectoId()).orElse(null) : null;
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
        return normalizeText(value) != null ? normalizeText(value).toLowerCase(Locale.ROOT) : null;
    }

    private String normalizePermission(String value) {
        return normalizeText(value) != null ? normalizeText(value).toUpperCase(Locale.ROOT) : null;
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

        return "director_proyecto".equals(normalized)
                || "lider_tecnico".equals(normalized)
                || "director_tecnico".equals(normalized);
    }

    @Transactional
    protected void backfillUsuariosSinRol() {
        List<SeguridadUsuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (SeguridadUsuario usuario : usuarios) {
            Optional<SeguridadRol> resolvedRole = resolveRoleForSecurityUser(usuario, null, false);
            if (resolvedRole.isEmpty()) {
                continue;
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

            if (localChange) {
                changed = true;
            }
        }

        if (changed) {
            usuarioRepository.saveAll(usuarios);
        }
    }

    private Optional<SeguridadRol> resolveRoleForSecurityUser(
            SeguridadUsuario usuario,
            Authentication authentication,
            boolean preferTokenRoles) {
        List<String> candidateRoles = new ArrayList<>();

        if (preferTokenRoles && authentication != null) {
            candidateRoles.addAll(authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(SecurityRoleCatalog::normalize)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList());
        }

        if (usuario != null) {
            candidateRoles.addAll(resolveRoleCodesFromText(
                    usuario.getRolCodigo(),
                    usuario.getRolNombre(),
                    usuario.getUsername(),
                    usuario.getCorreo(),
                    usuario.getNombre(),
                    usuario.getDependencia()));
        }

        if (authentication != null) {
            candidateRoles.addAll(authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(SecurityRoleCatalog::normalize)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList());
        }

        String preferredCode = pickPreferredSecurityRoleCode(candidateRoles);
        if (preferredCode == null) {
            return Optional.empty();
        }

        return rolRepository.findByCodigoIgnoreCase(preferredCode);
    }

    private List<String> resolveRoleCodesFromText(String... values) {
        List<String> codes = new ArrayList<>();
        if (values == null) {
            return codes;
        }

        for (String value : values) {
            String resolved = resolveRoleCodeFromText(value);
            if (resolved != null && !resolved.isBlank()) {
                codes.add(resolved);
            }
        }

        return codes;
    }

    private String resolveRoleCodeFromText(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("admin") || lower.contains("administrador")) {
            return "admin";
        }
        if (lower.contains("gestor_tic") || lower.contains("gestor tic") || lower.contains("gestor_proyectos_ti")) {
            return "gestor_tic";
        }
        if (lower.contains("director_pro")
                || lower.contains("director_proyecto")
                || lower.contains("director proyecto")
                || lower.contains("proyecta.director_proyecto")) {
            return "director_proyecto";
        }
        if (lower.contains("auditor")) {
            return "auditor";
        }
        if (lower.contains("consulta") || lower.contains("analista")) {
            return "consulta";
        }

        return null;
    }

    private String pickPreferredSecurityRoleCode(List<String> candidateRoles) {
        if (candidateRoles == null || candidateRoles.isEmpty()) {
            return null;
        }

        List<String> priority = List.of("admin", "gestor_tic", "director_proyecto", "auditor", "consulta");
        for (String preferred : priority) {
            if (candidateRoles.stream().anyMatch(preferred::equalsIgnoreCase)) {
                return preferred;
            }
        }

        return candidateRoles.stream()
                .map(SecurityRoleCatalog::normalize)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
