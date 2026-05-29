package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.dto.report.RiesgoReporteDTO;
import com.proyecta.api_gestion.model.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;

@Repository
public interface RiesgoRepository extends JpaRepository<Riesgo, Integer> {
    List<Riesgo> findByProyectoId(String proyectoId);

    long countByProyecto_Id(String proyectoId);

    long countByProyecto_IdAndEstado(String proyectoId, EstadoRiesgo estado);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.report.RiesgoReporteDTO(
            r.id, r.codigo, r.descripcion, r.probabilidad, r.impacto, r.nivel, r.tratamiento, r.estado, r.fechaActualizacion
        )
        FROM Riesgo r
        WHERE r.proyecto.id = :proyectoId
    """)
    List<RiesgoReporteDTO> findRiesgosReporteByProyecto(@Param("proyectoId") String proyectoId);
}
