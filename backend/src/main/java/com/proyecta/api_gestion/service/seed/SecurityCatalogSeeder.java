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

    private static final String PERM_ANALITICA_VER = "ANALITICA:VER";
    private static final String PERM_AVANCE_APROBAR = "AVANCE:APROBAR";
    private static final String PERM_AVANCE_EDITAR = "AVANCE:EDITAR";
    private static final String PERM_AVANCE_VER = "AVANCE:VER";
    private static final String PERM_BENEFICIO_IMPACTO_APROBAR = "BENEFICIO_IMPACTO:APROBAR";
    private static final String PERM_BENEFICIO_IMPACTO_EDITAR = "BENEFICIO_IMPACTO:EDITAR";
    private static final String PERM_BENEFICIO_IMPACTO_VER = "BENEFICIO_IMPACTO:VER";
    private static final String PERM_CIERRE_APROBAR = "CIERRE:APROBAR";
    private static final String PERM_CIERRE_SOLICITAR = "CIERRE:SOLICITAR";
    private static final String PERM_CONFIGURACION_VER = "CONFIGURACION:VER";
    private static final String PERM_CRONOGRAMA_CARGAR = "CRONOGRAMA:CARGAR";
    private static final String PERM_CRONOGRAMA_EDITAR = "CRONOGRAMA:EDITAR";
    private static final String PERM_CRONOGRAMA_ELIMINAR = "CRONOGRAMA:ELIMINAR";
    private static final String PERM_CRONOGRAMA_VER = "CRONOGRAMA:VER";
    private static final String PERM_DASHBOARD_VER = "DASHBOARD:VER";
    private static final String PERM_DOCUMENTO_CARGAR = "DOCUMENTO:CARGAR";
    private static final String PERM_DOCUMENTO_EDITAR = "DOCUMENTO:EDITAR";
    private static final String PERM_DOCUMENTO_ELIMINAR = "DOCUMENTO:ELIMINAR";
    private static final String PERM_DOCUMENTO_HISTORIAL = "DOCUMENTO:HISTORIAL";
    private static final String PERM_DOCUMENTO_REVERTIR = "DOCUMENTO:REVERTIR";
    private static final String PERM_DOCUMENTO_VER = "DOCUMENTO:VER";
    private static final String PERM_DOCUMENTO_INTERNO_CARGAR = "DOCUMENTO_INTERNO:CARGAR";
    private static final String PERM_DOCUMENTO_INTERNO_VER = "DOCUMENTO_INTERNO:VER";
    private static final String PERM_ENTREGABLE_APROBAR = "ENTREGABLE:APROBAR";
    private static final String PERM_ENTREGABLE_CAMBIAR_FECHA = "ENTREGABLE:CAMBIAR_FECHA";
    private static final String PERM_ENTREGABLE_CREAR = "ENTREGABLE:CREAR";
    private static final String PERM_ENTREGABLE_EDITAR = "ENTREGABLE:EDITAR";
    private static final String PERM_ENTREGABLE_VER = "ENTREGABLE:VER";
    private static final String PERM_EVIDENCIA_CARGAR = "EVIDENCIA:CARGAR";
    private static final String PERM_EVIDENCIA_EDITAR = "EVIDENCIA:EDITAR";
    private static final String PERM_EVIDENCIA_ELIMINAR = "EVIDENCIA:ELIMINAR";
    private static final String PERM_EVIDENCIA_VER = "EVIDENCIA:VER";
    private static final String PERM_PROYECTO_CERRAR = "PROYECTO:CERRAR";
    private static final String PERM_PROYECTO_CREAR = "PROYECTO:CREAR";
    private static final String PERM_PROYECTO_EDITAR = "PROYECTO:EDITAR";
    private static final String PERM_PROYECTO_VER = "PROYECTO:VER";
    private static final String PERM_REPORTE_DESCARGAR_ACTUAL = "REPORTE:DESCARGAR_ACTUAL";
    private static final String PERM_REPORTE_VER = "REPORTE:VER";
    private static final String PERM_SIDEBAR_ANALITICAS = "SIDEBAR:ANALITICAS";
    private static final String PERM_SIDEBAR_DASHBOARD = "SIDEBAR:DASHBOARD";
    private static final String PERM_SIDEBAR_DOCUMENTACION_INTERNA = "SIDEBAR:DOCUMENTACION_INTERNA";
    private static final String PERM_SIDEBAR_PROYECTOS = "SIDEBAR:PROYECTOS";
    private static final String PERM_SIDEBAR_REPORTES = "SIDEBAR:REPORTES";
    private static final String PERM_SIDEBAR_SEGURIDAD = "SIDEBAR:SEGURIDAD";
    private static final String PERM_SISTEMA_CONFIGURAR = "SISTEMA:CONFIGURAR";
    private static final String PERM_SISTEMA_CREAR = "SISTEMA:CREAR";
    private static final String PERM_SISTEMA_EDITAR = "SISTEMA:EDITAR";
    private static final String PERM_SISTEMA_VER = "SISTEMA:VER";

    private static final List<RoleSeed> ROLES = List.of(
            new RoleSeed("admin", "Administrador", "Control total de la plataforma", true),
            new RoleSeed("gestor_tic", "Gestor TIC", "Administracion tecnica y transversal", true),
            new RoleSeed("gestor_proyectos", "Gestor de Proyectos", "Gestion funcional y revision de evidencias", true),
            new RoleSeed("director_proyecto", "Director de Proyecto", "Operacion sobre sus proyectos asignados", false),
            new RoleSeed("auditor", "Auditor", "Consulta y revision sin edicion", false),
            new RoleSeed("consulta", "Consulta", "Solo lectura", false),
            new RoleSeed("visualizador", "Visualizador", "Acceso de solo lectura por defecto", false)
    );

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed(PERM_DASHBOARD_VER, "Ver dashboard", "Permite consultar la portada y resumen principal del sistema"),
            new PermissionSeed(PERM_PROYECTO_VER, "Ver proyecto", "Permite consultar el detalle de proyectos"),
            new PermissionSeed(PERM_PROYECTO_CREAR, "Crear proyecto", "Permite crear proyectos nuevos"),
            new PermissionSeed(PERM_PROYECTO_EDITAR, "Editar proyecto", "Permite editar proyectos existentes"),
            new PermissionSeed(PERM_PROYECTO_CERRAR, "Cerrar proyecto", "Permite cerrar proyectos"),
            new PermissionSeed(PERM_REPORTE_VER, "Ver reportes", "Permite acceder al modulo de reportes"),
            new PermissionSeed(PERM_REPORTE_DESCARGAR_ACTUAL, "Descargar reporte actual del proyecto", "Permite descargar el reporte Excel actualizado de cada proyecto"),
            new PermissionSeed(PERM_ANALITICA_VER, "Ver analiticas", "Permite acceder al modulo de analiticas"),
            new PermissionSeed(PERM_CONFIGURACION_VER, "Ver configuracion", "Permite mostrar la pantalla de administracion y seguridad"),
            new PermissionSeed(PERM_ENTREGABLE_VER, "Ver entregable", "Permite consultar el detalle de entregables"),
            new PermissionSeed(PERM_ENTREGABLE_CREAR, "Crear entregable", "Permite registrar entregables nuevos"),
            new PermissionSeed(PERM_ENTREGABLE_EDITAR, "Editar entregable", "Permite modificar entregables existentes"),
            new PermissionSeed(PERM_ENTREGABLE_APROBAR, "Aprobar entregable", "Permite marcar entregables como conformes"),
            new PermissionSeed(PERM_ENTREGABLE_CAMBIAR_FECHA, "Cambiar fecha limite", "Permite modificar la fecha limite de un entregable existente con justificacion y soporte"),
            new PermissionSeed(PERM_AVANCE_VER, "Ver avance", "Permite consultar el avance consolidado del proyecto"),
            new PermissionSeed(PERM_AVANCE_EDITAR, "Editar avance", "Permite registrar y actualizar avances del proyecto"),
            new PermissionSeed(PERM_AVANCE_APROBAR, "Aprobar avance", "Permite validar avances enviados por el Director de Proyecto"),
            new PermissionSeed(PERM_EVIDENCIA_VER, "Ver evidencia", "Permite consultar evidencias registradas"),
            new PermissionSeed(PERM_EVIDENCIA_CARGAR, "Cargar evidencia", "Permite subir evidencias PDF"),
            new PermissionSeed(PERM_EVIDENCIA_EDITAR, "Editar evidencia", "Permite actualizar evidencias existentes"),
            new PermissionSeed(PERM_EVIDENCIA_ELIMINAR, "Eliminar evidencia", "Permite eliminar evidencias registradas"),
            new PermissionSeed(PERM_DOCUMENTO_VER, "Ver documento", "Permite consultar documentos registrados"),
            new PermissionSeed(PERM_DOCUMENTO_CARGAR, "Cargar documento", "Permite subir documentos de soporte"),
            new PermissionSeed(PERM_DOCUMENTO_EDITAR, "Editar documento", "Permite actualizar documentos existentes"),
            new PermissionSeed(PERM_DOCUMENTO_ELIMINAR, "Eliminar documento", "Permite eliminar documentos registrados"),
            new PermissionSeed(PERM_DOCUMENTO_HISTORIAL, "Ver historico documental", "Permite consultar versiones anteriores de evidencias"),
            new PermissionSeed(PERM_DOCUMENTO_REVERTIR, "Revertir documento", "Permite restaurar una version anterior de una evidencia"),
            new PermissionSeed(PERM_CRONOGRAMA_VER, "Ver cronograma", "Permite consultar cronogramas registrados"),
            new PermissionSeed(PERM_CRONOGRAMA_CARGAR, "Cargar cronograma", "Permite subir el PDF del cronograma"),
            new PermissionSeed(PERM_CRONOGRAMA_EDITAR, "Editar cronograma", "Permite actualizar cronogramas existentes"),
            new PermissionSeed(PERM_CRONOGRAMA_ELIMINAR, "Eliminar cronograma", "Permite eliminar cronogramas registrados"),
            new PermissionSeed(PERM_BENEFICIO_IMPACTO_VER, "Ver beneficio e impacto", "Permite consultar la informacion de beneficio e impacto del proyecto"),
            new PermissionSeed(PERM_BENEFICIO_IMPACTO_EDITAR, "Editar beneficio e impacto", "Permite diligenciar la informacion de beneficio e impacto del proyecto"),
            new PermissionSeed(PERM_BENEFICIO_IMPACTO_APROBAR, "Revisar beneficio e impacto", "Permite aprobar u observar la informacion de beneficio e impacto del proyecto"),
            new PermissionSeed(PERM_CIERRE_SOLICITAR, "Solicitar cierre", "Permite enviar la solicitud de cierre del proyecto"),
            new PermissionSeed(PERM_CIERRE_APROBAR, "Aprobar cierre", "Permite validar y aprobar el cierre del proyecto"),
            new PermissionSeed(PERM_SISTEMA_VER, "Ver sistema", "Permite consultar la configuracion general del sistema"),
            new PermissionSeed(PERM_SISTEMA_CREAR, "Crear configuracion del sistema", "Permite registrar configuraciones de sistema"),
            new PermissionSeed(PERM_SISTEMA_EDITAR, "Editar sistema", "Permite actualizar la configuracion general del sistema"),
            new PermissionSeed(PERM_SISTEMA_CONFIGURAR, "Configurar sistema", "Permite administrar usuarios, roles y permisos"),
            new PermissionSeed(PERM_SIDEBAR_DASHBOARD, "Mostrar Dashboard en menu", "Controla la visibilidad del modulo Dashboard en el sidebar"),
            new PermissionSeed(PERM_SIDEBAR_PROYECTOS, "Mostrar Proyectos en menu", "Controla la visibilidad del modulo Proyectos en el sidebar"),
            new PermissionSeed(PERM_SIDEBAR_REPORTES, "Mostrar Reportes en menu", "Controla la visibilidad del modulo Reportes en el sidebar"),
            new PermissionSeed(PERM_SIDEBAR_ANALITICAS, "Mostrar Analiticas en menu", "Controla la visibilidad del modulo Analiticas en el sidebar"),
            new PermissionSeed(PERM_SIDEBAR_SEGURIDAD, "Mostrar Configuracion Seguridad en menu", "Controla la visibilidad del modulo Configuracion y Seguridad en el sidebar"),
            new PermissionSeed("AUDITORIA:VER", "Ver auditoria", "Permite consultar el registro de auditoria de acciones y logs del sistema"),
            new PermissionSeed(PERM_DOCUMENTO_INTERNO_VER, "Ver documentacion interna", "Permite consultar la documentacion interna del sistema"),
            new PermissionSeed(PERM_DOCUMENTO_INTERNO_CARGAR, "Cargar documentacion interna", "Permite subir archivos de documentacion interna"),
            new PermissionSeed(PERM_SIDEBAR_DOCUMENTACION_INTERNA, "Mostrar Documentacion Interna en menu", "Controla la visibilidad del modulo de documentacion interna en el sidebar")
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "admin", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_PROYECTO_CREAR, PERM_PROYECTO_EDITAR, PERM_PROYECTO_CERRAR,
                    PERM_REPORTE_VER, PERM_REPORTE_DESCARGAR_ACTUAL, PERM_ANALITICA_VER, PERM_CONFIGURACION_VER,
                    PERM_ENTREGABLE_VER, PERM_ENTREGABLE_CREAR, PERM_ENTREGABLE_EDITAR, PERM_ENTREGABLE_APROBAR, PERM_ENTREGABLE_CAMBIAR_FECHA,
                    PERM_AVANCE_VER, PERM_AVANCE_EDITAR, PERM_AVANCE_APROBAR,
                    PERM_EVIDENCIA_VER, PERM_EVIDENCIA_CARGAR, PERM_EVIDENCIA_EDITAR, PERM_EVIDENCIA_ELIMINAR,
                    PERM_DOCUMENTO_VER, PERM_DOCUMENTO_CARGAR, PERM_DOCUMENTO_EDITAR, PERM_DOCUMENTO_ELIMINAR, PERM_DOCUMENTO_HISTORIAL, PERM_DOCUMENTO_REVERTIR,
                    PERM_CRONOGRAMA_VER, PERM_CRONOGRAMA_CARGAR, PERM_CRONOGRAMA_EDITAR, PERM_CRONOGRAMA_ELIMINAR,
                    PERM_BENEFICIO_IMPACTO_VER, PERM_BENEFICIO_IMPACTO_EDITAR, PERM_BENEFICIO_IMPACTO_APROBAR,
                    PERM_CIERRE_SOLICITAR, PERM_CIERRE_APROBAR,
                    PERM_SISTEMA_VER, PERM_SISTEMA_CREAR, PERM_SISTEMA_EDITAR, PERM_SISTEMA_CONFIGURAR,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS, PERM_SIDEBAR_REPORTES, PERM_SIDEBAR_ANALITICAS, PERM_SIDEBAR_SEGURIDAD,
                    "AUDITORIA:VER",
                    PERM_DOCUMENTO_INTERNO_VER, PERM_DOCUMENTO_INTERNO_CARGAR, PERM_SIDEBAR_DOCUMENTACION_INTERNA),
            "gestor_tic", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_PROYECTO_CREAR, PERM_PROYECTO_EDITAR, PERM_PROYECTO_CERRAR,
                    PERM_REPORTE_VER, PERM_REPORTE_DESCARGAR_ACTUAL, PERM_ANALITICA_VER, PERM_CONFIGURACION_VER,
                    PERM_ENTREGABLE_VER, PERM_ENTREGABLE_CREAR, PERM_ENTREGABLE_EDITAR, PERM_ENTREGABLE_APROBAR, PERM_ENTREGABLE_CAMBIAR_FECHA,
                    PERM_AVANCE_VER, PERM_AVANCE_EDITAR, PERM_AVANCE_APROBAR,
                    PERM_EVIDENCIA_VER, PERM_EVIDENCIA_CARGAR, PERM_EVIDENCIA_EDITAR, PERM_EVIDENCIA_ELIMINAR,
                    PERM_DOCUMENTO_VER, PERM_DOCUMENTO_CARGAR, PERM_DOCUMENTO_EDITAR, PERM_DOCUMENTO_ELIMINAR,
                    PERM_CRONOGRAMA_VER, PERM_CRONOGRAMA_CARGAR, PERM_CRONOGRAMA_EDITAR, PERM_CRONOGRAMA_ELIMINAR,
                    PERM_BENEFICIO_IMPACTO_VER, PERM_BENEFICIO_IMPACTO_APROBAR,
                    PERM_CIERRE_APROBAR,
                    PERM_SISTEMA_VER, PERM_SISTEMA_CREAR, PERM_SISTEMA_EDITAR, PERM_SISTEMA_CONFIGURAR,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS, PERM_SIDEBAR_REPORTES, PERM_SIDEBAR_ANALITICAS, PERM_SIDEBAR_SEGURIDAD,
                    PERM_DOCUMENTO_INTERNO_VER, PERM_DOCUMENTO_INTERNO_CARGAR, PERM_SIDEBAR_DOCUMENTACION_INTERNA),
            "gestor_proyectos", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_PROYECTO_CREAR, PERM_PROYECTO_EDITAR, PERM_PROYECTO_CERRAR,
                    PERM_REPORTE_VER, PERM_REPORTE_DESCARGAR_ACTUAL, PERM_ANALITICA_VER, PERM_CONFIGURACION_VER,
                    PERM_ENTREGABLE_VER, PERM_ENTREGABLE_CREAR, PERM_ENTREGABLE_EDITAR, PERM_ENTREGABLE_APROBAR, PERM_ENTREGABLE_CAMBIAR_FECHA,
                    PERM_AVANCE_VER, PERM_AVANCE_EDITAR,
                    PERM_EVIDENCIA_VER, PERM_EVIDENCIA_CARGAR, PERM_EVIDENCIA_EDITAR, PERM_EVIDENCIA_ELIMINAR,
                    PERM_DOCUMENTO_VER, PERM_DOCUMENTO_CARGAR, PERM_DOCUMENTO_EDITAR, PERM_DOCUMENTO_ELIMINAR, PERM_DOCUMENTO_HISTORIAL, PERM_DOCUMENTO_REVERTIR,
                    PERM_CRONOGRAMA_VER, PERM_CRONOGRAMA_CARGAR, PERM_CRONOGRAMA_EDITAR, PERM_CRONOGRAMA_ELIMINAR,
                    PERM_BENEFICIO_IMPACTO_VER, PERM_BENEFICIO_IMPACTO_APROBAR,
                    PERM_CIERRE_SOLICITAR, PERM_CIERRE_APROBAR,
                    PERM_SISTEMA_VER, PERM_SISTEMA_CONFIGURAR,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS, PERM_SIDEBAR_REPORTES, PERM_SIDEBAR_ANALITICAS, PERM_SIDEBAR_SEGURIDAD,
                    PERM_DOCUMENTO_INTERNO_VER, PERM_DOCUMENTO_INTERNO_CARGAR, PERM_SIDEBAR_DOCUMENTACION_INTERNA),
            "director_proyecto", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_PROYECTO_EDITAR,
                    PERM_ENTREGABLE_VER, PERM_EVIDENCIA_VER, PERM_EVIDENCIA_CARGAR,
                    PERM_BENEFICIO_IMPACTO_VER, PERM_BENEFICIO_IMPACTO_EDITAR,
                    PERM_DOCUMENTO_VER, PERM_DOCUMENTO_CARGAR, PERM_DOCUMENTO_EDITAR, PERM_DOCUMENTO_ELIMINAR,
                    PERM_AVANCE_VER, PERM_AVANCE_EDITAR,
                    PERM_CRONOGRAMA_VER, PERM_CRONOGRAMA_CARGAR,
                    PERM_CIERRE_SOLICITAR,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS),
            "auditor", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_REPORTE_VER, PERM_ANALITICA_VER,
                    PERM_ENTREGABLE_VER, PERM_EVIDENCIA_VER, PERM_BENEFICIO_IMPACTO_VER,
                    PERM_DOCUMENTO_VER, PERM_CRONOGRAMA_VER, PERM_SISTEMA_VER,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS, PERM_SIDEBAR_REPORTES, PERM_SIDEBAR_ANALITICAS),
            "consulta", List.of(
                    PERM_DASHBOARD_VER, PERM_PROYECTO_VER, PERM_REPORTE_VER, PERM_ANALITICA_VER,
                    PERM_ENTREGABLE_VER, PERM_EVIDENCIA_VER, PERM_BENEFICIO_IMPACTO_VER,
                    PERM_DOCUMENTO_VER, PERM_CRONOGRAMA_VER,
                    PERM_SIDEBAR_DASHBOARD, PERM_SIDEBAR_PROYECTOS, PERM_SIDEBAR_REPORTES, PERM_SIDEBAR_ANALITICAS),
            "visualizador", List.of(
                    PERM_SIDEBAR_PROYECTOS, PERM_PROYECTO_VER)
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
