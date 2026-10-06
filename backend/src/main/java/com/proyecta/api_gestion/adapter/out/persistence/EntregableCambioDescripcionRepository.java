package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.EntregableCambioDescripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.EntregableCambioDescripcionRepositoryPort;
public interface EntregableCambioDescripcionRepository extends JpaRepository<EntregableCambioDescripcion, Long>, EntregableCambioDescripcionRepositoryPort {
    List<EntregableCambioDescripcion> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);
    boolean existsByEntregableId(Integer entregableId);
}