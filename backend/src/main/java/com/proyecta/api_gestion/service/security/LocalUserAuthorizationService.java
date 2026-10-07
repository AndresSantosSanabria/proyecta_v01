package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.domain.exception.ForbiddenException;
import com.proyecta.api_gestion.domain.exception.UnauthorizedException;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.service.security.dynamic.SecurityRoleCatalog;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service("localUserAuthorization")
public class LocalUserAuthorizationService {

    private final SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort;
    private final KeycloakIdentityExtractor identityExtractor;

    public LocalUserAuthorizationService(
            SeguridadUsuarioRepositoryPort seguridadUsuarioRepositoryPort,
            KeycloakIdentityExtractor identityExtractor) {
        this.seguridadUsuarioRepositoryPort = seguridadUsuarioRepositoryPort;
        this.identityExtractor = identityExtractor;
    }

    public SeguridadUsuario requireLocalUser(Authentication authentication) {
        SeguridadUsuario usuario = resolveLocalUser(authentication);
        if (usuario == null) {
            throw new ForbiddenException("El usuario autenticado no existe en la base local de Proyecta.");
        }
        return usuario;
    }

    // S3516: la denegacion se realiza lanzando ForbiddenException; solo retorna
    // true cuando el acceso esta permitido (contrato usado por SpEL de @PreAuthorize).
    @SuppressWarnings("java:S3516")
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

    public SeguridadUsuario validateAndTouch(Authentication authentication) {
        SeguridadUsuario usuario = requireLocalUser(authentication);
        usuario.setUltimoAcceso(LocalDateTime.now(ZoneId.systemDefault()));
        return seguridadUsuarioRepositoryPort.save(usuario);
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
            usuario = seguridadUsuarioRepositoryPort.save(usuario);
        }

        return usuario;
    }

    private SeguridadUsuario findExistingUser(String keycloakSub, String email, String username) {
        if (keycloakSub != null) {
            var bySub = seguridadUsuarioRepositoryPort.findByKeycloakSubIgnoreCase(keycloakSub);
            if (bySub.isPresent()) return bySub.get();
        }

        if (email != null) {
            var byEmail = seguridadUsuarioRepositoryPort.findByCorreoIgnoreCase(email);
            if (byEmail.isPresent()) return byEmail.get();
        }

        if (username != null && !username.equalsIgnoreCase(email)) {
            var byUsername = seguridadUsuarioRepositoryPort.findByUsernameIgnoreCase(username);
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
                .anyMatch("admin"::equals);
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
