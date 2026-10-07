package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadPermiso;
import java.util.List;
import java.util.Optional;

public interface SeguridadPermisoRepositoryPort {

    Optional<SeguridadPermiso> findByCodigoIgnoreCase(String codigo);

    List<SeguridadPermiso> findAllByActivoTrueOrderByCodigoAsc();

    <E extends SeguridadPermiso> E save(E entity);

    java.util.Optional<SeguridadPermiso> findById(Long id);
}

