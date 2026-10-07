package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoVersion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoVersionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.DocumentoVersionRepositoryPort;
public interface DocumentoVersionRepository extends JpaRepository<DocumentoVersion, Long>, DocumentoVersionRepositoryPort {
    List<DocumentoVersion> findByEntregableIdOrderByNumeroVersionDesc(Integer entregableId);

    List<DocumentoVersion> findByEntregableIdAndEstado(Integer entregableId, DocumentoVersionEstado estado);

    Optional<DocumentoVersion> findFirstByEntregableIdAndEstadoOrderByNumeroVersionDesc(Integer entregableId, DocumentoVersionEstado estado);

    Optional<DocumentoVersion> findByIdAndEntregableId(Long id, Integer entregableId);

    @Query("SELECT COALESCE(MAX(v.numeroVersion), 0) FROM DocumentoVersion v WHERE v.entregable.id = :entregableId")
    int findMaxNumeroVersionByEntregableId(@Param("entregableId") Integer entregableId);
}

