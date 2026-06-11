package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import com.proyecta.api_gestion.model.security.SeguridadRol;
import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import com.proyecta.api_gestion.repository.security.SeguridadPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolRepository;
import com.proyecta.api_gestion.service.security.dynamic.SecurityCatalogCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SecurityCatalogSeeder {

    private static final Logger logger = LoggerFactory.getLogger(SecurityCatalogSeeder.class);

    private static final List<RoleSeed> ROLES = List.of(
            new RoleSeed("admin", "Administrador", "Control total de la plataforma", true),
            new RoleSeed("gestor_tic", "Gestor TIC", "Administracion tecnica y transversal", true),
            new RoleSeed("gestor_proyectos", "Gestor de Proyectos", "Gestion funcional y revision de evidencias", true),
            new RoleSeed("director_proyecto", "Director de Proyecto", "Operacion sobre sus proyectos asignados", false),
            new RoleSeed("auditor", "Auditor", "Consulta y revision sin edicion", false),
            new RoleSeed("consulta", "Consulta", "Solo lectura", false)
    );

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("DASHBOARD:VER", "Ver dashboard", "Permite consultar la portada y resumen principal del sistema"),
            new PermissionSeed("PROYECTO:VER", "Ver proyecto", "Permite consultar el detalle de proyectos"),
            new PermissionSeed("PROYECTO:CREAR", "Crear proyecto", "Permite crear proyectos nuevos"),
            new PermissionSeed("PROYECTO:EDITAR", "Editar proyecto", "Permite editar proyectos existentes"),
            new PermissionSeed("PROYECTO:CERRAR", "Cerrar proyecto", "Permite cerrar proyectos"),
            new PermissionSeed("REPORTE:VER", "Ver reportes", "Permite acceder al modulo de reportes"),
            new PermissionSeed("ANALITICA:VER", "Ver analiticas", "Permite acceder al modulo de analiticas"),
            new PermissionSeed("CONFIGURACION:VER", "Ver configuracion", "Permite mostrar la pantalla de administracion y seguridad"),
            new PermissionSeed("ENTREGABLE:VER", "Ver entregable", "Permite consultar el detalle de entregables"),
            new PermissionSeed("ENTREGABLE:CREAR", "Crear entregable", "Permite registrar entregables nuevos"),
            new PermissionSeed("ENTREGABLE:EDITAR", "Editar entregable", "Permite modificar entregables existentes"),
            new PermissionSeed("ENTREGABLE:APROBAR", "Aprobar entregable", "Permite marcar entregables como conformes"),
            new PermissionSeed("EVIDENCIA:VER", "Ver evidencia", "Permite consultar evidencias registradas"),
            new PermissionSeed("EVIDENCIA:CARGAR", "Cargar evidencia", "Permite subir evidencias PDF"),
            new PermissionSeed("EVIDENCIA:EDITAR", "Editar evidencia", "Permite actualizar evidencias existentes"),
            new PermissionSeed("EVIDENCIA:ELIMINAR", "Eliminar evidencia", "Permite eliminar evidencias registradas"),
            new PermissionSeed("DOCUMENTO:VER", "Ver documento", "Permite consultar documentos registrados"),
            new PermissionSeed("DOCUMENTO:CARGAR", "Cargar documento", "Permite subir documentos de soporte"),
            new PermissionSeed("DOCUMENTO:EDITAR", "Editar documento", "Permite actualizar documentos existentes"),
            new PermissionSeed("DOCUMENTO:ELIMINAR", "Eliminar documento", "Permite eliminar documentos registrados"),
            new PermissionSeed("DOCUMENTO:HISTORIAL", "Ver historico documental", "Permite consultar versiones anteriores de evidencias"),
            new PermissionSeed("DOCUMENTO:REVERTIR", "Revertir documento", "Permite restaurar una version anterior de una evidencia"),
            new PermissionSeed("CRONOGRAMA:VER", "Ver cronograma", "Permite consultar cronogramas registrados"),
            new PermissionSeed("CRONOGRAMA:CARGAR", "Cargar cronograma", "Permite subir el PDF del cronograma"),
            new PermissionSeed("CRONOGRAMA:EDITAR", "Editar cronograma", "Permite actualizar cronogramas existentes"),
            new PermissionSeed("CRONOGRAMA:ELIMINAR", "Eliminar cronograma", "Permite eliminar cronogramas registrados"),
            new PermissionSeed("SISTEMA:VER", "Ver sistema", "Permite consultar la configuracion general del sistema"),
            new PermissionSeed("SISTEMA:CREAR", "Crear configuracion del sistema", "Permite registrar configuraciones de sistema"),
            new PermissionSeed("SISTEMA:EDITAR", "Editar sistema", "Permite actualizar la configuracion general del sistema"),
            new PermissionSeed("SISTEMA:CONFIGURAR", "Configurar sistema", "Permite administrar usuarios, roles y permisos")
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "admin", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER", "PROYECTO:CREAR", "PROYECTO:EDITAR", "PROYECTO:CERRAR",
                    "REPORTE:VER", "ANALITICA:VER", "CONFIGURACION:VER",
                    "ENTREGABLE:VER", "ENTREGABLE:CREAR", "ENTREGABLE:EDITAR", "ENTREGABLE:APROBAR",
                    "EVIDENCIA:VER", "EVIDENCIA:CARGAR", "EVIDENCIA:EDITAR", "EVIDENCIA:ELIMINAR",
                    "DOCUMENTO:VER", "DOCUMENTO:CARGAR", "DOCUMENTO:EDITAR", "DOCUMENTO:ELIMINAR", "DOCUMENTO:HISTORIAL", "DOCUMENTO:REVERTIR",
                    "CRONOGRAMA:VER", "CRONOGRAMA:CARGAR", "CRONOGRAMA:EDITAR", "CRONOGRAMA:ELIMINAR",
                    "SISTEMA:VER", "SISTEMA:CREAR", "SISTEMA:EDITAR", "SISTEMA:CONFIGURAR"),
            "gestor_tic", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER", "PROYECTO:CREAR", "PROYECTO:EDITAR", "PROYECTO:CERRAR",
                    "REPORTE:VER", "ANALITICA:VER", "CONFIGURACION:VER",
                    "ENTREGABLE:VER", "ENTREGABLE:CREAR", "ENTREGABLE:EDITAR", "ENTREGABLE:APROBAR",
                    "EVIDENCIA:VER", "EVIDENCIA:CARGAR", "EVIDENCIA:EDITAR", "EVIDENCIA:ELIMINAR",
                    "DOCUMENTO:VER", "DOCUMENTO:CARGAR", "DOCUMENTO:EDITAR", "DOCUMENTO:ELIMINAR",
                    "CRONOGRAMA:VER", "CRONOGRAMA:CARGAR", "CRONOGRAMA:EDITAR", "CRONOGRAMA:ELIMINAR",
                    "SISTEMA:VER", "SISTEMA:CREAR", "SISTEMA:EDITAR", "SISTEMA:CONFIGURAR"),
            "gestor_proyectos", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER", "PROYECTO:CREAR", "PROYECTO:EDITAR", "PROYECTO:CERRAR",
                    "REPORTE:VER", "ANALITICA:VER",
                    "ENTREGABLE:VER", "ENTREGABLE:CREAR", "ENTREGABLE:EDITAR", "ENTREGABLE:APROBAR",
                    "EVIDENCIA:VER", "EVIDENCIA:CARGAR", "EVIDENCIA:EDITAR", "EVIDENCIA:ELIMINAR",
                    "DOCUMENTO:VER", "DOCUMENTO:CARGAR", "DOCUMENTO:EDITAR", "DOCUMENTO:ELIMINAR", "DOCUMENTO:HISTORIAL", "DOCUMENTO:REVERTIR",
                    "CRONOGRAMA:VER", "CRONOGRAMA:CARGAR", "CRONOGRAMA:EDITAR", "CRONOGRAMA:ELIMINAR"),
            "director_proyecto", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER",
                    "ENTREGABLE:VER", "EVIDENCIA:VER", "EVIDENCIA:CARGAR",
                    "DOCUMENTO:VER"),
            "auditor", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER", "REPORTE:VER", "ANALITICA:VER",
                    "ENTREGABLE:VER", "EVIDENCIA:VER",
                    "DOCUMENTO:VER", "CRONOGRAMA:VER", "SISTEMA:VER"),
            "consulta", List.of(
                    "DASHBOARD:VER", "PROYECTO:VER", "REPORTE:VER", "ANALITICA:VER",
                    "ENTREGABLE:VER", "EVIDENCIA:VER",
                    "DOCUMENTO:VER", "CRONOGRAMA:VER")
    );

    private final SeguridadRolRepository rolRepository;
    private final SeguridadPermisoRepository permisoRepository;
    private final SeguridadRolPermisoRepository rolPermisoRepository;
    private final SecurityCatalogCacheService catalogCacheService;

    public SecurityCatalogSeeder(
            SeguridadRolRepository rolRepository,
            SeguridadPermisoRepository permisoRepository,
            SeguridadRolPermisoRepository rolPermisoRepository,
            SecurityCatalogCacheService catalogCacheService) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.catalogCacheService = catalogCacheService;
    }

    public void seedSecurityCatalog() {
        logger.info("Cargando catálogo de seguridad...");

        for (RoleSeed roleSeed : ROLES) {
            SeguridadRol rol = rolRepository.findByCodigoIgnoreCase(roleSeed.codigo())
                    .orElseGet(SeguridadRol::new);
            rol.setCodigo(roleSeed.codigo());
            rol.setNombre(roleSeed.nombre());
            rol.setDescripcion(roleSeed.descripcion());
            rol.setTransversal(roleSeed.transversal());
            rol.setActivo(true);
            rolRepository.save(rol);
        }

        for (PermissionSeed permissionSeed : PERMISSIONS) {
            SeguridadPermiso permiso = permisoRepository.findByCodigoIgnoreCase(permissionSeed.codigo())
                    .orElseGet(SeguridadPermiso::new);
            permiso.setCodigo(permissionSeed.codigo());
            permiso.setNombre(permissionSeed.nombre());
            permiso.setDescripcion(permissionSeed.descripcion());
            permiso.setActivo(true);
            permisoRepository.save(permiso);
        }

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            SeguridadRol rol = rolRepository.findByCodigoIgnoreCase(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException("Rol de seguridad no encontrado: " + entry.getKey()));

            for (String permissionCode : entry.getValue()) {
                SeguridadPermiso permiso = permisoRepository.findByCodigoIgnoreCase(permissionCode)
                        .orElseThrow(() -> new IllegalStateException("Permiso de seguridad no encontrado: " + permissionCode));

                boolean exists = rolPermisoRepository.findAll().stream().anyMatch(rp ->
                        rp.getRol() != null
                                && rp.getPermiso() != null
                                && rp.getRol().getCodigo() != null
                                && rp.getPermiso().getCodigo() != null
                                && rp.getRol().getCodigo().equalsIgnoreCase(rol.getCodigo())
                                && rp.getPermiso().getCodigo().equalsIgnoreCase(permiso.getCodigo()));

                if (exists) {
                    continue;
                }

                SeguridadRolPermiso relation = new SeguridadRolPermiso();
                relation.setRol(rol);
                relation.setPermiso(permiso);
                relation.setActivo(true);
                rolPermisoRepository.save(relation);
            }
        }

        catalogCacheService.evictAll();
        logger.info("✓ Catálogo de seguridad cargado");
    }

    private record RoleSeed(String codigo, String nombre, String descripcion, boolean transversal) {}

    private record PermissionSeed(String codigo, String nombre, String descripcion) {}
}
