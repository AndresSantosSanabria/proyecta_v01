package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@Component("proyectoSecurity")
public class ProyectoSecurity {

    private final KeycloakIdentityExtractor identityExtractor;
    private final SecurityCatalogCacheService catalogCacheService;
    private final LocalUserAuthorizationService localUserAuthorizationService;

    public ProyectoSecurity(
            KeycloakIdentityExtractor identityExtractor,
            SecurityCatalogCacheService catalogCacheService,
            LocalUserAuthorizationService localUserAuthorizationService) {
        this.identityExtractor = identityExtractor;
        this.catalogCacheService = catalogCacheService;
        this.localUserAuthorizationService = localUserAuthorizationService;
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

        Set<String> roleCodes = resolveRoleCodes(authentication);
        boolean hasPermission = catalogCacheService.getPermissionsForRoles(roleCodes).stream()
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
            throw new ForbiddenException("El usuario no está asignado al proyecto solicitado.");
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

        Set<String> roleCodes = resolveRoleCodes(authentication);
        if (!isTransversal(roleCodes)) {
            throw new ForbiddenException("El acceso global solo esta permitido para roles transversales.");
        }

        boolean hasPermission = catalogCacheService.getPermissionsForRoles(roleCodes).stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        return true;
    }

    public boolean canAccessOwnProjects(Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        Set<String> roleCodes = resolveRoleCodes(authentication);
        if (isTransversal(roleCodes)) {
            return true;
        }

        if (roleCodes.contains("director_proyecto")) {
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

    private boolean isTransversal(Collection<String> roleCodes) {
        return roleCodes.stream()
                .anyMatch(SecurityRoleCatalog::isTransversal);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
