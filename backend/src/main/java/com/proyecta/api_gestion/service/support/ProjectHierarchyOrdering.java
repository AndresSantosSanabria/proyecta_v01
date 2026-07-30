package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;

import java.util.Comparator;

public final class ProjectHierarchyOrdering {

    private ProjectHierarchyOrdering() {
    }

    public static final Comparator<Fase> FASES_BY_ORDEN = Comparator
            .comparing(Fase::getId, Comparator.nullsLast(Integer::compareTo));

    public static final Comparator<Hito> HITOS_BY_ORDEN = Comparator
            .comparing(Hito::getId, Comparator.nullsLast(Integer::compareTo));

    public static final Comparator<Entregable> ENTREGABLES_BY_ORDEN = Comparator
            .comparing(Entregable::getId, Comparator.nullsLast(Integer::compareTo));
}
