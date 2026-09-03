package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.EntregableCambioDescripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregableCambioDescripcionRepository extends JpaRepository<EntregableCambioDescripcion, Long> {
    List<EntregableCambioDescripcion> findByEntregableIdOrderByCreadoEnDesc(Integer entregableId);
    boolean existsByEntregableId(Integer entregableId);
}
