package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.EntregableCambioFecha;
import java.util.List;

public interface EntregableCambioFechaRepositoryPort {

    List<EntregableCambioFecha> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);

    boolean existsByEntregableId(Integer entregableId);

    <E extends EntregableCambioFecha> E save(E entity);

    java.util.Optional<EntregableCambioFecha> findById(Long id);
}

