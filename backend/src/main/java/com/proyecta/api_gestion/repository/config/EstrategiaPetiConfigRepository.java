package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.EstrategiaPetiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstrategiaPetiConfigRepository extends JpaRepository<EstrategiaPetiConfig, Long> {
    Optional<EstrategiaPetiConfig> findByCodigo(String codigo);
    List<EstrategiaPetiConfig> findByActivoTrueOrderByOrdenAsc();
}
