package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeguridadPermisoRepository extends JpaRepository<SeguridadPermiso, Long> {
    Optional<SeguridadPermiso> findByCodigoIgnoreCase(String codigo);
    List<SeguridadPermiso> findAllByActivoTrueOrderByCodigoAsc();
}
