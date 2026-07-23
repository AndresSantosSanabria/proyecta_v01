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

    private static final String KEYCLOAK_ADMIN_ROLE = "admin";
    private static final String KEYCLOAK_USUARIO_ROLE = "usuario";

    private final List<String> resourceClientIds;
    private final SecurityCatalogCacheService catalogCacheService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final RoleAliasService roleAliasService;

    public DynamicJwtAuthoritiesConverter(
            @Value("${gob.security.resource-client-ids}") String resourceClientIds,
            SecurityCatalogCacheService catalogCacheService,
            KeycloakIdentityExtractor identityExtractor,
            RoleAliasService roleAliasService) {
        this.resourceClientIds = List.of(resourceClientIds.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.catalogCacheService = catalogCacheService;
        this.identityExtractor = identityExtractor;
        this.roleAliasService = roleAliasService;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Set<String> keycloakRoles = identityExtractor.resolveRealmAndClientRoles(authenticationFrom(jwt), resourceClientIds).stream()
                .map(role -> role.trim().toLowerCase(Locale.ROOT))
                .filter(role -> !role.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        boolean isAdmin = keycloakRoles.stream()
                .anyMatch(role -> role.equals(KEYCLOAK_ADMIN_ROLE) || role.equals("administrador"));

        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + KEYCLOAK_ADMIN_ROLE));

            try {
                catalogCacheService.getPermissionsForAllRoles().forEach(permission ->
                        authorities.add(new SimpleGrantedAuthority("PERM_" + permission.toUpperCase(Locale.ROOT))));
            } catch (RuntimeException ex) {
                logger.warn("No se pudieron resolver permisos del catalogo de seguridad para ADMIN. Se continuo con rol ADMIN del JWT: {}", ex.getMessage());
            }
        } else {
            logger.debug("Usuario USUARIO detectado en JWT. Los permisos se resuelven desde la BD interna.");
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
