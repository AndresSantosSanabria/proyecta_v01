package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.TipoDocumentoConfig;
import java.util.List;
import java.util.Optional;

public interface TipoDocumentoConfigRepositoryPort {

    Optional<TipoDocumentoConfig> findByCodigo(String codigo);

    List<TipoDocumentoConfig> findByActivoTrueOrderByOrdenAsc();
}

