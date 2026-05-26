package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeguridadRolRepository extends JpaRepository<SeguridadRol, Long> {
    Optional<SeguridadRol> findByCodigoIgnoreCase(String codigo);
    List<SeguridadRol> findAllByActivoTrueOrderByCodigoAsc();
}
