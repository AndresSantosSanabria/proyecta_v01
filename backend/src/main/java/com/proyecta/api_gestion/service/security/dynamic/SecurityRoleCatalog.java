package com.proyecta.api_gestion.service.security.dynamic;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;

public final class SecurityRoleCatalog {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_GESTOR_TIC = "gestor_tic";
    private static final String ROLE_GESTOR_PROYECTOS = "gestor_proyectos";
    private static final String ROLE_DIRECTOR_PROYECTO = "director_proyecto";
    private static final String ROLE_AUDITOR = "auditor";
    private static final String ROLE_CONSULTA = "consulta";
    private static final String ROLE_VISUALIZADOR = "visualizador";

    public static final Set<String> PROTECTED_ROLE_CODES = Set.of(
            ROLE_ADMIN,
            "usuario",
            ROLE_GESTOR_TIC,
            ROLE_GESTOR_PROYECTOS,
            ROLE_DIRECTOR_PROYECTO,
            ROLE_AUDITOR,
            ROLE_CONSULTA,
            ROLE_VISUALIZADOR
    );

    public static final Set<String> TRANSVERSAL_ROLE_CODES = Set.of(ROLE_ADMIN, ROLE_GESTOR_TIC, ROLE_GESTOR_PROYECTOS);

    private static final Map<String, String> ROLE_ALIASES = Map.ofEntries(
            entry("administrador", ROLE_ADMIN),
            entry(ROLE_ADMIN, ROLE_ADMIN),
            entry("gestor_proyectos_ti", ROLE_GESTOR_TIC),
            entry(ROLE_GESTOR_TIC, ROLE_GESTOR_TIC),
            entry("gestor_pro", ROLE_GESTOR_PROYECTOS),
            entry("gestor_proyecto", ROLE_GESTOR_PROYECTOS),
            entry("gestor_de_proyectos", ROLE_GESTOR_PROYECTOS),
            entry(ROLE_GESTOR_PROYECTOS, ROLE_GESTOR_PROYECTOS),
            entry("director_pro", ROLE_DIRECTOR_PROYECTO),
            entry("director_de_proyecto", ROLE_DIRECTOR_PROYECTO),
            entry("director_proyectos", ROLE_DIRECTOR_PROYECTO),
            entry(ROLE_DIRECTOR_PROYECTO, ROLE_DIRECTOR_PROYECTO),
            entry("analista_proyectos", ROLE_CONSULTA),
            entry("analista", ROLE_CONSULTA),
            entry(ROLE_AUDITOR, ROLE_AUDITOR),
            entry(ROLE_CONSULTA, ROLE_CONSULTA)
    );

    private SecurityRoleCatalog() {}

    public static String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return ROLE_VISUALIZADOR;
        }

        String lower = sanitize(value);

        if (lower.contains(ROLE_ADMIN)) {
            return ROLE_ADMIN;
        }
        
        String mapped = ROLE_ALIASES.getOrDefault(lower, lower);
        if (PROTECTED_ROLE_CODES.contains(mapped)) {
            return mapped;
        }
        
        return ROLE_VISUALIZADOR;
    }

    public static boolean isProtected(String code) {
        if (code == null || code.trim().isBlank()) {
            return true;
        }
        String lower = sanitize(code);
        if (lower.contains(ROLE_ADMIN)) {
            return true;
        }
        String mapped = ROLE_ALIASES.getOrDefault(lower, lower);
        return PROTECTED_ROLE_CODES.contains(mapped);
    }

    private static String sanitize(String value) {
        String lower = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceFirst("^role[\\s_-]+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "");
        while (lower.endsWith("_")) {
            lower = lower.substring(0, lower.length() - 1);
        }
        return lower;
    }

    public static boolean isTransversal(String code) {
        String normalized = normalize(code);
        return normalized != null && TRANSVERSAL_ROLE_CODES.contains(normalized);
    }
}
