package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.domain.exception.ForbiddenException;
import com.proyecta.api_gestion.domain.model.Proyecto;

import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

// S3516: metodos de autorizacion cuya denegacion se realiza lanzando
// ForbiddenException; solo retornan true cuando el acceso esta permitido y el
// boolean existe exclusivamente como expresion SpEL de @PreAuthorize.
@SuppressWarnings("java:S3516")
@Component("proyectoSecurity")
public class ProyectoSecurity {
    private static final Logger logger = LoggerFactory.getLogger(ProyectoSecurity.class);

    private static final String GESTOR_TIC = "gestor_tic";
    private static final String GESTOR_PROYECTOS = "gestor_proyectos";
    private static final String DIRECTOR_PROYECTO = "director_proyecto";
    private static final String PERMISO_PROYECTO_VER = "PROYECTO:VER";
    private static final String MSG_PROYECTO_NO_EXISTE = "El proyecto solicitado no existe.";
    private static final String MSG_NO_ASIGNADO = "El usuario no esta asignado al proyecto solicitado.";
    private static final String MSG_NO_ASIGNADO_TILDE = "El usuario no está asignado al proyecto solicitado.";

    private static final Set<String> EVIDENCE_REVIEW_ROLE_CODES = Set.of(GESTOR_TIC, GESTOR_PROYECTOS);
    private static final Set<String> DOCUMENT_HISTORY_ROLE_CODES = Set.of(GESTOR_PROYECTOS);
    private static final Set<String> PROJECT_STRUCTURE_MANAGER_ROLE_CODES = Set.of(GESTOR_TIC, GESTOR_PROYECTOS);
    private static final Set<String> PROJECT_STRUCTURE_EDITOR_ROLE_CODES = Set.of(GESTOR_TIC, GESTOR_PROYECTOS, DIRECTOR_PROYECTO);
    private static final Set<String> CHANGE_DEADLINE_ROLE_CODES = Set.of(GESTOR_TIC, GESTOR_PROYECTOS);
    private static final Set<String> DIRECTOR_NOTIFICATION_ROLE_CODES = Set.of(GESTOR_TIC, GESTOR_PROYECTOS);
    private static final Set<String> DIRECTOR_BLOCKED_PERMISSIONS = Set.of(
            "PROYECTO:CREAR",
            "PROYECTO:CERRAR",
            "ENTREGABLE:CREAR",
            "ENTREGABLE:EDITAR",
            "ENTREGABLE:APROBAR",
            "ENTREGABLE:CAMBIAR_FECHA",
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
    private final SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort;
    private final SeguridadUsuarioProyectoRepositoryPort seguridadUsuarioProyectoRepositoryPort;
    private final ProyectoRepositoryPort proyectoRepositoryPort;

    public ProyectoSecurity(
            KeycloakIdentityExtractor identityExtractor,
            SecurityCatalogCacheService catalogCacheService,
            LocalUserAuthorizationService localUserAuthorizationService,
            PermisoUsuarioService permisoUsuarioService,
            SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort,
            SeguridadUsuarioProyectoRepositoryPort seguridadUsuarioProyectoRepositoryPort,
            ProyectoRepositoryPort proyectoRepositoryPort) {
        this.identityExtractor = identityExtractor;
        this.catalogCacheService = catalogCacheService;
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.permisoUsuarioService = permisoUsuarioService;
        this.seguridadUsuarioRepositoryPort = seguridadUsuarioRepositoryPort;
        this.seguridadUsuarioProyectoRepositoryPort = seguridadUsuarioProyectoRepositoryPort;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
    }

    private void logCanAccess(String username, String normalizedPermission, String proyectoId, Set<String> roleCodes) {
        if (logger.isDebugEnabled()) {
            logger.debug("canAccess: user='{}', perm='{}', project='{}', roles={}",
                    com.proyecta.api_gestion.infrastructure.LogSanitizer.clean(username),
                    normalizedPermission,
                    com.proyecta.api_gestion.infrastructure.LogSanitizer.clean(proyectoId),
                    roleCodes);
        }
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
        logCanAccess(username, normalizedPermission, proyectoId, roleCodes);
        if (isDirectorOnly(roleCodes) && DIRECTOR_BLOCKED_PERMISSIONS.contains(normalizedPermission)) {
            throw new ForbiddenException("El Director de Proyecto solo puede cargar, reemplazar y subsanar evidencias de sus proyectos asignados.");
        }

        if (isDirectorOnly(roleCodes)
                && proyectoId != null && !proyectoId.isBlank()
                && !DIRECTOR_BLOCKED_PERMISSIONS.contains(normalizedPermission)
                && (catalogCacheService.isAssignedToProject(username, proyectoId)
                    || isProjectDirector(username, proyectoId))) {
            return true;
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

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)
                && !isProjectDirector(username, proyectoId)) {
            logger.warn("ACCESS DENIED: user='{}', project='{}', roles={}, isDirectorOnly={}, perm='{}'",
                    username, proyectoId, roleCodes, isDirectorOnly(roleCodes), normalizedPermission);
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

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (!isTransversal(roleCodes)) {
            throw new ForbiddenException("El acceso global solo esta permitido para roles transversales. Use el endpoint de proyectos asignados.");
        }

        return true;
    }

    public boolean canAccessOperational(String permissionCode, String proyectoId, Authentication authentication) {
        canAccess(permissionCode, proyectoId, authentication);
        assertOperationalProjectReady(proyectoId, authentication, permissionCode);
        return true;
    }

    /**
     * Variante no lanzadora de {@link #canAccess}: devuelve {@code false} cuando el
     * usuario no tiene acceso al proyecto en lugar de propagar {@link ForbiddenException}.
     * Util para endpoints de listado (p.ej. informes de avance pendientes) que deben
     * filtrar proyectos usando exactamente la misma regla de autorizacion que
     * {@code GET /proyectos/{id}} y {@code GET /proyectos/{id}/avance}.
     */
    public boolean canAccessQuietly(String permissionCode, String proyectoId, Authentication authentication) {
        try {
            return canAccess(permissionCode, proyectoId, authentication);
        } catch (ForbiddenException _) {
            logger.debug("canAccessQuietly: denegado user project perm reason={}",
                    proyectoId);
            return false;
        } catch (RuntimeException _) {
            logger.warn("canAccessQuietly: error evaluando acceso user project perm: {}",
                    proyectoId);
            return false;
        }
    }

    public boolean canSendDirectorNotification(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean allowedRole = roleCodes.stream().anyMatch(DIRECTOR_NOTIFICATION_ROLE_CODES::contains);
        if (!allowedRole) {
            throw new ForbiddenException("Solo el Administrador o un Gestor pueden enviar notificaciones al director.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(PERMISO_PROYECTO_VER::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: PROYECTO:VER");
        }

        if (isTransversal(roleCodes)) {
            return true;
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException(MSG_NO_ASIGNADO);
        }

        return true;
    }

    /**
     * Cierre forzoso / extraordinario desde la lista de proyectos, sin necesidad de
     * entrar al proyecto y sin importar si tiene documentacion inicial cargada.
     * Permitido solo para Administrador o Gestores (gestor_tic / gestor_proyectos).
     */
    public boolean canForceCloseExtraordinary(Authentication authentication) {
        return reviewGate(null, authentication, EVIDENCE_REVIEW_ROLE_CODES,
                "Solo el Administrador o un Gestor pueden realizar el cierre extraordinario del proyecto.",
                PERMISO_PROYECTO_VER, MSG_NO_ASIGNADO);
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

        if (isTransversal(roleCodes)) {
            return true;
        }

        // CWE-862: ademas del permiso global, el usuario no transversal debe estar
        // asignado al proyecto (misma regla que canEditBenefitImpact / canAccess).
        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException(MSG_NO_ASIGNADO);
        }

        return true;
    }

    /**
     * Autoriza a aprobar u observar la informacion de beneficio e impacto (CWE-862).
     * Solo Administrador o roles Gestor con permiso BENEFICIO_IMPACTO:APROBAR.
     * El Director de Proyecto solo diligencia; la revision la hacen los Gestores.
     */
    public boolean canReviewBenefitImpact(String proyectoId, Authentication authentication) {
        return reviewGate(proyectoId, authentication, EVIDENCE_REVIEW_ROLE_CODES,
                "Solo un Gestor puede revisar la informacion de beneficio e impacto.",
                "BENEFICIO_IMPACTO:APROBAR", MSG_NO_ASIGNADO);
    }

    /**
     * Autoriza la consulta de KPIs y resumen del dashboard (CWE-862).
     * Requiere el permiso DASHBOARD:VER. A diferencia de canAccessGlobal no exige
     * rol transversal: directores, auditores y consultas usan el dashboard por diseño.
     */
    public boolean canViewDashboard(Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch("DASHBOARD:VER"::equals);
        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: DASHBOARD:VER");
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

        resolveEffectiveRoleCodes(authentication);
        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch("BENEFICIO_IMPACTO:EDITAR"::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: BENEFICIO_IMPACTO:EDITAR");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("Solo el Director de Proyecto asignado puede diligenciar esta informacion.");
        }

        return true;
    }

    public boolean canCompleteInitialRegistration(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            throw new ForbiddenException("No se pudo evaluar el proyecto solicitado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (isTransversal(roleCodes)) {
            Proyecto proyecto = proyectoRepositoryPort.findById(normalizeProjectId(proyectoId))
                    .orElseThrow(() -> new ForbiddenException(MSG_PROYECTO_NO_EXISTE));
            if (!proyecto.requiereCompletitudDirector()) {
                throw new ForbiddenException("El proyecto no esta pendiente de completar.");
            }
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("Solo el Director de Proyecto asignado puede completar la informacion inicial.");
        }

        Proyecto proyecto = proyectoRepositoryPort.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ForbiddenException(MSG_PROYECTO_NO_EXISTE));
        if (!proyecto.requiereCompletitudDirector()) {
            throw new ForbiddenException("El proyecto no esta pendiente de completar.");
        }

        return true;
    }

    public boolean canReviewEvidence(String proyectoId, Authentication authentication) {
        return reviewGate(proyectoId, authentication, EVIDENCE_REVIEW_ROLE_CODES,
                "Solo el Gestor de Proyectos puede aprobar u observar evidencias.",
                "ENTREGABLE:APROBAR", MSG_NO_ASIGNADO_TILDE);
    }

    /**
     * Gate de carga del informe de avance: solo Director de Proyecto asignado (o admin).
     */
    public boolean canUploadAdvanceReport(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean director = roleCodes.stream().anyMatch(DIRECTOR_PROYECTO::equalsIgnoreCase);
        if (!director) {
            throw new ForbiddenException("Solo el Director de Proyecto puede cargar el informe de avance.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)
                && !isProjectDirector(username, proyectoId)) {
            throw new ForbiddenException(MSG_NO_ASIGNADO_TILDE);
        }

        return true;
    }

    /**
     * Autoriza a verificar o devolver un informe de avance (CWE-639 / CWE-862).
     * Solo Administrador o roles Gestor (transversal o asignado al proyecto) con
     * permiso EVIDENCIA:APROBAR. El Director de Proyecto NO puede validar su
     * propio informe: evita auto-aprobacion.
     */
    public boolean canReviewAdvanceReport(String proyectoId, Authentication authentication) {
        return reviewGate(proyectoId, authentication, EVIDENCE_REVIEW_ROLE_CODES,
                "Solo un Gestor de Proyectos puede verificar o devolver informes de avance.",
                "EVIDENCIA:APROBAR", MSG_NO_ASIGNADO);
    }

    /**
     * Puerta de revision generica: admin incondicional, identidad obligatoria, rol
     * permitido, permiso funcional y (si aplica) asignacion al proyecto.
     */
    private boolean reviewGate(String proyectoId, Authentication authentication, Set<String> allowedRoleCodes,
                               String roleMessage, String permissionCode, String assignmentMessage) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean allowedRole = roleCodes.stream().anyMatch(allowedRoleCodes::contains);
        if (!allowedRole) {
            throw new ForbiddenException(roleMessage);
        }

        boolean hasPermission = permisoUsuarioService.getEffectivePermissions(username).stream()
                .map(this::normalize)
                .anyMatch(normalize(permissionCode)::equals);
        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + permissionCode);
        }

        if (isTransversal(roleCodes) || proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException(assignmentMessage);
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
            throw new ForbiddenException(MSG_NO_ASIGNADO);
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
        boolean isAssignedDirector = roleCodes.contains(DIRECTOR_PROYECTO) && !isTransversal(roleCodes);

        if (!hasPermission && !isAssignedDirector) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        if (isTransversal(roleCodes) || proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException(MSG_NO_ASIGNADO);
        }

        assertOperationalProjectReady(proyectoId, authentication);
        return true;
    }

    public boolean canChangeDescription(String proyectoId, Authentication authentication) {
        return canChangeDeadline(proyectoId, authentication);
    }

    public boolean canChangeDeadline(String proyectoId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("No fue posible identificar el usuario autenticado.");
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        boolean allowedRole = roleCodes.stream().anyMatch(CHANGE_DEADLINE_ROLE_CODES::contains);
        if (!allowedRole) {
            throw new ForbiddenException("Solo el Administrador o un Gestor pueden modificar la fecha limite de entregables.");
        }

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        String normalizedPermission = normalize("ENTREGABLE:CAMBIAR_FECHA");
        boolean hasPermission = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(normalizedPermission::equals);

        if (!hasPermission) {
            throw new ForbiddenException("El usuario no posee el permiso funcional requerido: " + normalizedPermission);
        }

        if (isTransversal(roleCodes) || proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException("El usuario no est\u00e1 asignado al proyecto solicitado.");
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
        if (!roleCodes.contains(DIRECTOR_PROYECTO)) {
            throw new ForbiddenException("Solo el Director de Proyecto puede marcar observaciones como subsanadas.");
        }

        if (proyectoId == null || proyectoId.isBlank()) {
            return true;
        }

        if (!catalogCacheService.isAssignedToProject(username, proyectoId)) {
            throw new ForbiddenException(MSG_NO_ASIGNADO);
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

        Set<String> effectivePermissions = permisoUsuarioService.getEffectivePermissions(username);
        boolean canViewProjects = effectivePermissions.stream()
                .map(this::normalize)
                .anyMatch(PERMISO_PROYECTO_VER::equals);
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
            SeguridadUsuario usuario = localUserAuthorizationService.requireLocalUser(authentication);
            return LocalUserAuthorizationService.esAdministrador(usuario);
        } catch (RuntimeException _) {
            return false;
        }
    }

    private Set<String> resolveRoleCodes(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(SecurityRoleCatalog::normalize)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<String> resolveEffectiveRoleCodes(Authentication authentication) {
        Set<String> roleCodes = new LinkedHashSet<>(resolveRoleCodes(authentication));

        try {
            String username = identityExtractor.resolveUsername(authentication);
            if (username != null && !username.isBlank()) {
                SeguridadUsuario segUsuario = seguridadUsuarioRepositoryPort.findByUsernameIgnoreCase(username).orElse(null);
                if (segUsuario != null && segUsuario.getRolCodigo() != null && !segUsuario.getRolCodigo().isBlank()) {
                    String securityRole = SecurityRoleCatalog.normalize(segUsuario.getRolCodigo());
                    if (securityRole != null && !securityRole.isBlank()) {
                        roleCodes.add(securityRole);
                    }
                }
            }
        } catch (RuntimeException _) {
            logger.debug("No se pudo resolver rol desde SeguridadUsuario para resolveEffectiveRoleCodes");
        }

        return roleCodes;
    }

    private boolean isTransversal(Collection<String> roleCodes) {
        return roleCodes.stream()
                .anyMatch(SecurityRoleCatalog::isTransversal);
    }

    private boolean isDirectorOnly(Collection<String> roleCodes) {
        return roleCodes.contains(DIRECTOR_PROYECTO)
                && roleCodes.stream().noneMatch(PROJECT_STRUCTURE_MANAGER_ROLE_CODES::contains)
                && !roleCodes.contains("admin");
    }

    /**
     * Verifica si el usuario es el director asignado al proyecto mediante una consulta
     * JPQL directa, sin cargar la relacion lazy directorUsuario. Esto previene
     * LazyInitializationException en contextos de seguridad (fuera de transaccion).
     * <p>
     * Adicionalmente consulta la tabla usuario_proyecto buscando el cargo de director
     * como fallback para cubrir proyectos cuya asignacion se gestiona solo por esa tabla.
     */
    private boolean isProjectDirector(String username, String proyectoId) {
        if (username == null || username.isBlank() || proyectoId == null || proyectoId.isBlank()) {
            return false;
        }
        try {
            // Consulta directa sobre campo director_usuario_id (sin lazy load).
            if (proyectoRepositoryPort.existsDirectorByProyectoIdAndUsername(proyectoId.trim(), username.trim())) {
                logger.debug("isProjectDirector: user='{}' es director (via director_usuario_id) del proyecto '{}'",
                        username, proyectoId);
                return true;
            }
        } catch (Exception e) {
            logger.warn("isProjectDirector: error consultando director_usuario_id para proyecto='{}', user='{}': {}",
                    proyectoId, username, e.getMessage());
        }

        // Fallback: asignacion activa en usuario_proyecto con cargo de director
        // (cubre proyectos legados cuyo director_usuario_id quedo NULL).
        try {
            if (usuarioProyectoHasDirectorCargo(username, proyectoId)) {
                logger.debug("isProjectDirector: user='{}' es director (via usuario_proyecto) del proyecto '{}'",
                        username, proyectoId);
                return true;
            }
        } catch (Exception e) {
            logger.warn("isProjectDirector: error consultando usuario_proyecto para proyecto='{}', user='{}': {}",
                    proyectoId, username, e.getMessage());
        }
        return false;
    }

    private boolean usuarioProyectoHasDirectorCargo(String username, String proyectoId) {
        return seguridadUsuarioProyectoRepositoryPort
                .findByUsuarioUsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(
                        username.trim(), proyectoId.trim(), "DIRECTOR_PROYECTO")
                .map(asignacion -> Boolean.TRUE.equals(asignacion.getActivo()))
                .orElse(false);
    }

    private static final Set<String> COMPLETION_ALLOWED_PERMISSIONS = Set.of(
            "DOCUMENTO:CARGAR",
            PERMISO_PROYECTO_VER
    );

    private void assertOperationalProjectReady(String proyectoId, Authentication authentication) {
        assertOperationalProjectReady(proyectoId, authentication, null);
    }

    private void assertOperationalProjectReady(String proyectoId, Authentication authentication, String permissionCode) {
        if (proyectoId == null || proyectoId.isBlank() || isAdmin(authentication)) {
            return;
        }

        Set<String> roleCodes = resolveEffectiveRoleCodes(authentication);
        if (isTransversal(roleCodes)) {
            return;
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || !catalogCacheService.isAssignedToProject(username, proyectoId)) {
            return;
        }

        Proyecto proyecto = proyectoRepositoryPort.findById(normalizeProjectId(proyectoId))
                .orElseThrow(() -> new ForbiddenException(MSG_PROYECTO_NO_EXISTE));
        if (proyecto.requiereCompletitudDirector()) {
            String normalized = normalize(permissionCode);
            if (normalized != null && COMPLETION_ALLOWED_PERMISSIONS.contains(normalized)) {
                return;
            }
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
