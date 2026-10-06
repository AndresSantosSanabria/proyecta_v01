package com.proyecta.api_gestion.application.port.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureTemplate;
import java.util.Optional;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface ClosureTemplateRepositoryPort {

    Optional<ClosureTemplate> findByActivoTrue();

    void deactivateAll();

    <E extends ClosureTemplate> E save(E entity);

    java.util.List<ClosureTemplate> findAll();

    PageResult<ClosureTemplate> findAll(PageQuery query);
}
