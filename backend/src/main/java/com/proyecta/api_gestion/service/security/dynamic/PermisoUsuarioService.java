package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixDTO;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixUpdateRequest;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioPermiso;
import com.proyecta.api_gestion.repository.security.SeguridadPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import jakarta.persistence.EntityManager;
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

    private final SeguridadUsuarioRepository usuarioRepository;
    private final SeguridadPermisoRepository permisoRepository;
    private final SeguridadRolPermisoRepository rolPermisoRepository;
    private final SeguridadUsuarioPermisoRepository usuarioPermisoRepository;
    private final SecurityCatalogCacheService catalogCacheService;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PermisoUsuarioService(
            SeguridadUsuarioRepository usuarioRepository,
            SeguridadPermisoRepository permisoRepository,
            SeguridadRolPermisoRepository rolPermisoRepository,
            SeguridadUsuarioPermisoRepository usuarioPermisoRepository,
            SecurityCatalogCacheService catalogCacheService) {
        this.usuarioRepository = usuarioRepository;
        this.permisoRepository = permisoRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.catalogCacheService = catalogCacheService;
    }

    public PermisoUsuarioMatrixDTO getMatrix(Long usuarioId) {
        SeguridadUsuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        Set<Long> rolePermisoIds = getRolePermissionIds(usuario.getRolCodigo());
        Map<Long, SeguridadUsuarioPermiso> userOverrides = getUserOverridesMap(usuarioId);

        List<SeguridadPermiso> allPermissions = permisoRepository.findAllByActivoTrueOrderByCodigoAsc();

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

        SeguridadUsuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.usuarioId()));

        Set<Long> rolePermisoIds = getRolePermissionIds(usuario.getRolCodigo());

        entityManager.createNativeQuery("DELETE FROM proyecta_db.usuario_permiso WHERE usuario_id = ?1")
                .setParameter(1, usuario.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.usuarioId()));

        if (request.permisos() == null || request.permisos().isEmpty()) {
            catalogCacheService.evictAll();
            return;
        }

        List<SeguridadUsuarioPermiso> overrides = new ArrayList<>();
        for (PermisoUsuarioMatrixUpdateRequest.PermisoUpdateItem item : request.permisos()) {
            SeguridadPermiso permiso = permisoRepository.findById(item.permisoId())
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
            usuarioPermisoRepository.saveAll(overrides);
        }

        catalogCacheService.evictAll();
        logger.info("Matriz de permisos actualizada para usuario {} ({} overrides)",
                usuario.getUsername(), overrides.size());
    }

    public Set<String> getEffectivePermissions(String username) {
        SeguridadUsuario usuario = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null) {
            return Set.of();
        }

        Set<String> rolePermissions = usuario.getRolCodigo() == null
                ? Set.of()
                : catalogCacheService.getPermissionsForRoles(Set.of(usuario.getRolCodigo().toLowerCase()));
        Set<String> grantedOverrides = usuarioPermisoRepository.findGrantedPermissionCodesByUsuarioId(usuario.getId());
        Set<String> deniedOverrides = usuarioPermisoRepository.findDeniedPermissionCodesByUsuarioId(usuario.getId());

        Set<String> effective = new LinkedHashSet<>(rolePermissions);
        effective.addAll(grantedOverrides);
        effective.removeAll(deniedOverrides);

        return effective;
    }

    private Set<Long> getRolePermissionIds(String rolCodigo) {
        if (rolCodigo == null || rolCodigo.isBlank()) {
            return Set.of();
        }
        return rolPermisoRepository.findActiveByRoleCodes(Set.of(rolCodigo.trim().toLowerCase())).stream()
                .map(SeguridadRolPermiso::getPermiso)
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()))
                .map(SeguridadPermiso::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, SeguridadUsuarioPermiso> getUserOverridesMap(Long usuarioId) {
        return usuarioPermisoRepository.findByUsuario_Id(usuarioId).stream()
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
                default -> prefix;
            };
        }
        return "Otros";
    }
}
