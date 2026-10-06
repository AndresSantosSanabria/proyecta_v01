package com.proyecta.api_gestion.domain.value;

/**
 * Orden de una columna dentro de una paginacion (ADR-007 dec.3).
 * Reemplaza a org.springframework.data.domain.Sort.Order fuera de los adaptadores.
 */
public record SortOrder(String property, boolean ascending) {
}
