package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.RiesgoTratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiesgoTratamientoRepository extends JpaRepository<RiesgoTratamiento, Long> {
    List<RiesgoTratamiento> findByRiesgo_IdOrderByIteracionDesc(Integer riesgoId);
    long countByRiesgo_Id(Integer riesgoId);
}
