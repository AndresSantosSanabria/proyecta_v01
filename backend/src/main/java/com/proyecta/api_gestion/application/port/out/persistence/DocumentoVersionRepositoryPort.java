package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoVersion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoVersionEstado;
import java.util.List;
import java.util.Optional;

public interface DocumentoVersionRepositoryPort {

    List<DocumentoVersion> findByEntregableIdOrderByNumeroVersionDesc(Integer entregableId);

    List<DocumentoVersion> findByEntregableIdAndEstado(Integer entregableId, DocumentoVersionEstado estado);

    Optional<DocumentoVersion> findFirstByEntregableIdAndEstadoOrderByNumeroVersionDesc(Integer entregableId, DocumentoVersionEstado estado);

    Optional<DocumentoVersion> findByIdAndEntregableId(Long id, Integer entregableId);

    int findMaxNumeroVersionByEntregableId(Integer entregableId);

    <E extends DocumentoVersion> E save(E entity);
}

