package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.RiesgoTratamientoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiesgoTratamientoAdjuntoRepository extends JpaRepository<RiesgoTratamientoAdjunto, Long> {
    List<RiesgoTratamientoAdjunto> findByTratamiento_IdOrderByFechaCargaAsc(Long tratamientoId);
}
