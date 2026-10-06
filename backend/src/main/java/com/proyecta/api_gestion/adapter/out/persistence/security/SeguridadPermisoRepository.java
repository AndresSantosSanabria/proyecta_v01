package com.proyecta.api_gestion.adapter.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadPermisoRepositoryPort;
public interface SeguridadPermisoRepository extends JpaRepository<SeguridadPermiso, Long>, SeguridadPermisoRepositoryPort {
    Optional<SeguridadPermiso> findByCodigoIgnoreCase(String codigo);
    List<SeguridadPermiso> findAllByActivoTrueOrderByCodigoAsc();
}

