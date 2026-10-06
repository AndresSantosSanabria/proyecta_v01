package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoTratamientoAdjunto;

public interface RiesgoTratamientoAdjuntoRepositoryPort {

    <E extends RiesgoTratamientoAdjunto> E save(E entity);

    java.util.Optional<RiesgoTratamientoAdjunto> findById(Long id);
}

