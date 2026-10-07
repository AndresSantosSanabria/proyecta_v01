package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.ActaCierre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.ActaCierreRepositoryPort;
@Repository
public interface ActaCierreRepository extends JpaRepository<ActaCierre, Long>, ActaCierreRepositoryPort {
    Optional<ActaCierre> findByProyectoId(String proyectoId);
}

