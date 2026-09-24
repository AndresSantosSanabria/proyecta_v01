package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificationActorResolver {

    private final KeycloakIdentityExtractor identityExtractor;
    private final SeguridadUsuarioRepository usuarioRepository;

    public NotificationActorResolver(KeycloakIdentityExtractor identityExtractor,
                                     SeguridadUsuarioRepository usuarioRepository) {
        this.identityExtractor = identityExtractor;
        this.usuarioRepository = usuarioRepository;
    }

    public Set<String> resolveActorIdentifiers(String fallbackActorUsername) {
        Set<String> identifiers = new LinkedHashSet<>();
        addIfPresent(identifiers, normalize(fallbackActorUsername));
        expandWithLocalUser(identifiers, fallbackActorUsername);

        Authentication authentication = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (authentication != null) {
            String contextUsername = identityExtractor.resolveUsername(authentication);
            addIfPresent(identifiers, normalize(contextUsername));
            expandWithLocalUser(identifiers, contextUsername);
            addIfPresent(identifiers, normalize(identityExtractor.resolveEmail(authentication)));
            addIfPresent(identifiers, normalize(identityExtractor.resolveSub(authentication)));

            Jwt jwt = identityExtractor.resolveJwt(authentication);
            if (jwt != null) {
                addIfPresent(identifiers, normalize(jwt.getClaimAsString("preferred_username")));
                addIfPresent(identifiers, normalize(jwt.getClaimAsString("email")));
                addIfPresent(identifiers, normalize(jwt.getSubject()));
            }
        }

        return identifiers;
    }

    private void expandWithLocalUser(Set<String> identifiers, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        String value = candidate.trim();
        usuarioRepository.findByUsernameIgnoreCase(value)
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(value))
                .ifPresent(user -> addLocalUserIdentifiers(identifiers, user));
        usuarioRepository.findByNombreIgnoreCase(value)
                .forEach(user -> addLocalUserIdentifiers(identifiers, user));
    }

    private void addLocalUserIdentifiers(Set<String> identifiers, SeguridadUsuario user) {
        if (user == null) {
            return;
        }
        addIfPresent(identifiers, normalize(user.getUsername()));
        addIfPresent(identifiers, normalize(user.getCorreo()));
        addIfPresent(identifiers, normalize(user.getNombre()));
        addIfPresent(identifiers, normalize(user.getKeycloakSub()));
    }

    public boolean isActor(String candidate, Set<String> actorIdentifiers) {
        String normalizedCandidate = normalize(candidate);
        return normalizedCandidate != null
                && actorIdentifiers.stream()
                .anyMatch(identifier -> identifier.equalsIgnoreCase(normalizedCandidate));
    }

    private void addIfPresent(Set<String> identifiers, String value) {
        if (value != null && !value.isBlank()) {
            identifiers.add(value);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
