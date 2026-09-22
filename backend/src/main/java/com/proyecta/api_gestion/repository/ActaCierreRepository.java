package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.ActaCierre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActaCierreRepository extends JpaRepository<ActaCierre, Long> {
    Optional<ActaCierre> findByProyectoId(String proyectoId);

    @Modifying
    @Query(value = "DELETE FROM proyecta_db.actas_cierre WHERE proyecto_id = :proyectoId", nativeQuery = true)
    int deleteByProyectoId(@Param("proyectoId") String proyectoId);
}
