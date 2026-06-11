package com.proyecta.api_gestion.dto.config;

import java.util.List;

public record PetiCatalogDTO(
        List<String> vigencias,
        List<CatalogOptionDTO> estrategias
) {
}
