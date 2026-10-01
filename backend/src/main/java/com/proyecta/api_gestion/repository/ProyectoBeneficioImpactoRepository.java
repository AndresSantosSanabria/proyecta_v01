package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.beneficioimpacto.ProyectoBeneficioImpacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProyectoBeneficioImpactoRepository extends JpaRepository<ProyectoBeneficioImpacto, Long> {
    Optional<ProyectoBeneficioImpacto> findByProyecto_Id(String proyectoId);
}
