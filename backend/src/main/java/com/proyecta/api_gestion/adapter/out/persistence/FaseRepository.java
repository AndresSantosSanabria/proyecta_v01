package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.Fase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.FaseRepositoryPort;
@Repository
public interface FaseRepository extends JpaRepository<Fase, Integer>, FaseRepositoryPort {
    List<Fase> findByProyectoId(String proyectoId);
}

