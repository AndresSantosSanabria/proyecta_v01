package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstadoEntregableConfig;
import java.util.Optional;

public interface EstadoEntregableConfigRepositoryPort {

    Optional<EstadoEntregableConfig> findByCodigo(String codigo);
}

