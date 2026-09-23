package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.MatrizRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatrizRiesgoRepository extends JpaRepository<MatrizRiesgo, Long> {
    Optional<MatrizRiesgo> findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(String probabilidad, String impacto);

    List<MatrizRiesgo> findAllByOrderByIdAsc();
}
