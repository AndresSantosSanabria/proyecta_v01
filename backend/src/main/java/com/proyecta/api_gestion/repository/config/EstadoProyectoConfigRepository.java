package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.EstadoProyectoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoProyectoConfigRepository extends JpaRepository<EstadoProyectoConfig, Long> {
    Optional<EstadoProyectoConfig> findByCodigo(String codigo);
    List<EstadoProyectoConfig> findByActivoTrueOrderByOrdenAsc();
    List<EstadoProyectoConfig> findByEsTerminalFalseOrderByOrdenAsc();
}
