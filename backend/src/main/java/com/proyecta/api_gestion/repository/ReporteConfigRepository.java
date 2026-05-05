package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.ReporteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteConfigRepository extends JpaRepository<ReporteConfig, String> {
    List<ReporteConfig> findAllByActivoTrueOrderByOrdenAsc();
}
