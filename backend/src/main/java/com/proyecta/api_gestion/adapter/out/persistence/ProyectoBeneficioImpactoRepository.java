package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.beneficioimpacto.ProyectoBeneficioImpacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.ProyectoBeneficioImpactoRepositoryPort;
public interface ProyectoBeneficioImpactoRepository extends JpaRepository<ProyectoBeneficioImpacto, Long>, ProyectoBeneficioImpactoRepositoryPort {
    Optional<ProyectoBeneficioImpacto> findByProyectoId(String proyectoId);
}

