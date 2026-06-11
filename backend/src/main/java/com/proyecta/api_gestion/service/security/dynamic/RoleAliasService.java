package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RoleAliasService {

    private static final String DEFAULT_ALIASES = "ADMINISTRADOR:ADMIN,DIRECTOR_PRO:DIRECTOR_PROYECTO,"
            + "GESTOR_PROYECTOS_TI:GESTOR_TIC,ANALISTA_PROYECTOS:CONSULTA";

    private static final Set<String> CANONICAL_ROLE_CODES = Set.of(
            "admin",
            "director_proyecto",
            "gestor_proyectos",
            "gestor_tic",
            "auditor",
            "consulta"
    );

    private final SystemParameterService systemParameterService;

    public RoleAliasService(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    public String normalize(String value) {
        String cleaned = normalizeKey(value);
        if (cleaned == null) {
            return null;
        }
        if (CANONICAL_ROLE_CODES.contains(cleaned)) {
            return cleaned;
        }
        return roleAliasesLowercase().getOrDefault(cleaned, SecurityRoleCatalog.normalize(cleaned));
    }

    public Map<String, String> roleAliasesForClient() {
        Map<String, String> aliases = new LinkedHashMap<>();
        roleAliasesLowercase().forEach((source, target) ->
                aliases.put(source.toUpperCase(Locale.ROOT), target.toUpperCase(Locale.ROOT)));
        return aliases;
    }

    private Map<String, String> roleAliasesLowercase() {
        String raw = systemParameterService.getString(SystemParameterKeys.SEGURIDAD_ROLE_ALIASES, DEFAULT_ALIASES);
        Map<String, String> aliases = new LinkedHashMap<>();
        for (String pair : raw.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                continue;
            }
            String source = normalizeKey(parts[0]);
            String target = normalizeKey(parts[1]);
            if (source == null || target == null || CANONICAL_ROLE_CODES.contains(source)) {
                continue;
            }
            aliases.put(source, CANONICAL_ROLE_CODES.contains(target) ? target : SecurityRoleCatalog.normalize(target));
        }
        return aliases;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^role[\\s_-]+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isBlank() ? null : cleaned;
    }
}
