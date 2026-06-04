package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.RiesgoSolucionAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiesgoSolucionAdjuntoRepository extends JpaRepository<RiesgoSolucionAdjunto, Long> {
    List<RiesgoSolucionAdjunto> findByRiesgo_IdOrderByFechaCargaAsc(Integer riesgoId);
}
