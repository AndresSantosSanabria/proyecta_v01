package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoTratamiento;
import java.util.List;

public interface RiesgoTratamientoRepositoryPort {

    List<RiesgoTratamiento> findByRiesgoIdOrderByIteracionDesc(Integer riesgoId);

    long countByRiesgoId(Integer riesgoId);

    <E extends RiesgoTratamiento> E save(E entity);
}

