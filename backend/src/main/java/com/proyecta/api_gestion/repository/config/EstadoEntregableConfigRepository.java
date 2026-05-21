package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.EstadoEntregableConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoEntregableConfigRepository extends JpaRepository<EstadoEntregableConfig, Long> {
    Optional<EstadoEntregableConfig> findByCodigo(String codigo);
    List<EstadoEntregableConfig> findByActivoTrueOrderByOrdenAsc();
    List<EstadoEntregableConfig> findByEsConformeTrueOrderByOrdenAsc();
}
