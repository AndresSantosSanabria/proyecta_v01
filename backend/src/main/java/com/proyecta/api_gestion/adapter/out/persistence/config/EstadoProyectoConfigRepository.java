package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstadoProyectoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.EstadoProyectoConfigRepositoryPort;
@Repository
public interface EstadoProyectoConfigRepository extends JpaRepository<EstadoProyectoConfig, Long>, EstadoProyectoConfigRepositoryPort {

    Optional<EstadoProyectoConfig> findByCodigo(String codigo);
}

