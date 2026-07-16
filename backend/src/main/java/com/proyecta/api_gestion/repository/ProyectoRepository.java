package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.dto.report.ProyectoReporteResumenDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, String>, JpaSpecificationExecutor<Proyecto> {

    Page<Proyecto> findAll(Specification<Proyecto> spec, Pageable pageable);

    boolean existsByNombreAndDependencia(String nombre, String dependencia);

    @Query("SELECT COUNT(p) FROM Proyecto p LEFT JOIN p.estadoConfig ec WHERE (ec IS NULL OR ec.codigo != 'PENDIENTE_COMPLETAR') AND (ec IS NOT NULL OR p.estado != com.proyecta.api_gestion.model.enums.EstadoProyecto.PENDIENTE_COMPLETAR)")
    long countTotal();

    @Query("SELECT COUNT(p) FROM Proyecto p LEFT JOIN p.estadoConfig ec WHERE (ec IS NOT NULL AND ec.codigo IN ('PLANIFICACION', 'ACTIVO', 'CON_RETRASOS', 'EN_REVISION')) OR (ec IS NULL AND p.estado IN (com.proyecta.api_gestion.model.enums.EstadoProyecto.PLANIFICACION, com.proyecta.api_gestion.model.enums.EstadoProyecto.ACTIVO, com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS, com.proyecta.api_gestion.model.enums.EstadoProyecto.EN_REVISION))")
    long countActivos();

    @Query("SELECT COUNT(p) FROM Proyecto p LEFT JOIN p.estadoConfig ec WHERE (ec IS NOT NULL AND ec.codigo = 'CERRADO') OR (ec IS NULL AND p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.CERRADO)")
    long countCerrados();

    @Query("SELECT AVG(p.avanceTotal) FROM Proyecto p LEFT JOIN p.estadoConfig ec WHERE (ec IS NULL OR ec.codigo != 'PENDIENTE_COMPLETAR') AND (ec IS NOT NULL OR p.estado != com.proyecta.api_gestion.model.enums.EstadoProyecto.PENDIENTE_COMPLETAR)")
    BigDecimal getAvancePromedio();

    @Query("""
        SELECT COUNT(e) FROM Entregable e 
        JOIN e.hito h 
        JOIN h.fase f 
        JOIN f.proyecto p 
        LEFT JOIN e.estadoConfig ec
        WHERE ((ec IS NOT NULL AND ec.codigo NOT IN ('COMPLETADO', 'A_CONFORMIDAD')) OR (ec IS NULL AND e.estado NOT IN (com.proyecta.api_gestion.model.enums.EstadoEntregable.COMPLETADO, com.proyecta.api_gestion.model.enums.EstadoEntregable.A_CONFORMIDAD))) AND e.fechaLimite < :hoy
    """)
    long countEntregablesAtrasados(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT COUNT(e) FROM Entregable e 
        JOIN e.hito h 
        JOIN h.fase f 
        JOIN f.proyecto p 
        LEFT JOIN e.estadoConfig ec
        WHERE ((ec IS NOT NULL AND ec.codigo NOT IN ('COMPLETADO', 'A_CONFORMIDAD')) OR (ec IS NULL AND e.estado NOT IN (com.proyecta.api_gestion.model.enums.EstadoEntregable.COMPLETADO, com.proyecta.api_gestion.model.enums.EstadoEntregable.A_CONFORMIDAD)))
        AND e.fechaLimite >= :hoy 
        AND e.fechaLimite <= :umbral
    """)
    long countEntregablesProximosAVencer(@Param("hoy") LocalDate hoy, @Param("umbral") LocalDate umbral);

    @Query(value = "SELECT CAST(param_value AS INTEGER) FROM system_parameters WHERE param_key = 'dias_umbral_proximo'", nativeQuery = true)
    Optional<Integer> getDiasUmbralProximo();

    @Query(value = """
        WITH metricas_base AS (
            SELECT
                p.proyecto_id,
                p.avance_total,
                e.entregable_id,
                e.estado,
                e.fecha_limite
            FROM proyecto p
            LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
            LEFT JOIN hito h ON f.fase_id = h.fase_id
            LEFT JOIN entregable e ON h.hito_id = e.hito_id
            WHERE p.proyecto_id = :proyectoId
        )
        SELECT
            COALESCE(MAX(avance_total), 0)                                               AS avance_total,
            COUNT(entregable_id) FILTER (WHERE estado IN ('COMPLETADO', 'A_CONFORMIDAD'))  AS entregables_conformes,
            COUNT(entregable_id)                                                             AS total_entregables,
            CONCAT(
                COUNT(entregable_id) FILTER (WHERE estado IN ('COMPLETADO', 'A_CONFORMIDAD')),
                '/',
                COUNT(entregable_id)
            )                                                                                AS entregables_label,
            COUNT(entregable_id) FILTER (
                WHERE estado NOT IN ('COMPLETADO', 'A_CONFORMIDAD') AND fecha_limite < CURRENT_DATE
            )                                                                                AS atrasados,
            COUNT(entregable_id) FILTER (
                WHERE estado NOT IN ('COMPLETADO', 'A_CONFORMIDAD')
                AND fecha_limite >= CURRENT_DATE
                AND fecha_limite <= (CURRENT_DATE + (
                    SELECT (param_value || ' days')::INTERVAL
                    FROM system_parameters
                    WHERE param_key = 'ventana_vencimiento_dias'
                ))
            )                                                                                AS proximos_vencer
        FROM metricas_base
        """, nativeQuery = true)
    Optional<com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO> getAvanceProyecto(@Param("proyectoId") String proyectoId);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.report.ProyectoReporteResumenDTO(
            p.id, p.nombre, p.avanceTotal, p.dependencia, CAST(p.estado AS string),
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estado NOT IN (com.proyecta.api_gestion.model.enums.EstadoEntregable.COMPLETADO, com.proyecta.api_gestion.model.enums.EstadoEntregable.A_CONFORMIDAD) AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        LEFT JOIN p.estadoConfig ec
        WHERE (ec IS NULL OR ec.codigo != 'PENDIENTE_COMPLETAR') AND (ec IS NOT NULL OR p.estado != com.proyecta.api_gestion.model.enums.EstadoProyecto.PENDIENTE_COMPLETAR)
    """)
    List<ProyectoReporteResumenDTO> getProyectosResumen(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.report.ProyectoReporteResumenDTO(
            p.id, p.nombre, p.avanceTotal, p.dependencia, CAST(p.estado AS string),
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estado NOT IN (com.proyecta.api_gestion.model.enums.EstadoEntregable.COMPLETADO, com.proyecta.api_gestion.model.enums.EstadoEntregable.A_CONFORMIDAD) AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.CON_RETRASOS
    """)
    List<ProyectoReporteResumenDTO> getProyectosConAtrasosResumen(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO(
            p.id, 
            p.nombre, 
            p.dependencia, 
            p.avanceTotal, 
            CAST(p.estado AS string), 
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estado NOT IN (com.proyecta.api_gestion.model.enums.EstadoEntregable.COMPLETADO, com.proyecta.api_gestion.model.enums.EstadoEntregable.A_CONFORMIDAD) AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        LEFT JOIN p.estadoConfig ec
        WHERE (ec IS NULL OR ec.codigo != 'PENDIENTE_COMPLETAR') AND (ec IS NOT NULL OR p.estado != com.proyecta.api_gestion.model.enums.EstadoProyecto.PENDIENTE_COMPLETAR)
    """)
    List<DashboardProjectSummaryDTO> getDashboardProjectSummary(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.dto.dashboard.ProjectsByDependenciaDTO(
            p.dependencia, 
            COUNT(p), 
            AVG(p.avanceTotal)
        )
        FROM Proyecto p
        LEFT JOIN p.estadoConfig ec
        WHERE (ec IS NULL OR ec.codigo != 'PENDIENTE_COMPLETAR') AND (ec IS NOT NULL OR p.estado != com.proyecta.api_gestion.model.enums.EstadoProyecto.PENDIENTE_COMPLETAR)
        GROUP BY p.dependencia
    """)
    List<com.proyecta.api_gestion.dto.dashboard.ProjectsByDependenciaDTO> getProjectsByDependencia();
}
