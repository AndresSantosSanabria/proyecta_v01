package com.proyecta.api_gestion.service.security.dynamic;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;

public final class SecurityRoleCatalog {

    public static final Set<String> PROTECTED_ROLE_CODES = Set.of(
            "admin",
            "usuario",
            "gestor_tic",
            "gestor_proyectos",
            "director_proyecto",
            "auditor",
            "consulta",
            "visualizador"
    );

    public static final Set<String> TRANSVERSAL_ROLE_CODES = Set.of("admin", "gestor_tic", "gestor_proyectos");

    private static final Map<String, String> ROLE_ALIASES = Map.ofEntries(
            entry("administrador", "admin"),
            entry("admin", "admin"),
            entry("gestor_proyectos_ti", "gestor_tic"),
            entry("gestor_tic", "gestor_tic"),
            entry("gestor_pro", "gestor_proyectos"),
            entry("gestor_proyecto", "gestor_proyectos"),
            entry("gestor_de_proyectos", "gestor_proyectos"),
            entry("gestor_proyectos", "gestor_proyectos"),
            entry("director_pro", "director_proyecto"),
            entry("director_de_proyecto", "director_proyecto"),
            entry("director_proyectos", "director_proyecto"),
            entry("director_proyecto", "director_proyecto"),
            entry("analista_proyectos", "consulta"),
            entry("analista", "consulta"),
            entry("auditor", "auditor"),
            entry("consulta", "consulta")
    );

    private SecurityRoleCatalog() {}

    public static String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return "visualizador";
        }

        String cleaned = value.trim();
        String lower = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceFirst("^role[\\s_-]+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (lower.contains("admin")) {
            return "admin";
        }
        
        String mapped = ROLE_ALIASES.getOrDefault(lower, lower);
        if (PROTECTED_ROLE_CODES.contains(mapped)) {
            return mapped;
        }
        
        return "visualizador";
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
