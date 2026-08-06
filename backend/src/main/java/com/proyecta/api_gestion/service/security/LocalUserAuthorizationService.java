package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.UnauthorizedException;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.RolConfig;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.config.RolConfigRepository;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;

@Service("localUserAuthorization")
public class LocalUserAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(LocalUserAuthorizationService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolConfigRepository rolConfigRepository;
    private final SeguridadUsuarioRepository seguridadUsuarioRepository;
    private final KeycloakIdentityExtractor identityExtractor;
    private final Set<String> bootstrapAdminEmails;

    public LocalUserAuthorizationService(
            UsuarioRepository usuarioRepository,
            RolConfigRepository rolConfigRepository,
            SeguridadUsuarioRepository seguridadUsuarioRepository,
            KeycloakIdentityExtractor identityExtractor,
            @Value("${gob.security.admin-emails:}") String adminEmails) {
        this.usuarioRepository = usuarioRepository;
        this.rolConfigRepository = rolConfigRepository;
        this.seguridadUsuarioRepository = seguridadUsuarioRepository;
        this.identityExtractor = identityExtractor;
        this.bootstrapAdminEmails = Arrays.stream(adminEmails.split(","))
                .map(this::clean)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public Usuario requireLocalUser(Authentication authentication) {
        Usuario usuario = resolveLocalUser(authentication);
        if (usuario == null) {
            throw new ForbiddenException("El usuario autenticado no existe en la base local de Proyecta.");
        }
        return usuario;
    }

    public boolean isValid(Authentication authentication) {
        requireLocalUser(authentication);
        return true;
    }

    public boolean hasBaseAccess(Authentication authentication) {
        if (hasAdminAuthority(authentication)) {
            return true;
        }

        Usuario usuario = requireLocalUser(authentication);
        if (usuario.esAdministrador()) {
            return true;
        }

        boolean hasFunctionalRole = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(SecurityRoleCatalog::normalize)
                .anyMatch(SecurityRoleCatalog::isProtected);

        boolean hasLocalRole = usuario.getRolConfig() != null || usuario.getRol() != null;

        if (!hasFunctionalRole && !hasLocalRole) {
            String username = identityExtractor.resolveUsername(authentication);
            if (username != null && !username.isBlank()) {
                Optional<SeguridadUsuario> segUsuario = seguridadUsuarioRepository.findByUsernameIgnoreCase(username);
                if (segUsuario.isPresent() && segUsuario.get().getRolCodigo() != null) {
                    String normalizedRole = SecurityRoleCatalog.normalize(segUsuario.get().getRolCodigo());
                    if (normalizedRole != null && SecurityRoleCatalog.isProtected(normalizedRole)) {
                        return true;
                    }
                }
            }
            throw new ForbiddenException("El usuario no tiene un rol funcional valido en Proyecta.");
        }

        return true;
    }

    public boolean hasAnyRole(Authentication authentication, String... allowedRoles) {
        if (hasAdminAuthority(authentication)) {
            return true;
        }

        Usuario usuario = requireLocalUser(authentication);
        String rolCodigo = SecurityRoleCatalog.normalize(clean(usuario.getRolCodigo()));
        if (rolCodigo == null) {
            throw new ForbiddenException("El usuario autenticado no tiene un rol local configurado.");
        }

        boolean allowed = Arrays.stream(allowedRoles)
                .filter(Objects::nonNull)
                .map(this::clean)
                .map(SecurityRoleCatalog::normalize)
                .anyMatch(rolCodigo::equals);

        if (!allowed) {
            throw new ForbiddenException("El usuario autenticado no tiene permisos para esta accion.");
        }

        return true;
    }

    public Usuario validateAndTouch(Authentication authentication) {
        Usuario usuario = requireLocalUser(authentication);
        usuario.setUltimoAcceso(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private Usuario resolveLocalUser(Authentication authentication) {
        String keycloakSub = clean(identityExtractor.resolveSub(authentication));
        String email = clean(identityExtractor.resolveEmail(authentication));
        String username = clean(identityExtractor.resolveUsername(authentication));
        String displayName = clean(identityExtractor.resolveDisplayName(authentication));

        if (keycloakSub == null && email == null && username == null) {
            throw new UnauthorizedException("No se pudo identificar al usuario autenticado.");
        }

        Usuario usuario = null;
        if (keycloakSub != null) {
            usuario = usuarioRepository.findByKeycloakSubIgnoreCase(keycloakSub).orElse(null);
        }

        if (usuario == null && email != null) {
            usuario = usuarioRepository.findByCorreoIgnoreCase(email).orElse(null);
        }

        if (usuario == null && username != null && !username.equalsIgnoreCase(email)) {
            usuario = usuarioRepository.findByCorreoIgnoreCase(username).orElse(null);
        }

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setKeycloakSub(keycloakSub);
            usuario.setNombre(firstNonBlank(displayName, username, email, keycloakSub));
            usuario.setCorreo(firstNonBlank(email, username, keycloakSub));
            applyDefaultRole(usuario);
            usuario.setActivo(true);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuario = usuarioRepository.save(usuario);
            logger.info("Usuario auto-provisionado con rol VISUALIZADOR: {}", usuario.getCorreo());
        }

        boolean shouldSave = false;
        if (keycloakSub != null && !keycloakSub.equalsIgnoreCase(clean(usuario.getKeycloakSub()))) {
            usuario.setKeycloakSub(keycloakSub);
            shouldSave = true;
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new ForbiddenException("El usuario autenticado esta inactivo en la base local de Proyecta.");
        }

        RolConfig rolConfig = usuario.getRolConfig();
        if (rolConfig != null && Boolean.FALSE.equals(rolConfig.getActivo())) {
            throw new ForbiddenException("El rol asignado al usuario esta inactivo en la base local de Proyecta.");
        }

        if (shouldSave) {
            usuario = usuarioRepository.save(usuario);
        }

        return usuario;
    }

    private void applyDefaultRole(Usuario usuario) {
        RolConfig viewerRole = rolConfigRepository.findByCodigo("VISUALIZADOR").orElseGet(() -> {
            RolConfig nuevo = new RolConfig();
            nuevo.setCodigo("VISUALIZADOR");
            nuevo.setNombre("Visualizador");
            nuevo.setDescripcion("Acceso de solo lectura por defecto");
            nuevo.setNivelAcceso(10);
            nuevo.setActivo(true);
            return rolConfigRepository.save(nuevo);
        });

        usuario.setRolConfig(viewerRole);
        usuario.setRol(null);
    }

    public boolean hasAdminAuthority(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(SecurityRoleCatalog::normalize)
                .anyMatch(role -> "admin".equals(role));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
