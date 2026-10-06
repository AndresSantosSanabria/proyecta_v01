package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoObservacion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoObservacionEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.DocumentoObservacionRepositoryPort;
public interface DocumentoObservacionRepository extends JpaRepository<DocumentoObservacion, Long>, DocumentoObservacionRepositoryPort {
    List<DocumentoObservacion> findByEntregableIdOrderByCreadaEnDesc(Integer entregableId);

    List<DocumentoObservacion> findByEntregableIdAndEstadoIn(Integer entregableId, Collection<DocumentoObservacionEstado> estados);

    Optional<DocumentoObservacion> findByIdAndEntregableId(Long id, Integer entregableId);
}

