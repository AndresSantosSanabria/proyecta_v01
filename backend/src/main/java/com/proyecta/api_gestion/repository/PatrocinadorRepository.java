package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Integer> {
    Optional<Patrocinador> findByNombre(String nombre);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Patrocinador p WHERE p.id IN (SELECT MIN(p2.id) FROM Patrocinador p2 GROUP BY p2.nombre)")
    java.util.List<Patrocinador> findUniquePatrocinadores();
}
