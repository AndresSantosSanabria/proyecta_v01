package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.EstrategiaPetiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.EstrategiaPetiConfigRepositoryPort;
@Repository
public interface EstrategiaPetiConfigRepository extends JpaRepository<EstrategiaPetiConfig, Long>, EstrategiaPetiConfigRepositoryPort {
    Optional<EstrategiaPetiConfig> findByCodigo(String codigo);
    List<EstrategiaPetiConfig> findByActivoTrueOrderByOrdenAsc();
}

