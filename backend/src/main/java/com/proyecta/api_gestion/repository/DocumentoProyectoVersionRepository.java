package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.DocumentoProyectoVersion;
import com.proyecta.api_gestion.model.enums.DocumentoProyectoVersionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoProyectoVersionRepository extends JpaRepository<DocumentoProyectoVersion, Long> {

    List<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoOrderByNumeroVersionDesc(
            String proyectoId, String tipoDocumento);

    Optional<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoAndEstado(
            String proyectoId, String tipoDocumento, DocumentoProyectoVersionEstado estado);

    List<DocumentoProyectoVersion> findByProyectoIdAndEstado(
            String proyectoId, DocumentoProyectoVersionEstado estado);

    Optional<DocumentoProyectoVersion> findByProyectoIdAndTipoDocumentoAndNumeroVersion(
            String proyectoId, String tipoDocumento, Integer numeroVersion);

    @Query("SELECT COALESCE(MAX(d.numeroVersion), 0) FROM DocumentoProyectoVersion d WHERE d.proyectoId = :proyectoId AND d.tipoDocumento = :tipoDocumento")
    Integer findMaxNumeroVersion(@Param("proyectoId") String proyectoId, @Param("tipoDocumento") String tipoDocumento);

    List<DocumentoProyectoVersion> findByProyectoIdOrderBySubidoEnDesc(String proyectoId);
}
