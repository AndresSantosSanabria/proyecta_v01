package com.proyecta.api_gestion.domain.value;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina devuelta por los puertos (ADR-007 dec.3).
 * Reemplaza a org.springframework.data.domain.Page fuera de la aplicacion;
 * el adaptador web la traduce a PageImpl para conservar el JSON del contrato HTTP.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements,
                            List<SortOrder> sorts) {

    public PageResult {
        content = content == null ? List.of() : content;
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public <U> PageResult<U> map(Function<? super T, ? extends U> mapper) {
        List<U> mapped = content.stream().<U>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements, sorts);
    }
}
