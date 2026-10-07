package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.EntregableCambioFecha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.EntregableCambioFechaRepositoryPort;
public interface EntregableCambioFechaRepository extends JpaRepository<EntregableCambioFecha, Long>, EntregableCambioFechaRepositoryPort {
    List<EntregableCambioFecha> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);
    boolean existsByEntregableId(Integer entregableId);
}

