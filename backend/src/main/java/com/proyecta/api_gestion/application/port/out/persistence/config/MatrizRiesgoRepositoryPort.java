package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.MatrizRiesgo;
import java.util.List;
import java.util.Optional;

public interface MatrizRiesgoRepositoryPort {

    Optional<MatrizRiesgo> findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(String probabilidad, String impacto);

    List<MatrizRiesgo> findAllByOrderByIdAsc();
}

