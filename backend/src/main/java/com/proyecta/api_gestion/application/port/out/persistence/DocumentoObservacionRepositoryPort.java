package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoObservacion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoObservacionEstado;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentoObservacionRepositoryPort {

    List<DocumentoObservacion> findByEntregableIdOrderByCreadaEnDesc(Integer entregableId);

    List<DocumentoObservacion> findByEntregableIdAndEstadoIn(Integer entregableId, Collection<DocumentoObservacionEstado> estados);

    Optional<DocumentoObservacion> findByIdAndEntregableId(Long id, Integer entregableId);

    <E extends DocumentoObservacion> E save(E entity);
}

