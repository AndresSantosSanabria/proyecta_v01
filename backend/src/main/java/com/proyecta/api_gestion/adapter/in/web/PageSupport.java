package com.proyecta.api_gestion.adapter.in.web;

import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.domain.value.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduccion PageResult <-> Page en la frontera HTTP
 * (ADR-007 dec.3-4): el contrato JSON conserva la forma de Page de Spring.
 */
public final class PageSupport {

    private PageSupport() {
    }

    /**
     * Construye el PageQuery desde parametros query crudos de HTTP (Fase 4):
     * replicamos el binding de PageableHandlerMethodArgumentResolver sin exponer
     * org.springframework.data en los controladores. limitMax pageSize = 2000
     * (DEFAULT_MAX_PAGE_SIZE de Spring Data).
     */
    public static PageQuery fromParams(int page, int size, List<String> sort, int defaultPageSize) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size < 1 ? defaultPageSize : Math.min(size, 2000);
        List<SortOrder> orders = new ArrayList<>();
        if (sort != null) {
            for (String raw : sort) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String[] parts = raw.split(",", 2);
                String property = parts[0].trim();
                if (!property.isEmpty()) {
                    boolean ascending = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());
                    orders.add(new SortOrder(property, ascending));
                }
            }
        }
        return new PageQuery(pageNumber, pageSize, orders);
    }

    public static <T> Page<T> toPage(PageResult<T> result) {
        if (result.sorts().isEmpty()) {
            return new PageImpl<>(result.content(), PageRequest.of(result.page(), result.size()),
                    result.totalElements());
        }
        Sort sort = Sort.by(result.sorts().stream()
                .map(o -> o.ascending() ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                .toList());
        return new PageImpl<>(result.content(), PageRequest.of(result.page(), result.size(), sort),
                result.totalElements());
    }
}
