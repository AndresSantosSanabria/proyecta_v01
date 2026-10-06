package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadRol;
import java.util.List;
import java.util.Optional;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface SeguridadRolRepositoryPort {

    java.util.List<SeguridadRol> findAll(java.util.List<com.proyecta.api_gestion.domain.value.SortOrder> sorts);


    Optional<SeguridadRol> findByCodigoIgnoreCase(String codigo);

    List<SeguridadRol> findAllByActivoTrueOrderByCodigoAsc();

    <E extends SeguridadRol> E save(E entity);

    java.util.List<SeguridadRol> findAll();

    PageResult<SeguridadRol> findAll(PageQuery query);
}

