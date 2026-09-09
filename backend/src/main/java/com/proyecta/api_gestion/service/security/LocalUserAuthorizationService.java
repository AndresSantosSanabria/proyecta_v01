package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.UnauthorizedException;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service("localUserAuthorization")
public class LocalUserAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(LocalUserAuthorizationService.class);

    private final SeguridadUsuarioRepository seguridadUsuarioRepository;
    private final KeycloakIdentityExtractor identityExtractor;
    private final Set<String> bootstrapAdminEmails;

    public LocalUserAuthorizationService(
            SeguridadUsuarioRepository seguridadUsuarioRepository,
            KeycloakIdentityExtractor identityExtractor,
            @Value("${gob.security.admin-emails:}") String adminEmails) {
        this.seguridadUsuarioRepository = seguridadUsuarioRepository;
        this.identityExtractor = identityExtractor;
        this.bootstrapAdminEmails = Arrays.stream(adminEmails.split(","))
                .map(this::clean)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public SeguridadUsuario requireLocalUser(Authentication authentication) {
        SeguridadUsuario usuario = resolveLocalUser(authentication);
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

        SeguridadUsuario usuario = requireLocalUser(authentication);
        if (esAdministrador(usuario)) {
            return true;
        }

        boolean hasFunctionalRole = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(SecurityRoleCatalog::normalize)
                .anyMatch(SecurityRoleCatalog::isProtected);

        boolean hasLocalRole = usuario.getRolCodigo() != null && !usuario.getRolCodigo().isBlank();

        if (!hasFunctionalRole && !hasLocalRole) {
            throw new ForbiddenException("El usuario no tiene un rol funcional valido en Proyecta.");
        }

        return true;
    }

    public boolean hasAnyRole(Authentication authentication, String... allowedRoles) {
        if (hasAdminAuthority(authentication)) {
            return true;
        }

        SeguridadUsuario usuario = requireLocalUser(authentication);
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

    public SeguridadUsuario validateAndTouch(Authentication authentication) {
        SeguridadUsuario usuario = requireLocalUser(authentication);
        usuario.setUltimoAcceso(LocalDateTime.now());
        return seguridadUsuarioRepository.save(usuario);
    }

    private SeguridadUsuario resolveLocalUser(Authentication authentication) {
        String keycloakSub = clean(identityExtractor.resolveSub(authentication));
        String email = clean(identityExtractor.resolveEmail(authentication));
        String username = clean(identityExtractor.resolveUsername(authentication));

        if (keycloakSub == null && email == null && username == null) {
            throw new UnauthorizedException("No se pudo identificar al usuario autenticado.");
        }

        SeguridadUsuario usuario = findExistingUser(keycloakSub, email, username);

        if (usuario == null) {
            return null;
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new ForbiddenException("El usuario autenticado esta inactivo en la base local de Proyecta.");
        }

        boolean shouldSave = false;
        if (keycloakSub != null && !keycloakSub.equalsIgnoreCase(clean(usuario.getKeycloakSub()))) {
            usuario.setKeycloakSub(keycloakSub);
            shouldSave = true;
        }

        if (shouldSave) {
            usuario = seguridadUsuarioRepository.save(usuario);
        }

        return usuario;
    }

    private SeguridadUsuario findExistingUser(String keycloakSub, String email, String username) {
        if (keycloakSub != null) {
            var bySub = seguridadUsuarioRepository.findByKeycloakSubIgnoreCase(keycloakSub);
            if (bySub.isPresent()) return bySub.get();
        }

        if (email != null) {
            var byEmail = seguridadUsuarioRepository.findByCorreoIgnoreCase(email);
            if (byEmail.isPresent()) return byEmail.get();
        }

        if (username != null && !username.equalsIgnoreCase(email)) {
            var byUsername = seguridadUsuarioRepository.findByUsernameIgnoreCase(username);
            if (byUsername.isPresent()) return byUsername.get();
        }

        return null;
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

    public static boolean esAdministrador(SeguridadUsuario usuario) {
        if (usuario == null || usuario.getRolCodigo() == null) {
            return false;
        }
        String normalized = SecurityRoleCatalog.normalize(usuario.getRolCodigo());
        return "admin".equals(normalized);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
