package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.dto.report.EntregablePendienteDTO;
import com.proyecta.api_gestion.model.Entregable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EntregableRepository extends JpaRepository<Entregable, Integer> {
    List<Entregable> findByHitoId(Integer hitoId);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE e.conforme = false AND e.fechaLimite < :hoy")
    long countAtrasadosTotal(@Param("hoy") LocalDate hoy);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado IN (com.proyecta.api_gestion.model.enums.EstadoProyecto.ACTIVO, com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS, com.proyecta.api_gestion.model.enums.EstadoProyecto.EN_REVISION) AND e.conforme = false AND e.fechaLimite BETWEEN :hoy AND :fin")
    long countProximosActivos(@Param("hoy") LocalDate hoy, @Param("fin") LocalDate fin);

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado IN (com.proyecta.api_gestion.model.enums.EstadoProyecto.ACTIVO, com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS, com.proyecta.api_gestion.model.enums.EstadoProyecto.EN_REVISION) AND e.conforme = true")
    BigDecimal sumPonderacionConformeActivos();

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado IN (com.proyecta.api_gestion.model.enums.EstadoProyecto.ACTIVO, com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS, com.proyecta.api_gestion.model.enums.EstadoProyecto.EN_REVISION) AND e.fechaLimite <= :hoy")
    BigDecimal sumPonderacionEsperadaActivos(@Param("hoy") LocalDate hoy);

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado IN (com.proyecta.api_gestion.model.enums.EstadoProyecto.ACTIVO, com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS, com.proyecta.api_gestion.model.enums.EstadoProyecto.EN_REVISION)")
    BigDecimal sumTotalPonderacionActivos();

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.id = :proyectoId AND e.conforme = false AND e.fechaLimite < :hoy")
    long countAtrasadosByProyecto(@Param("proyectoId") String proyectoId, @Param("hoy") LocalDate hoy);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.report.EntregablePendienteDTO(e.id, e.nombre, e.fechaLimite, f.descripcion)
        FROM Entregable e
        JOIN e.hito h
        JOIN h.fase f
        WHERE f.proyecto.id = :proyectoId
        AND e.conforme = false
        AND e.fechaLimite < :hoy
    """)
    List<EntregablePendienteDTO> findPendientesVencidosByProyecto(@Param("proyectoId") String proyectoId, @Param("hoy") LocalDate hoy);

    @Query("SELECT e FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = :proyectoId")
    List<Entregable> findByProyectoId(@Param("proyectoId") String proyectoId);
}
