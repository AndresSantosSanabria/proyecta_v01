package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.MatrizRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.MatrizRiesgoRepositoryPort;
@Repository
public interface MatrizRiesgoRepository extends JpaRepository<MatrizRiesgo, Long>, MatrizRiesgoRepositoryPort {
    Optional<MatrizRiesgo> findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(String probabilidad, String impacto);

    List<MatrizRiesgo> findAllByOrderByIdAsc();
}

