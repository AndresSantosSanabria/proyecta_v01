package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SecurityCatalogCacheService {

    private final SeguridadRolPermisoRepository rolPermisoRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;

    private final Map<String, Set<String>> permissionsByRoleCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> projectsByUserCache = new ConcurrentHashMap<>();

    public SecurityCatalogCacheService(
            SeguridadRolPermisoRepository rolPermisoRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository) {
        this.rolPermisoRepository = rolPermisoRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
    }

    public Set<String> getPermissionsForRoles(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedRoles = roleCodes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> permissions = new LinkedHashSet<>();
        for (String roleCode : normalizedRoles) {
            permissions.addAll(getPermissionsForRole(roleCode));
        }

        return permissions;
    }

    public Set<String> getPermissionsForRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return Set.of();
        }

        String cacheKey = roleCode.trim().toLowerCase(Locale.ROOT);
        return permissionsByRoleCache.computeIfAbsent(cacheKey, key ->
                rolPermisoRepository.findActiveByRoleCodes(Set.of(key)).stream()
                        .map(SeguridadRolPermiso::getPermiso)
                        .filter(permiso -> permiso != null && Boolean.TRUE.equals(permiso.getActivo()))
                        .map(SeguridadPermiso::getCodigo)
                        .filter(codigo -> codigo != null && !codigo.isBlank())
                        .map(codigo -> codigo.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    public boolean isAssignedToProject(String username, String proyectoId) {
        if (username == null || username.isBlank() || proyectoId == null || proyectoId.isBlank()) {
            return false;
        }

        String cacheKey = (username.trim().toLowerCase(Locale.ROOT) + "::" + proyectoId.trim().toUpperCase(Locale.ROOT));
        return projectsByUserCache.computeIfAbsent(cacheKey, key ->
                usuarioProyectoRepository.existsByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndActivoTrue(
                        username.trim(), proyectoId.trim()));
    }

    public List<String> getProjectsForUser(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        return usuarioProyectoRepository.findByUsername(username).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .map(SeguridadUsuarioProyecto::getProyectoId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    public void evictAll() {
        permissionsByRoleCache.clear();
        projectsByUserCache.clear();
    }
}
