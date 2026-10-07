package com.proyecta.api_gestion.domain.value;

import java.util.Set;

/**
 * Usuario autenticado que consumen los casos de uso (ADR-007 dec.4).
 * Reemplaza a org.springframework.security.core.Authentication fuera del adaptador web.
 */
public record UserContext(String username, Set<String> roles) {

    public static UserContext anonymous() {
        return new UserContext(null, Set.of());
    }
}
