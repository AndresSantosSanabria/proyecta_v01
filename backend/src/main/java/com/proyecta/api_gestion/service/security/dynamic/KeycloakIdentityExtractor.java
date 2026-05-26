package com.proyecta.api_gestion.service.security.dynamic;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeycloakIdentityExtractor {

    public String resolveUsername(Authentication authentication) {
        Jwt jwt = resolveJwt(authentication);
        if (jwt == null) {
            return null;
        }

        return firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("username"),
                jwt.getSubject(),
                jwt.getClaimAsString("email"));
    }

    public String resolveSub(Authentication authentication) {
        Jwt jwt = resolveJwt(authentication);
        return jwt != null ? jwt.getSubject() : null;
    }

    public String resolveDisplayName(Authentication authentication) {
        Jwt jwt = resolveJwt(authentication);
        if (jwt == null) {
            return null;
        }

        return firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("given_name"), resolveUsername(authentication));
    }

    public String resolveEmail(Authentication authentication) {
        Jwt jwt = resolveJwt(authentication);
        if (jwt == null) {
            return null;
        }

        return firstNonBlank(jwt.getClaimAsString("email"), jwt.getClaimAsString("preferred_username"));
    }

    public String resolveDependencia(Authentication authentication) {
        Jwt jwt = resolveJwt(authentication);
        if (jwt == null) {
            return null;
        }

        return firstNonBlank(
                jwt.getClaimAsString("department"),
                jwt.getClaimAsString("organizational_unit"),
                jwt.getClaimAsString("branch"),
                jwt.getClaimAsString("department_code"));
    }

    public Set<String> resolveRealmAndClientRoles(Authentication authentication, Collection<String> resourceClientIds) {
        Jwt jwt = resolveJwt(authentication);
        if (jwt == null) {
            return Set.of();
        }

        Set<String> roles = new LinkedHashSet<>();

        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> realmMap) {
            addRoles(realmMap.get("roles"), roles);
        }

        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceMap) {
            for (String clientId : resourceClientIds) {
                Object clientAccess = resourceMap.get(clientId);
                if (clientAccess instanceof Map<?, ?> clientMap) {
                    addRoles(clientMap.get("roles"), roles);
                }
            }
        }

        return roles.stream()
                .map(role -> role.trim().toLowerCase(Locale.ROOT))
                .filter(role -> !role.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Jwt resolveJwt(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        return null;
    }

    private void addRoles(Object rolesObject, Set<String> roles) {
        if (rolesObject instanceof Collection<?> collection) {
            collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(roles::add);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }
}
