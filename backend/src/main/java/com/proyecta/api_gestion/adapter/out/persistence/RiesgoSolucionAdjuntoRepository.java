package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoSolucionAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoSolucionAdjuntoRepositoryPort;
public interface RiesgoSolucionAdjuntoRepository extends JpaRepository<RiesgoSolucionAdjunto, Long>, RiesgoSolucionAdjuntoRepositoryPort {
    List<RiesgoSolucionAdjunto> findByRiesgoIdOrderByFechaCargaAsc(Integer riesgoId);
}

