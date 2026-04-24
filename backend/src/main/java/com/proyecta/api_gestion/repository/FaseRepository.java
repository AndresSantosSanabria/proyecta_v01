package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Fase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Integer> {
    List<Fase> findByProyectoIdOrderByNumeroAsc(String proyectoId);
}
