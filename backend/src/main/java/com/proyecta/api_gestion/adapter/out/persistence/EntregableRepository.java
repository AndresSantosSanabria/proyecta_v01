package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.application.readmodel.EntregablePendienteDTO;
import com.proyecta.api_gestion.domain.model.Entregable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
@Repository
public interface EntregableRepository extends JpaRepository<Entregable, Integer>, EntregableRepositoryPort {
    List<Entregable> findByHitoId(Integer hitoId);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR' AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite < :hoy")
    long countAtrasadosTotal(@Param("hoy") LocalDate hoy);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estadoConfig.codigo IN ('PLANIFICACION', 'ACTIVO', 'CON_RETRASOS', 'EN_REVISION') AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite BETWEEN :hoy AND :fin")
    long countProximosActivos(@Param("hoy") LocalDate hoy, @Param("fin") LocalDate fin);

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estadoConfig.codigo IN ('PLANIFICACION', 'ACTIVO', 'CON_RETRASOS', 'EN_REVISION') AND e.estadoConfig.codigo IN ('COMPLETADO', 'APROBADO')")
    BigDecimal sumPonderacionConformeActivos();

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estadoConfig.codigo IN ('PLANIFICACION', 'ACTIVO', 'CON_RETRASOS', 'EN_REVISION') AND e.fechaLimite <= :hoy")
    BigDecimal sumPonderacionEsperadaActivos(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT new com.proyecta.api_gestion.application.readmodel.EntregablePendienteDTO(e.id, e.nombre, e.fechaLimite, f.descripcion)
        FROM Entregable e
        JOIN e.hito h
        JOIN h.fase f
        WHERE f.proyecto.id = :proyectoId
        AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO')
        AND e.fechaLimite < :hoy
    """)
    List<EntregablePendienteDTO> findPendientesVencidosByProyecto(@Param("proyectoId") String proyectoId, @Param("hoy") LocalDate hoy);

    @Query("SELECT e FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = :proyectoId")
    List<Entregable> findByProyectoId(@Param("proyectoId") String proyectoId);

    @Query("""
        SELECT e
        FROM Entregable e
        JOIN FETCH e.hito h
        JOIN FETCH h.fase f
        JOIN FETCH f.proyecto p
        WHERE e.fechaLimite IS NOT NULL
        AND e.fechaLimite > :hoy
        AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO')
        AND p.estadoConfig.codigo IN ('ACTIVO', 'CON_RETRASOS', 'EN_REVISION')
    """)
    List<Entregable> findNoCompletados(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT e
        FROM Entregable e
        JOIN FETCH e.hito h
        JOIN FETCH h.fase f
        JOIN FETCH f.proyecto p
        WHERE e.fechaLimite IS NOT NULL
        AND e.fechaLimite < :hoy
        AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO')
        AND p.estadoConfig.codigo IN ('ACTIVO', 'CON_RETRASOS', 'EN_REVISION')
    """)
    List<Entregable> findVencidosNoEntregados(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT e
        FROM Entregable e
        JOIN FETCH e.hito h
        JOIN FETCH h.fase f
        JOIN FETCH f.proyecto p
        WHERE e.id = :entregableId
        AND UPPER(p.id) = UPPER(:proyectoId)
    """)
    Optional<Entregable> findByIdAndProyectoIdWithHierarchy(
            @Param("entregableId") Integer entregableId,
            @Param("proyectoId") String proyectoId);
}

