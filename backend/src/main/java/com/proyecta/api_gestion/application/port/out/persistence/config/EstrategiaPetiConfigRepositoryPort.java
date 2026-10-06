package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstrategiaPetiConfig;
import java.util.List;
import java.util.Optional;

public interface EstrategiaPetiConfigRepositoryPort {

    Optional<EstrategiaPetiConfig> findByCodigo(String codigo);

    List<EstrategiaPetiConfig> findByActivoTrueOrderByOrdenAsc();

    <E extends EstrategiaPetiConfig> E save(E entity);

    long count();
}

