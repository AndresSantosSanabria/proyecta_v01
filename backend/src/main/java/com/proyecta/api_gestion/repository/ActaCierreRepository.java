package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.ActaCierre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActaCierreRepository extends JpaRepository<ActaCierre, Long> {
    Optional<ActaCierre> findByProyectoId(String proyectoId);
}
