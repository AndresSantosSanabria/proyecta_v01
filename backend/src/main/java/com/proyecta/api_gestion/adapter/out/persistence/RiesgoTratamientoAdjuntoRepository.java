package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoTratamientoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoTratamientoAdjuntoRepositoryPort;
public interface RiesgoTratamientoAdjuntoRepository extends JpaRepository<RiesgoTratamientoAdjunto, Long>, RiesgoTratamientoAdjuntoRepositoryPort {
}

