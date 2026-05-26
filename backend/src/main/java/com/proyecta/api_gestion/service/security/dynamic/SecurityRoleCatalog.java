package com.proyecta.api_gestion.service.security.dynamic;

import java.util.Map;
import java.util.Set;

public final class SecurityRoleCatalog {

    public static final Set<String> PROTECTED_ROLE_CODES = Set.of(
            "admin",
            "gestor_tic",
            "director_proyecto",
            "auditor",
            "consulta"
    );

    public static final Set<String> TRANSVERSAL_ROLE_CODES = Set.of("admin", "gestor_tic");

    private static final Map<String, String> ROLE_ALIASES = Map.of(
            "administrador", "admin",
            "admin", "admin",
            "gestor_proyectos_ti", "gestor_tic",
            "gestor_tic", "gestor_tic",
            "gestor_proyectos", "director_proyecto",
            "director_proyecto", "director_proyecto",
            "analista_proyectos", "consulta",
            "analista", "consulta",
            "auditor", "auditor",
            "consulta", "consulta"
    );

    private SecurityRoleCatalog() {}

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        String lower = trimmed.toLowerCase();
        if (lower.startsWith("role_")) {
            lower = lower.substring(5);
        }
        return ROLE_ALIASES.getOrDefault(lower, lower);
    }

    public static boolean isProtected(String code) {
        String normalized = normalize(code);
        return normalized != null && PROTECTED_ROLE_CODES.contains(normalized);
    }

    public static boolean isTransversal(String code) {
        String normalized = normalize(code);
        return normalized != null && TRANSVERSAL_ROLE_CODES.contains(normalized);
    }
}
