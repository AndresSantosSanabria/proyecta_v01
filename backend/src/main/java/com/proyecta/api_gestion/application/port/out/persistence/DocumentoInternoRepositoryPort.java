package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoInterno;
import java.util.List;
import java.util.Optional;
import com.proyecta.api_gestion.domain.value.PageQuery;

public interface DocumentoInternoRepositoryPort {

    boolean existsByCodigo(String codigo);

    Optional<DocumentoInterno> findByCodigo(String codigo);

    List<DocumentoInterno> buscarConFiltros(String nombrePattern, Integer anio, String descripcionPattern, PageQuery pageable);

    long contarConFiltros(String nombrePattern, Integer anio, String descripcionPattern);

    java.util.Optional<DocumentoInterno> findById(Long id);

    <E extends DocumentoInterno> E saveAndFlush(E entity);
}

