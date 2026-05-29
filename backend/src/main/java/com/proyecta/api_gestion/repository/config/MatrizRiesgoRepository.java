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
    @Query("SELECT m.nivelResultante FROM MatrizRiesgo m WHERE m.probabilidad = :probabilidad AND m.impacto = :impacto")
    Optional<String> findNivelByProbabilidadAndImpacto(@Param("probabilidad") String probabilidad, @Param("impacto") String impacto);

    Optional<MatrizRiesgo> findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(String probabilidad, String impacto);

    List<MatrizRiesgo> findAllByOrderByIdAsc();
}
