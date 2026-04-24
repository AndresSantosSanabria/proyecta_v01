package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.dto.DashboardDto.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;

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
        SELECT new com.proyecta.api_gestion.dto.DashboardDto.DashboardProjectSummaryDTO(
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
}