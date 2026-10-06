package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixDTO;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixUpdateRequest;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.domain.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioPermiso;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadRolPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioPermisoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermisoUsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(PermisoUsuarioService.class);
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado: ";

    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    private final SeguridadPermisoRepositoryPort permisoRepositoryPort;
    private final SeguridadRolPermisoRepositoryPort rolPermisoRepositoryPort;
    private final SeguridadUsuarioPermisoRepositoryPort usuarioPermisoRepositoryPort;
    private final SecurityCatalogCacheService catalogCacheService;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PermisoUsuarioService(
            SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
            SeguridadPermisoRepositoryPort permisoRepositoryPort,
            SeguridadRolPermisoRepositoryPort rolPermisoRepositoryPort,
            SeguridadUsuarioPermisoRepositoryPort usuarioPermisoRepositoryPort,
            SecurityCatalogCacheService catalogCacheService) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.permisoRepositoryPort = permisoRepositoryPort;
        this.rolPermisoRepositoryPort = rolPermisoRepositoryPort;
        this.usuarioPermisoRepositoryPort = usuarioPermisoRepositoryPort;
        this.catalogCacheService = catalogCacheService;
    }

    public PermisoUsuarioMatrixDTO getMatrix(Long usuarioId) {
        SeguridadUsuario usuario = usuarioRepositoryPort.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + usuarioId));

        Set<Long> rolePermisoIds = getRolePermissionIds(usuario.getRolCodigo());
        Map<Long, SeguridadUsuarioPermiso> userOverrides = getUserOverridesMap(usuarioId);

        List<SeguridadPermiso> allPermissions = permisoRepositoryPort.findAllByActivoTrueOrderByCodigoAsc();

        List<PermisoUsuarioMatrixDTO.PermisoItemDTO> items = allPermissions.stream()
                .map(permiso -> {
                    boolean fromRole = rolePermisoIds.contains(permiso.getId());
                    SeguridadUsuarioPermiso override = userOverrides.get(permiso.getId());

                    boolean concedido;
                    boolean source;

                    if (override != null) {
                        concedido = override.getConcedido();
                        source = true;
                    } else {
                        concedido = fromRole;
                        source = false;
                    }

                    String categoria = extractCategory(permiso.getCodigo());
                    boolean sidebar = permiso.getCodigo() != null
                            && permiso.getCodigo().toUpperCase(Locale.ROOT).startsWith("SIDEBAR:")
                            && concedido;

                    return new PermisoUsuarioMatrixDTO.PermisoItemDTO(
                            permiso.getId(),
                            permiso.getCodigo(),
                            permiso.getNombre(),
                            categoria,
                            concedido,
                            source,
                            sidebar
                    );
                })
                .toList();

        return new PermisoUsuarioMatrixDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getRolCodigo(),
                items
        );
    }

    @Transactional
    public void saveMatrix(PermisoUsuarioMatrixUpdateRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new BadRequestException("El ID del usuario es obligatorio.");
        }

        SeguridadUsuario usuario = usuarioRepositoryPort.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + request.usuarioId()));

        Set<Long> rolePermisoIds = getRolePermissionIds(usuario.getRolCodigo());

        entityManager.createNativeQuery("DELETE FROM proyecta_db.usuario_permiso WHERE usuario_id = ?1")
                .setParameter(1, usuario.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        usuario = usuarioRepositoryPort.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + request.usuarioId()));

        if (request.permisos() == null || request.permisos().isEmpty()) {
            catalogCacheService.evictAll();
            return;
        }

        List<SeguridadUsuarioPermiso> overrides = new ArrayList<>();
        for (PermisoUsuarioMatrixUpdateRequest.PermisoUpdateItem item : request.permisos()) {
            SeguridadPermiso permiso = permisoRepositoryPort.findById(item.permisoId())
                    .orElseThrow(() -> new BadRequestException("Permiso no encontrado: " + item.permisoId()));

            boolean fromRole = rolePermisoIds.contains(permiso.getId());
            if (item.concedido() == fromRole) {
                continue;
            }

            SeguridadUsuarioPermiso override = new SeguridadUsuarioPermiso();
            override.setUsuario(usuario);
            override.setPermiso(permiso);
            override.setConcedido(item.concedido());
            overrides.add(override);
        }

        if (!overrides.isEmpty()) {
            usuarioPermisoRepositoryPort.saveAll(overrides);
        }

        catalogCacheService.evictAll();
        logger.info("Matriz de permisos actualizada para usuario {} ({} overrides)",
                usuario.getUsername(), overrides.size());
    }

    public Set<String> getEffectivePermissions(String username) {
        SeguridadUsuario usuario = usuarioRepositoryPort.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null) {
            return Set.of();
        }

        Set<String> rolePermissions = usuario.getRolCodigo() == null
                ? Set.of()
                : catalogCacheService.getPermissionsForRoles(Set.of(usuario.getRolCodigo().toLowerCase()));
        Set<String> grantedOverrides = usuarioPermisoRepositoryPort.findGrantedPermissionCodesByUsuarioId(usuario.getId());
        Set<String> deniedOverrides = usuarioPermisoRepositoryPort.findDeniedPermissionCodesByUsuarioId(usuario.getId());

        Set<String> effective = new LinkedHashSet<>(rolePermissions);
        effective.addAll(grantedOverrides);
        effective.removeAll(deniedOverrides);

        return effective;
    }

    private Set<Long> getRolePermissionIds(String rolCodigo) {
        if (rolCodigo == null || rolCodigo.isBlank()) {
            return Set.of();
        }
        return rolPermisoRepositoryPort.findActiveByRoleCodes(Set.of(rolCodigo.trim().toLowerCase())).stream()
                .map(SeguridadRolPermiso::getPermiso)
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()))
                .map(SeguridadPermiso::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, SeguridadUsuarioPermiso> getUserOverridesMap(Long usuarioId) {
        return usuarioPermisoRepositoryPort.findByUsuarioId(usuarioId).stream()
                .collect(Collectors.toMap(
                        up -> up.getPermiso().getId(),
                        up -> up,
                        (a, b) -> b
                ));
    }

    private String extractCategory(String codigo) {
        if (codigo == null) return "Otros";
        int idx = codigo.indexOf(':');
        if (idx > 0) {
            String prefix = codigo.substring(0, idx);
            return switch (prefix) {
                case "DASHBOARD" -> "Dashboard";
                case "PROYECTO" -> "Proyectos";
                case "REPORTE" -> "Reportes";
                case "ANALITICA" -> "Analiticas";
                case "ENTREGABLE" -> "Entregables";
                case "AVANCE" -> "Avances";
                case "EVIDENCIA" -> "Evidencias";
                case "DOCUMENTO" -> "Documentos";
                case "CRONOGRAMA" -> "Cronograma";
                case "BENEFICIO_IMPACTO" -> "Beneficio e Impacto";
                case "CIERRE" -> "Cierre";
                case "SISTEMA" -> "Sistema";
                case "CONFIGURACION" -> "Configuracion";
                case "SIDEBAR" -> "Modulos Sidebar";
                case "AUDITORIA" -> "Auditoria";
                default -> prefix;
            };
        }
        return "Otros";
    }
}
