package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.RolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolConfigRepository extends JpaRepository<RolConfig, Long> {
    Optional<RolConfig> findByCodigo(String codigo);
    List<RolConfig> findByActivoTrueOrderByNivelAccesoDesc();
    List<RolConfig> findByNivelAccesoGreaterThanEqualOrderByNivelAccesoDesc(Integer nivelMinimo);
}
