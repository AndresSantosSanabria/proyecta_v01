package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstadoEntregableConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.EstadoEntregableConfigRepositoryPort;
@Repository
public interface EstadoEntregableConfigRepository extends JpaRepository<EstadoEntregableConfig, Long>, EstadoEntregableConfigRepositoryPort {

    Optional<EstadoEntregableConfig> findByCodigo(String codigo);
}

