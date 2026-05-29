package com.proyecta.api_gestion.service.security.dynamic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class DynamicJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger logger = LoggerFactory.getLogger(DynamicJwtAuthoritiesConverter.class);

    private final List<String> resourceClientIds;
    private final SecurityCatalogCacheService catalogCacheService;
    private final KeycloakIdentityExtractor identityExtractor;

    public DynamicJwtAuthoritiesConverter(
            @Value("${gob.security.resource-client-ids:proyecta-web}") String resourceClientIds,
            SecurityCatalogCacheService catalogCacheService,
            KeycloakIdentityExtractor identityExtractor) {
        this.resourceClientIds = List.of(resourceClientIds.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.catalogCacheService = catalogCacheService;
        this.identityExtractor = identityExtractor;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Set<String> roles = identityExtractor.resolveRealmAndClientRoles(authenticationFrom(jwt), resourceClientIds);
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))));

        try {
            catalogCacheService.getPermissionsForRoles(roles).forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority("PERM_" + permission.toUpperCase(Locale.ROOT))));
        } catch (RuntimeException ex) {
            logger.warn("No se pudieron resolver permisos del catálogo de seguridad. Se continuó con roles del JWT: {}", ex.getMessage());
        }

        addScopes(jwt.getClaimAsString("scope"), authorities);
        addScopes(jwt.getClaimAsStringList("scp"), authorities);

        return authorities;
    }

    private void addScopes(String scopes, Set<GrantedAuthority> authorities) {
        if (scopes == null || scopes.isBlank()) {
            return;
        }

        for (String scope : scopes.split(" ")) {
            if (!scope.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope.trim()));
            }
        }
    }

    private void addScopes(Collection<String> scopes, Set<GrantedAuthority> authorities) {
        if (scopes == null) {
            return;
        }

        scopes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> new SimpleGrantedAuthority("SCOPE_" + value.trim()))
                .forEach(authorities::add);
    }

    private org.springframework.security.core.Authentication authenticationFrom(Jwt jwt) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(jwt, jwt.getTokenValue(), List.of());
    }
}
