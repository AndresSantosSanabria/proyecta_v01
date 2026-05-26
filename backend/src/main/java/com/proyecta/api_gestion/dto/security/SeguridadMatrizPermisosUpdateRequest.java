package com.proyecta.api_gestion.dto.security;

import java.util.List;
import java.util.Map;

public record SeguridadMatrizPermisosUpdateRequest(
        Map<String, List<String>> matriz
) {}
