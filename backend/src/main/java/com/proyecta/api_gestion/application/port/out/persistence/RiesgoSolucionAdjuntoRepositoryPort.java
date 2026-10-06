package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoSolucionAdjunto;
import java.util.List;

public interface RiesgoSolucionAdjuntoRepositoryPort {

    List<RiesgoSolucionAdjunto> findByRiesgoIdOrderByFechaCargaAsc(Integer riesgoId);

    <E extends RiesgoSolucionAdjunto> E save(E entity);

    java.util.Optional<RiesgoSolucionAdjunto> findById(Long id);
}

