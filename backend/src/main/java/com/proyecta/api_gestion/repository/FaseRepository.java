package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Fase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Integer> {
    List<Fase> findByProyectoId(String proyectoId);

    @Query("SELECT AVG(f.avanceCalculado) FROM Fase f WHERE f.proyecto.id = :proyectoId")
    BigDecimal getAvancePromedioByProyecto(@Param("proyectoId") String proyectoId);
}
