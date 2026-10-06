package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Fase;
import java.util.List;

public interface FaseRepositoryPort {

    List<Fase> findByProyectoId(String proyectoId);

    <E extends Fase> E save(E entity);

    java.util.Optional<Fase> findById(Integer id);
}

