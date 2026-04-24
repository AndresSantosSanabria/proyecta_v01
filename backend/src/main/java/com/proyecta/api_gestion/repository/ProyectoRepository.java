package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import java.util.Optional;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, String> {

    // --- Consultas de Entidades ---

    @Query("SELECT p FROM Proyecto p WHERE p.estado = 'activo' AND p.avanceCalculado > :minimo")
    List<Proyecto> buscarProyectosConAvanceMayorA(@Param("minimo") BigDecimal minimo);

    long countByEstado(EstadoProyecto estado);

    @Query("SELECT AVG(p.avanceCalculado) FROM Proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo")
    BigDecimal getAvancePromedioActivos();

    List<Proyecto> findByEstado(EstadoProyecto estado);

    @Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo AND EXISTS (SELECT e FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto = p AND e.conforme = false AND e.fechaEntrega < :hoy)")
    long countProyectosConAtrasos(@Param("hoy") LocalDate hoy);

    // --- Consulta para el DTO del Dashboard ---

    @Query("""
                SELECT new com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO(
                    p.id,
                    p.nombre,
                    p.dependencia,
                    CAST(p.avanceCalculado AS integer),
                    (CASE WHEN SUM(CASE WHEN e.conforme = false AND e.fechaEntrega < :hoy THEN 1 ELSE 0 END) > 0
                          THEN 'Con retrasos' ELSE 'Al día' END),
                    CAST(COALESCE(SUM(CASE WHEN e.conforme = false AND e.fechaEntrega < :hoy THEN 1 ELSE 0 END), 0) AS integer)
                )
                FROM Proyecto p
                LEFT JOIN Fase f ON f.proyecto = p
                LEFT JOIN Hito h ON h.fase = f
                LEFT JOIN Entregable e ON e.hito = h
                GROUP BY p.id, p.nombre, p.dependencia, p.avanceCalculado
            """)
    List<DashboardProjectSummaryDTO> getDashboardProjectSummary(@Param("hoy") LocalDate hoy);

    // --- Consulta nativa de avance detallado por proyecto ---

    @Query(value = """
        WITH metricas_base AS (
            SELECT
                p.proyecto_id,
                p.avance_calculado,
                e.entregable_id,
                e.conforme,
                e.fecha_entrega
            FROM proyecta_db.proyecto p
            LEFT JOIN proyecta_db.fase f ON p.proyecto_id = f.proyecto_id
            LEFT JOIN proyecta_db.hito h ON f.fase_id = h.fase_id
            LEFT JOIN proyecta_db.entregable e ON h.hito_id = e.hito_id
            WHERE p.proyecto_id = :proyectoId
        )
        SELECT
            COALESCE(MAX(avance_calculado), 0)                                               AS avance_total,
            COUNT(entregable_id) FILTER (WHERE conforme = TRUE)                              AS entregables_conformes,
            COUNT(entregable_id)                                                             AS total_entregables,
            CONCAT(
                COUNT(entregable_id) FILTER (WHERE conforme = TRUE),
                '/',
                COUNT(entregable_id)
            )                                                                                AS entregables_label,
            COUNT(entregable_id) FILTER (
                WHERE conforme = FALSE AND fecha_entrega < CURRENT_DATE
            )                                                                                AS atrasados,
            COUNT(entregable_id) FILTER (
                WHERE conforme = FALSE
                AND fecha_entrega >= CURRENT_DATE
                AND fecha_entrega <= (CURRENT_DATE + (
                    SELECT (param_value || ' days')::INTERVAL
                    FROM proyecta_db.system_parameters
                    WHERE param_key = 'dias_alerta_vencimiento'
                ))
            )                                                                                AS proximos_vencer
        FROM metricas_base
        """, nativeQuery = true)
    Optional<ProyectoAvanceDetalleDTO> getAvanceProyecto(@Param("proyectoId") String proyectoId);
}