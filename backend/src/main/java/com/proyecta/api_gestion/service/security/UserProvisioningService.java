package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.model.security.SeguridadRol;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.security.SeguridadRolRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Servicio de aprovisionamiento JIT (Just-In-Time / Upsert) de usuarios.
 *
 * Flujo garantizado en cada petición autenticada:
 *  1. Extrae username, email, sub, nombre y dependencia del JWT.
 *  2. Busca el registro en BD por keycloak_sub → email → username (orden de prioridad).
 *  3a. Si NO existe  → INSERT con rol por defecto ("visualizador") y saveAndFlush.
 *  3b. Si YA existe  → UPDATE solo los campos que hayan cambiado y saveAndFlush.
 *
 * Usa Propagation.REQUIRES_NEW para forzar una transacción autónoma que siempre
 * hace commit, incluso cuando es llamado desde un Servlet Filter (fuera de cualquier
 * transacción externa) o desde otro método @Transactional.
 */
@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    /** Rol que se asigna al primer login de un usuario nuevo. */
    private static final String DEFAULT_ROLE = "visualizador";

    private final SeguridadUsuarioRepository usuarioRepository;
    private final SeguridadRolRepository     rolRepository;
    private final KeycloakIdentityExtractor  identityExtractor;

    public UserProvisioningService(
            SeguridadUsuarioRepository usuarioRepository,
            SeguridadRolRepository rolRepository,
            KeycloakIdentityExtractor identityExtractor) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository     = rolRepository;
        this.identityExtractor = identityExtractor;
    }

    // =========================================================================
    //  Punto de entrada público
    // =========================================================================

    /**
     * Ejecuta el upsert del usuario autenticado.
     *
     * REQUIRES_NEW: crea una transacción independiente y hace commit antes de retornar,
     * garantizando que el usuario quede persistido sin importar el estado del
     * contexto transaccional del llamador.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SeguridadUsuario upsert(Authentication authentication) {
        // — 1. Extraer claims del JWT ——————————————————————————————————————————
        String username = trim(identityExtractor.resolveUsername(authentication));
        String email    = normalizeEmail(identityExtractor.resolveEmail(authentication));
        String nombre   = trim(identityExtractor.resolveDisplayName(authentication));
        String sub      = trim(identityExtractor.resolveSub(authentication));
        String dep      = trim(identityExtractor.resolveDependencia(authentication));

        if (username == null) {
            log.warn("[JIT-Upsert] JWT sin username válido — aprovisionamiento omitido.");
            return null;
        }

        log.debug("[JIT-Upsert] Procesando usuario: username='{}', sub='{}', email='{}'",
                username, sub, email);

        // — 2. Buscar registro existente (sub > email > username) ——————————————
        SeguridadUsuario usuario = findExistingUser(sub, email, username);

        // — 3a. INSERT ——————————————————————————————————————————————————————————
        if (usuario == null) {
            return crearNuevoUsuario(username, email, nombre, sub, dep);
        }

        // — 3b. UPDATE ——————————————————————————————————————————————————————————
        return actualizarUsuarioExistente(usuario, username, email, nombre, sub, dep);
    }

    // =========================================================================
    //  INSERT
    // =========================================================================

    private SeguridadUsuario crearNuevoUsuario(
            String username, String email, String nombre, String sub, String dep) {

        log.info("[JIT-Upsert] Usuario nuevo detectado — creando registro: username='{}'", username);

        SeguridadUsuario nuevo = new SeguridadUsuario();
        nuevo.setUsername(username);
        nuevo.setKeycloakSub(sub != null ? sub : username);
        nuevo.setNombre(nombre != null ? nombre : username);
        nuevo.setCorreo(resolveCorreo(email, username));
        nuevo.setDependencia(dep);
        nuevo.setActivo(true);

        // Asignar rol por defecto
        Optional<SeguridadRol> rolOpt = rolRepository.findByCodigoIgnoreCase(DEFAULT_ROLE);
        if (rolOpt.isPresent()) {
            SeguridadRol rol = rolOpt.get();
            nuevo.setRolCodigo(rol.getCodigo());
            nuevo.setRolNombre(rol.getNombre());
            log.info("[JIT-Upsert] Rol '{}' asignado al nuevo usuario.", rol.getCodigo());
        } else {
            log.warn("[JIT-Upsert] Rol por defecto '{}' no encontrado. El usuario quedará sin rol.", DEFAULT_ROLE);
        }

        try {
            SeguridadUsuario saved = usuarioRepository.saveAndFlush(nuevo);
            log.info("[JIT-Upsert] ✅ Usuario CREADO — ID={}, username='{}'",
                    saved.getId(), saved.getUsername());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Condición de carrera: otro hilo ya lo creó entre el SELECT y el INSERT.
            // Recuperamos el registro y retornamos el existente.
            log.warn("[JIT-Upsert] Conflicto de unicidad al crear '{}' — recuperando registro existente.", username);
            return findExistingUser(sub, email, username);
        }
    }

    // =========================================================================
    //  UPDATE
    // =========================================================================

    private SeguridadUsuario actualizarUsuarioExistente(
            SeguridadUsuario usuario,
            String username, String email, String nombre, String sub, String dep) {

        boolean changed = false;

        // keycloak_sub
        if (sub != null && !sub.equalsIgnoreCase(trim(usuario.getKeycloakSub()))) {
            log.debug("[JIT-Upsert] keycloak_sub actualizado: '{}' → '{}'",
                    usuario.getKeycloakSub(), sub);
            usuario.setKeycloakSub(sub);
            changed = true;
        }

        // nombre
        if (nombre != null && !nombre.equalsIgnoreCase(trim(usuario.getNombre()))) {
            log.debug("[JIT-Upsert] nombre actualizado: '{}' → '{}'",
                    usuario.getNombre(), nombre);
            usuario.setNombre(nombre);
            changed = true;
        }

        // correo
        String resolvedEmail = normalizeEmail(email);
        if (resolvedEmail != null && !resolvedEmail.equalsIgnoreCase(normalizeEmail(usuario.getCorreo()))) {
            log.debug("[JIT-Upsert] correo actualizado: '{}' → '{}'",
                    usuario.getCorreo(), resolvedEmail);
            usuario.setCorreo(resolvedEmail);
            changed = true;
        }

        // username
        if (!username.equalsIgnoreCase(trim(usuario.getUsername()))) {
            log.debug("[JIT-Upsert] username actualizado: '{}' → '{}'",
                    usuario.getUsername(), username);
            usuario.setUsername(username);
            changed = true;
        }

        // dependencia
        if (dep != null && !dep.equalsIgnoreCase(trim(usuario.getDependencia()))) {
            log.debug("[JIT-Upsert] dependencia actualizada: '{}' → '{}'",
                    usuario.getDependencia(), dep);
            usuario.setDependencia(dep);
            changed = true;
        }

        // reactivar si estaba desactivado
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.info("[JIT-Upsert] Usuario '{}' estaba inactivo — reactivando.", username);
            usuario.setActivo(true);
            changed = true;
        }

        if (changed) {
            SeguridadUsuario saved = usuarioRepository.saveAndFlush(usuario);
            log.info("[JIT-Upsert] ✅ Usuario ACTUALIZADO — ID={}, username='{}'",
                    saved.getId(), saved.getUsername());
            return saved;
        }

        log.debug("[JIT-Upsert] Usuario '{}' sin cambios — nada que persistir.", username);
        return usuario;
    }

    // =========================================================================
    //  Helpers de búsqueda
    // =========================================================================

    /**
     * Busca en orden de prioridad: keycloak_sub > correo > username.
     * Retorna el primer registro encontrado o null si no existe ninguno.
     */
    private SeguridadUsuario findExistingUser(String sub, String email, String username) {
        if (hasValue(sub)) {
            Optional<SeguridadUsuario> bySub = usuarioRepository.findByKeycloakSubIgnoreCase(sub);
            if (bySub.isPresent()) {
                log.debug("[JIT-Upsert] Registro encontrado por keycloak_sub='{}'", sub);
                return bySub.get();
            }
        }
        if (hasValue(email)) {
            Optional<SeguridadUsuario> byEmail = usuarioRepository.findByCorreoIgnoreCase(email);
            if (byEmail.isPresent()) {
                log.debug("[JIT-Upsert] Registro encontrado por correo='{}'", email);
                return byEmail.get();
            }
        }
        if (hasValue(username)) {
            Optional<SeguridadUsuario> byUsername = usuarioRepository.findByUsernameIgnoreCase(username);
            if (byUsername.isPresent()) {
                log.debug("[JIT-Upsert] Registro encontrado por username='{}'", username);
                return byUsername.get();
            }
        }
        return null;
    }

    // =========================================================================
    //  Helpers de normalización
    // =========================================================================

    /**
     * El campo correo es NOT NULL + UNIQUE.
     * Si el JWT no trae email, construimos un pseudo-email local para evitar
     * constraint violations al insertar.
     */
    private String resolveCorreo(String email, String username) {
        if (hasValue(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        return username.trim().toLowerCase(Locale.ROOT) + "@local.proyecta";
    }

    private String normalizeEmail(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }

    private String trim(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isBlank() ? null : v;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
