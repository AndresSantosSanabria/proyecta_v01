package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.PatrocinadorRepositoryPort;
@Repository
public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Integer>, PatrocinadorRepositoryPort {
    Optional<Patrocinador> findFirstByNombreOrderByIdAsc(String nombre);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Patrocinador p WHERE p.id IN (SELECT MIN(p2.id) FROM Patrocinador p2 GROUP BY p2.nombre)")
    java.util.List<Patrocinador> findUniquePatrocinadores();
}

