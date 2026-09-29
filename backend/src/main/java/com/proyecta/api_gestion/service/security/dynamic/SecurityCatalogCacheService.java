package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.security.SeguridadPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class SecurityCatalogCacheService {

    private static final Logger log = LoggerFactory.getLogger(SecurityCatalogCacheService.class);

    /**
     * TTL del cache de permisos/asignaciones. Incluso si una escritura no pasa por
     * {@link #evictAll()} (carga directa en BD, etc.), la entrada caduca como maximo
     * en un minuto: limita el crecimiento del mapa y acota datos de autorizacion
     * obsoletos (CWE-404 / datos desactualizados de seguridad).
     */
    private static final long CACHE_TTL_MS = 60_000L;
    /** Barrido de entradas expiradas como maximo cada 10 segundos. */
    private static final long SWEEP_INTERVAL_MS = 10_000L;

    private record CachedValue<T>(T value, long expiresAt) {}

    private final SeguridadRolPermisoRepository rolPermisoRepository;
    private final SeguridadPermisoRepository permisoRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;

    private final Map<String, CachedValue<Set<String>>> permissionsByRoleCache = new ConcurrentHashMap<>();
    private final Map<String, CachedValue<Boolean>> projectsByUserCache = new ConcurrentHashMap<>();
    private final AtomicLong lastSweepMs = new AtomicLong(System.currentTimeMillis());

    public SecurityCatalogCacheService(
            SeguridadRolPermisoRepository rolPermisoRepository,
            SeguridadPermisoRepository permisoRepository,
            SeguridadUsuarioProyectoRepository usuarioProyectoRepository) {
        this.rolPermisoRepository = rolPermisoRepository;
        this.permisoRepository = permisoRepository;
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
        long now = System.currentTimeMillis();
        CachedValue<Set<String>> cached = permissionsByRoleCache.get(cacheKey);
        if (cached != null && now < cached.expiresAt()) {
            return cached.value();
        }
        sweepExpired(now);
        Set<String> result = rolPermisoRepository.findActiveByRoleCodes(Set.of(cacheKey)).stream()
                .map(SeguridadRolPermiso::getPermiso)
                .filter(permiso -> permiso != null && Boolean.TRUE.equals(permiso.getActivo()))
                .map(SeguridadPermiso::getCodigo)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(codigo -> codigo.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.info("[AuthzDebug] getPermissionsForRole roleCode={}, foundPermissions={}", roleCode, result.size());
        log.info("[AuthzDebug] getPermissionsForRole roleCode={}, allPermissions={}", roleCode, result);
        Set<String> settled = result.isEmpty() ? Set.of() : Set.copyOf(result);
        permissionsByRoleCache.put(cacheKey, new CachedValue<>(settled, now + CACHE_TTL_MS));
        return settled;
    }

    public boolean isAssignedToProject(String username, String proyectoId) {
        if (username == null || username.isBlank() || proyectoId == null || proyectoId.isBlank()) {
            return false;
        }

        String cacheKey = (username.trim().toLowerCase(Locale.ROOT) + "::" + proyectoId.trim().toUpperCase(Locale.ROOT));
        long now = System.currentTimeMillis();
        CachedValue<Boolean> cached = projectsByUserCache.get(cacheKey);
        if (cached != null && now < cached.expiresAt()) {
            return cached.value();
        }
        sweepExpired(now);
        boolean assigned = usuarioProyectoRepository.existsByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndActivoTrue(
                username.trim(), proyectoId.trim());
        projectsByUserCache.put(cacheKey, new CachedValue<>(assigned, now + CACHE_TTL_MS));
        return assigned;
    }

    /** Elimina entradas vencidas; se ejecuta como maximo una vez por SWEEP_INTERVAL_MS. */
    private void sweepExpired(long now) {
        long last = lastSweepMs.get();
        if (now - last < SWEEP_INTERVAL_MS || !lastSweepMs.compareAndSet(last, now)) {
            return;
        }
        permissionsByRoleCache.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
        projectsByUserCache.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
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

    public Set<String> getPermissionsForAllRoles() {
        return permisoRepository.findAllByActivoTrueOrderByCodigoAsc().stream()
                .map(SeguridadPermiso::getCodigo)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(codigo -> codigo.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void evictAll() {
        permissionsByRoleCache.clear();
        projectsByUserCache.clear();
    }
}
