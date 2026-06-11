package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Hito;

import java.time.LocalDate;
import java.util.Comparator;

public final class ProjectHierarchyOrdering {

    private ProjectHierarchyOrdering() {
    }

    public static final Comparator<Entregable> ENTREGABLES_BY_SCHEDULE = Comparator
            .comparing(Entregable::getFechaInicio, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(Entregable::getFechaLimite, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(Entregable::getId, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(Entregable::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    public static final Comparator<Hito> HITOS_BY_SEQUENCE = Comparator
            .comparing(Hito::getId, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(Hito::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
}
