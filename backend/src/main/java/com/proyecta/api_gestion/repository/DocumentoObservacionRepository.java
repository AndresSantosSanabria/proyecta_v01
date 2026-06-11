package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.DocumentoObservacion;
import com.proyecta.api_gestion.model.enums.DocumentoObservacionEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentoObservacionRepository extends JpaRepository<DocumentoObservacion, Long> {
    List<DocumentoObservacion> findByEntregableIdOrderByCreadaEnDesc(Integer entregableId);

    List<DocumentoObservacion> findByEntregableIdAndEstadoIn(Integer entregableId, Collection<DocumentoObservacionEstado> estados);

    Optional<DocumentoObservacion> findByIdAndEntregableId(Long id, Integer entregableId);
}
