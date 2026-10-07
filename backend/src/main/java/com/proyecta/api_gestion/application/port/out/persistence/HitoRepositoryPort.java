package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Hito;
import java.util.List;

public interface HitoRepositoryPort {

    List<Hito> findByFaseId(Integer faseId);

    List<Hito> findByProyectoId(String proyectoId);

    <E extends Hito> E save(E entity);

    java.util.Optional<Hito> findById(Integer id);
}

