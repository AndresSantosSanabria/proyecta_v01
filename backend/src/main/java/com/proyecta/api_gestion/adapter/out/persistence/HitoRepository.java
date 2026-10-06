package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.Hito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.HitoRepositoryPort;
@Repository
public interface HitoRepository extends JpaRepository<Hito, Integer>, HitoRepositoryPort {
    List<Hito> findByFaseId(Integer faseId);

    @Query("SELECT h FROM Hito h JOIN FETCH h.fase f WHERE f.proyecto.id = :proyectoId")
    List<Hito> findByProyectoId(@Param("proyectoId") String proyectoId);
}

