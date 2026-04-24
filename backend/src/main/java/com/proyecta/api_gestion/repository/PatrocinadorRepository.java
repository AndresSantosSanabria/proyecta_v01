package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Integer> {
}
