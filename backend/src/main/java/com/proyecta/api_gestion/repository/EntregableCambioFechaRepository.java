package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.EntregableCambioFecha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregableCambioFechaRepository extends JpaRepository<EntregableCambioFecha, Long> {
    List<EntregableCambioFecha> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);
    boolean existsByEntregableId(Integer entregableId);
}
