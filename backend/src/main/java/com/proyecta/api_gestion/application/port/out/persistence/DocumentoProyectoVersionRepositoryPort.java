package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoProyectoVersion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoProyectoVersionEstado;
import java.util.List;
import java.util.Optional;

public interface DocumentoProyectoVersionRepositoryPort {

    List<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(String proyectoId, String tipoDocumento);

    Optional<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoAndEstado(String proyectoId, String tipoDocumento, DocumentoProyectoVersionEstado estado);

    List<DocumentoProyectoVersion> findByProyectoIdAndEstado(String proyectoId, DocumentoProyectoVersionEstado estado);

    Optional<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoAndNumeroVersion(String proyectoId, String tipoDocumento, Integer numeroVersion);

    Integer findMaxNumeroVersion(String proyectoId, String tipoDocumento);

    List<DocumentoProyectoVersion> findByProyectoIdOrderBySubidoEnDesc(String proyectoId);

    <E extends DocumentoProyectoVersion> E save(E entity);
}

