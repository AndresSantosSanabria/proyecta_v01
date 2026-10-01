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

    private final List<String> resourceClientIds;
    /**
     * CWE-266/CWE-287: los claim "scope"/"scp" llegan del token y no son autoridad
     * de la aplicacion. Solo se convierten en authorities si el scope esta en esta
     * allowlist explicita (por defecto vacia: no se confia en ningun scope).
     */
    private final Set<String> trustedScopes;
    private final SecurityCatalogCacheService catalogCacheService;
    private final KeycloakIdentityExtractor identityExtractor;

    public DynamicJwtAuthoritiesConverter(
            @Value("${gob.security.resource-client-ids}") String resourceClientIds,
            @Value("${gob.security.trusted-scopes:}") String trustedScopes,
            SecurityCatalogCacheService catalogCacheService,
            KeycloakIdentityExtractor identityExtractor,
            RoleAliasService roleAliasService) {
        this.resourceClientIds = List.of(resourceClientIds.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.trustedScopes = List.of(trustedScopes.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.catalogCacheService = catalogCacheService;
        this.identityExtractor = identityExtractor;
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
                // El rol ADMIN del JWT se conserva, pero sin permisos derivados del
                // catalogo (deny-by-default): no se sustituyen por una lista completa.
                logger.warn("No se pudieron resolver permisos del catalogo de seguridad para ADMIN; "
                        + "se mantiene el rol ADMIN del JWT sin permisos del catalogo. causa={}", ex.toString());
            }
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_usuario"));
            logger.debug("Usuario detectado en JWT. Los permisos funcionales se resuelven desde la BD interna.");
        }

        addScopes(jwt.getClaimAsString("scope"), authorities);
        addScopes(jwt.getClaimAsStringList("scp"), authorities);

        return authorities;
    }

    private void addScopes(String scopes, Set<GrantedAuthority> authorities) {
        if (scopes == null || scopes.isBlank() || trustedScopes.isEmpty()) {
            return;
        }

        for (String scope : scopes.split(" ")) {
            if (!scope.isBlank()) {
                addTrustedScope(scope.trim(), authorities);
            }
        }
    }

    private void addScopes(Collection<String> scopes, Set<GrantedAuthority> authorities) {
        if (scopes == null || trustedScopes.isEmpty()) {
            return;
        }

        scopes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(value -> addTrustedScope(value, authorities));
    }

    private void addTrustedScope(String scope, Set<GrantedAuthority> authorities) {
        if (trustedScopes.contains(scope)) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        } else if (logger.isDebugEnabled()) {
            logger.debug("Scope descartado por no estar en la allowlist: {}", scope);
        }
    }

    /**
     * CWE-522: el token JWT nunca se expone como "credentials" de la autenticacion
     * (evita que aparezca en logs, trazas o eventos de seguridad).
     */
    private org.springframework.security.core.Authentication authenticationFrom(Jwt jwt) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(jwt, null, List.of());
    }
}
