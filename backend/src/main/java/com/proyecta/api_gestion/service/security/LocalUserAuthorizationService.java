package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.UnauthorizedException;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.RolConfig;
import com.proyecta.api_gestion.repository.config.RolConfigRepository;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

@Service("localUserAuthorization")
public class LocalUserAuthorizationService {

    private final UsuarioRepository usuarioRepository;
    private final RolConfigRepository rolConfigRepository;
    private final KeycloakIdentityExtractor identityExtractor;

    public LocalUserAuthorizationService(
            UsuarioRepository usuarioRepository,
            RolConfigRepository rolConfigRepository,
            KeycloakIdentityExtractor identityExtractor) {
        this.usuarioRepository = usuarioRepository;
        this.rolConfigRepository = rolConfigRepository;
        this.identityExtractor = identityExtractor;
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
        Usuario usuario = requireLocalUser(authentication);
        if (usuario.esAdministrador()) {
            return true;
        }

        boolean hasAppAccess = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_APP_ACCESS"::equalsIgnoreCase);

        if (!hasAppAccess) {
            throw new ForbiddenException("El usuario no tiene el rol base requerido APP_ACCESS.");
        }

        return true;
    }

    public boolean hasAnyRole(Authentication authentication, String... allowedRoles) {
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
            throw new ForbiddenException("El usuario autenticado no tiene permisos para esta acción.");
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
            applyLocalRole(authentication, usuario);
            usuario.setActivo(true);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuario = usuarioRepository.save(usuario);
        }

        boolean shouldSave = false;
        if (keycloakSub != null && !keycloakSub.equalsIgnoreCase(clean(usuario.getKeycloakSub()))) {
            usuario.setKeycloakSub(keycloakSub);
            shouldSave = true;
        }

        shouldSave = applyLocalRole(authentication, usuario) || shouldSave;

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new ForbiddenException("El usuario autenticado está inactivo en la base local de Proyecta.");
        }

        RolConfig rolConfig = usuario.getRolConfig();
        if (rolConfig != null && Boolean.FALSE.equals(rolConfig.getActivo())) {
            throw new ForbiddenException("El rol asignado al usuario está inactivo en la base local de Proyecta.");
        }

        if (shouldSave) {
            usuario = usuarioRepository.save(usuario);
        }

        return usuario;
    }

    private boolean applyLocalRole(Authentication authentication, Usuario usuario) {
        if (usuario == null || usuario.getRolConfig() != null || authentication == null) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equalsIgnoreCase);

        if (!isAdmin) {
            return false;
        }

        RolConfig adminRole = rolConfigRepository.findByCodigo("ADMINISTRADOR").orElseGet(() -> {
            RolConfig nuevo = new RolConfig();
            nuevo.setCodigo("ADMINISTRADOR");
            nuevo.setNombre("Administrador");
            nuevo.setDescripcion("Acceso administrativo transversal");
            nuevo.setNivelAcceso(100);
            nuevo.setActivo(true);
            return rolConfigRepository.save(nuevo);
        });

        usuario.setRolConfig(adminRole);
        usuario.setRol(null);
        return true;
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
