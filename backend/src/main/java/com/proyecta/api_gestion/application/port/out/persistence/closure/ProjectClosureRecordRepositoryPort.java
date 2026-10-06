package com.proyecta.api_gestion.application.port.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ProjectClosureRecord;
import java.util.Optional;

public interface ProjectClosureRecordRepositoryPort {

    Optional<ProjectClosureRecord> findByProyectoId(String proyectoId);

    <E extends ProjectClosureRecord> E save(E entity);
}

