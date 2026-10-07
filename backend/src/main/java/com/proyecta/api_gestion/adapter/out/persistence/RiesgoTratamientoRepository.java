package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.RiesgoTratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoTratamientoRepositoryPort;
public interface RiesgoTratamientoRepository extends JpaRepository<RiesgoTratamiento, Long>, RiesgoTratamientoRepositoryPort {
    List<RiesgoTratamiento> findByRiesgoIdOrderByIteracionDesc(Integer riesgoId);
    long countByRiesgoId(Integer riesgoId);
}

