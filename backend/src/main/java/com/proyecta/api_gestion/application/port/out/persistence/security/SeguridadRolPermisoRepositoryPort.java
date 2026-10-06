package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadRolPermiso;
import java.util.Collection;
import java.util.List;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface SeguridadRolPermisoRepositoryPort {

    List<SeguridadRolPermiso> findActiveByRoleCodes(Collection<String> roleCodes);

    List<SeguridadRolPermiso> findAllActiveWithRelations();

    <E extends SeguridadRolPermiso> E save(E entity);

    <E extends SeguridadRolPermiso> java.util.List<E> saveAll(Iterable<E> entities);

    java.util.List<SeguridadRolPermiso> findAll();

    void deleteAllInBatch(Iterable<SeguridadRolPermiso> entities);

    void deleteAllInBatch();

    PageResult<SeguridadRolPermiso> findAll(PageQuery query);
}
