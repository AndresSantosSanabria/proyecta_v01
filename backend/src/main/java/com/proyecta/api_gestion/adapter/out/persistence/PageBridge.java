package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.domain.value.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Traduccion PageQuery/PageResult <-> Pageable/Page dentro del adaptador de
 * persistencia (ADR-007 dec.3). Solo lo usan los repositorios JPA.
 */
public final class PageBridge {

    private PageBridge() {
    }

    public static Pageable toPageable(PageQuery query) {
        if (query.sorts().isEmpty()) {
            return PageRequest.of(query.page(), query.size());
        }
        Sort sort = Sort.by(query.sorts().stream()
                .map(o -> o.ascending() ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                .toList());
        return PageRequest.of(query.page(), query.size(), sort);
    }

    public static <T> PageResult<T> toResult(Page<T> page, PageQuery query) {
        return new PageResult<>(page.getContent(), query.page(), query.size(),
                page.getTotalElements(), query.sorts());
    }

    public static Sort toSort(java.util.List<SortOrder> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.unsorted();
        }
        return Sort.by(sorts.stream()
                .map(o -> o.ascending() ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                .toList());
    }
}
