package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.TipoDocumentoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.TipoDocumentoConfigRepositoryPort;
@Repository
public interface TipoDocumentoConfigRepository extends JpaRepository<TipoDocumentoConfig, Long>, TipoDocumentoConfigRepositoryPort {
    Optional<TipoDocumentoConfig> findByCodigo(String codigo);
    List<TipoDocumentoConfig> findByActivoTrueOrderByOrdenAsc();
}

