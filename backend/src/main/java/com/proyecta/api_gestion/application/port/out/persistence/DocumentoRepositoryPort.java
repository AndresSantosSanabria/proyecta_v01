package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Documento;
import java.util.List;
import java.util.Optional;

public interface DocumentoRepositoryPort {

    List<Documento> findByProyectoIdOrderByFechaCargaDesc(String proyectoId);

    Optional<Documento> findByProyectoIdAndTipoDocumentoConfigCodigo(String proyectoId, String codigo);

    <E extends Documento> E save(E entity);
}

