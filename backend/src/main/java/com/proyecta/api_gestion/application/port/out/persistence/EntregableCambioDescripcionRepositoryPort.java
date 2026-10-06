package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.EntregableCambioDescripcion;
import java.util.List;

public interface EntregableCambioDescripcionRepositoryPort {

    List<EntregableCambioDescripcion> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);

    boolean existsByEntregableId(Integer entregableId);

    <E extends EntregableCambioDescripcion> E save(E entity);

    java.util.Optional<EntregableCambioDescripcion> findById(Long id);
}

