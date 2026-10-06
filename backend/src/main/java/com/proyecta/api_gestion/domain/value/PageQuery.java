package com.proyecta.api_gestion.domain.value;

import java.util.List;

/**
 * Pagina solicitada en los puertos (ADR-007 dec.3).
 * Reemplaza a org.springframework.data.domain.Pageable fuera de los adaptadores.
 */
public record PageQuery(int page, int size, List<SortOrder> sorts) {

    public PageQuery {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 10;
        }
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, List.of());
    }
}
