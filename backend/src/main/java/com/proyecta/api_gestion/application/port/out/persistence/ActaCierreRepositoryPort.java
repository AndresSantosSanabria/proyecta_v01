package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.ActaCierre;
import java.util.Optional;

public interface ActaCierreRepositoryPort {

    Optional<ActaCierre> findByProyectoId(String proyectoId);

    <E extends ActaCierre> E save(E entity);

    void delete(ActaCierre entity);
}

