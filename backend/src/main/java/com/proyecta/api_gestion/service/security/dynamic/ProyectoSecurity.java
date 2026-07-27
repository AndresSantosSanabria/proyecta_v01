package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoBeneficioImpacto;
import com.proyecta.api_gestion.repository.ProyectoBeneficioImpactoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component("proyectoSecurity")
public class ProyectoSecurity {
    private static final Set<String> EVIDENCE_REVIEW_ROLE_CODES = Set.of("gestor_tic", "gestor_proyectos");
    private static final Set<String> DOCUMENT_HISTORY_ROLE_CODES = Set.of("gestor_proyectos");
    private static final Set<String> PROJECT_STRUCTURE_MANAGER_ROLE_CODES = Set.of("gestor_tic", "gestor_proyectos");
    private static final Set<String> PROJECT_STRUCTURE_EDITOR_ROLE_CODES = Set.of("gestor_tic", "gestor_proyectos", "director_proyecto");
    private static final Set<String> DIRECTOR_BLOCKED_PERMISSIONS = Set.of(
            "PROYECTO:CREAR",
            "PROYECTO:CERRAR",
            "ENTREGABLE:CREAR",
            "ENTREGABLE:EDITAR",
            "ENTREGABLE:APROBAR",
            "EVIDENCIA:EDITAR",
            "EVIDENCIA:ELIMINAR",
            "DOCUMENTO:HISTORIAL",
            "DOCUMENTO:REVERTIR",
            "CRONOGRAMA:CARGAR",
            "CRONOGRAMA:EDITAR",
            "CRONOGRAMA:ELIMINAR",
            "REPORTE:VER",
            "ANALITICA:VER",
            "CONFIGURACION:VER",
            "SISTEMA:VER",
            "SISTEMA:CREAR",
            "SISTEMA:EDITAR",
            "SISTEMA:CONFIGURAR"
    );

    private final KeycloakIdentityExtractor identityExtractor;
    private final SecurityCatalogCacheService catalogCacheService;
    private final LocalUserAuthorizationService localUserAuthorizationService;
    private final PermisoUsuarioService permisoUsuarioService;
    private final ProyectoRepository proyectoRepository;
    private final ProyectoBeneficioImpactoRepository beneficioImpactoRepository;

    public ProyectoSecurity(
            KeycloakIdentityExtractor identityExtractor,
            SecurityCatalogCacheService catalogCacheService,
            LocalUserAuthorizationService localUserAuthorizationService,
            PermisoUsuarioService permisoUsuarioService,
            ProyectoRepository proyectoRepository,
            ProyectoBeneficioImpactoRepository beneficioImpactoRepository) {
        this.identityExtractor = identityExtractor;
        this.catalogCacheService = catalogCacheService;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.permisoUsuarioService = permisoUsuarioService;
        this.proyectoRepository = proyectoRepository;
        this.beneficioImpactoRepository = beneficioImpactoRepository;
    }

    public boolean canAccess(String permissionCode, String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String normalizedPermission = normalize(permissionCode);
        if (normalizedPermission == null) {
            throw new ForbiddenException("No se pudo evaluar el permiso solicitado.");
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (isDirectorOnly(roleCodes) && DIRECTOR_BLOCKED_PERMISSIONS.contains(normalizedPermission)) {
            throw new ForbiddenException("El Director de Proyecto solo puede cargar, reemplazar y subsanar evidencias de sus proyectos asignados.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        if (isTransversal(roleCodes)) {
            return true;
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no est\u00e1 asignado al proyecto solicitado.");
        }

        return true;
    }

    public boolean canAccessGlobal(String permissionCode, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String normalizedPermission = normalize(permissionCode);
        if (normalizedPermission == null) {
            throw new ForbiddenException("No se pudo evaluar el permiso solicitado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (!isTransversal(roleCodes)) {
            throw new ForbiddenException("El acceso global solo esta permitido para roles transversales.");
        }

        String username = identityExtractor.resolveUsername(authentication);
        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        return true;
    }

    public boolean canAccessOperational(String permissionCode, String proyectoId, Authentication authentication) {
        canAccess(permissionCode, proyectoId, authentication);
        assertOperationalProjectReady(proyectoId, authentication);
        return true;
    }

    public boolean canViewBenefitImpact(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch("BENEFICIO_IMPACTO:VER"::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: BENEFICIO_IMPACTO:VER");
        }

        if (roleCodes.contains("director_proyecto") && !isTransversal(roleCodes)) {
            if (proyectoId == null || proyectoId.isBlank()) {
                return true;
            }

            if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
                throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
            }
            return true;
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        Proyecto proyecto = proyectoRepository.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ForbiddenException("El proyecto solicitado no existe."));
        var record = beneficioImpactoRepository.findByProyecto_Id(proyecto.getId()).orElse(null);
        if (record == null || record.getEstado() != EstadoBeneficioImpacto.DILIGENCIADO) {
            throw new ForbiddenException("La informacion de beneficio e impacto aun no esta disponible para consulta.");
        }
        return true;
    }

    public boolean canEditBenefitImpact(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch("BENEFICIO_IMPACTO:EDITAR"::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: BENEFICIO_IMPACTO:EDITAR");
        }

        if (!roleCodes.contains("director_proyecto") || isTransversal(roleCodes)) {
            throw new ForbiddenException("Solo el Director de Proyecto asignado puede diligenciar esta informacion.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
        }

        return true;
    }

    public boolean canCompleteInitialRegistration(String proyectoId, Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (!roleCodes.contains("director_proyecto") || isTransversal(roleCodes)) {
            throw new ForbiddenException("Solo el Director de Proyecto asignado puede completar la informacion inicial.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            throw new ForbiddenException("No se pudo evaluar el proyecto solicitado.");
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
        }

        Proyecto proyecto = proyectoRepository.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ForbiddenException("El proyecto solicitado no existe."));
        if (!proyecto.requiereCompletitudDirector()) {
            throw new ForbiddenException("El proyecto no esta pendiente de completar.");
        }

        return true;
    }

    public boolean canReviewEvidence(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean hasReviewerRole = roleCodes.stream().anyMatch(EVIDENCE_REVIEW_ROLE_CODES::contains);
        if (!hasReviewerRole) {
            throw new ForbiddenException("Solo el Gestor de Proyectos puede aprobar u observar evidencias.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        String normalizedPermission = normalize("ENTREGABLE:APROBAR");
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        if (isTransversal(roleCodes)) {
            return true;
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no estÃ¡ asignado al proyecto solicitado.");
        }

        return true;
    }

    public boolean canViewDocumentHistory(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean allowedRole = roleCodes.stream().anyMatch(DOCUMENT_HISTORY_ROLE_CODES::contains);
        if (!allowedRole) {
            throw new ForbiddenException("Solo el Administrador o el Gestor de Proyectos pueden consultar el historico documental.");
        }

        if (isTransversal(roleCodes) || proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
        }

        assertOperationalProjectReady(proyectoId, authentication);
        return true;
    }

    public boolean canRevertDocumentVersion(String proyectoId, Authentication authentication) {
        return canViewDocumentHistory(proyectoId, authentication);
    }

    public boolean canManageProjectStructure(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean allowedRole = roleCodes.stream().anyMatch(PROJECT_STRUCTURE_EDITOR_ROLE_CODES::contains);
        if (!allowedRole) {
            throw new ForbiddenException("Solo el Gestor TIC, el Gestor de Proyectos o el Director asignado pueden modificar la estructura del proyecto.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        String normalizedPermission = normalize("PROYECTO:EDITAR");
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);
        boolean isAssignedDirector = roleCodes.contains("director_proyecto") && !isTransversal(roleCodes);

        if (!hasPermission && !isAssignedDirector) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        if (isTransversal(roleCodes) || proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
        }

        assertOperationalProjectReady(proyectoId, authentication);
        return true;
    }

    public boolean canMarkEvidenceCorrected(String proyectoId, Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (!roleCodes.contains("director_proyecto")) {
            throw new ForbiddenException("Solo el Director de Proyecto puede marcar observaciones como subsanadas.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no esta asignado al proyecto solicitado.");
        }

        assertOperationalProjectReady(proyectoId, authentication);
        return true;
    }

    public boolean canAccessOwnProjects(Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (isTransversal(roleCodes)) {
            return true;
        }

        if (roleCodes.contains("director_proyecto")) {
            return true;
        }

        if (roleCodes.contains("consulta")) {
            return true;
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean canViewProjects = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch("PROYECTO:VER"::equals);
        if (canViewProjects) {
            return true;
        }

        throw new ForbiddenException("El usuario no tiene permisos para ver sus proyectos asignados.");
    }

    private boolean isAdmin(Authentication authentication) {
        if (localUserAuthorizationService.hasAdminAuthority(authentication)) {
            return true;
        }

        try {
            Usuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
            return usuario != null && usuario.esAdministrador();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Set<String> resolveRoleCodes(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(SecurityRoleCatalog::normalize)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<String> resolveEffectiveRoleCodes(Authentication authentication) {
        Set<String> roleCodes = new LinkedHashSet<>(resolveRoleCodes(authentication));
        try {
            Usuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
            String localRole = SecurityRoleCatalog.normalize(usuario.getRolCodigo());
            if (localRole != null && !localRole.isBlank()) {
                roleCodes.add(localRole);
            }
        } catch (RuntimeException ignored) {
            // hasBaseAccess already validates local users; keep JWT roles as fallback here.
        }
        return roleCodes;
    }

    private boolean isTransversal(Collection<String> roleCodes) {
        return roleCodes.stream()
                .anyMatch(SecurityRoleCatalog::isTransversal);
    }

    private boolean isDirectorOnly(Collection<String> roleCodes) {
        return roleCodes.contains("director_proyecto")
                && roleCodes.stream().noneMatch(PROJECT_STRUCTURE_MANAGER_ROLE_CODES::contains)
                && !roleCodes.contains("admin");
    }

    private void assertOperationalProjectReady(String proyectoId, Authentication authentication) {
        if (proyectoId == null || proyectoId.isBlank() || isAdmin(authentication)) {
            return;
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (!roleCodes.contains("director_proyecto") || isTransversal(roleCodes)) {
            return;
        }

        Proyecto proyecto = proyectoRepository.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ForbiddenException("El proyecto solicitado no existe."));
        if (proyecto.requiereCompletitudDirector()) {
            throw new ForbiddenException("Debe completar la informacion inicial del proyecto antes de acceder a este modulo.");
        }
    }

    private String normalizeProjectId(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
