package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstadoProyectoConfig;
import java.util.Optional;

public interface EstadoProyectoConfigRepositoryPort {

    Optional<EstadoProyectoConfig> findByCodigo(String codigo);
}

