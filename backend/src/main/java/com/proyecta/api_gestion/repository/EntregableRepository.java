package com.proyecta.api_gestion.repository;

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
    List<Entregable> findByHitoIdOrderByNumeroAsc(Integer hitoId);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo AND e.conforme = false AND e.fechaEntrega < :hoy")
    long countAtrasadosActivos(@Param("hoy") LocalDate hoy);

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo AND e.conforme = false AND e.fechaEntrega BETWEEN :hoy AND :fin")
    long countProximosActivos(@Param("hoy") LocalDate hoy, @Param("fin") LocalDate fin);

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo AND e.conforme = true")
    BigDecimal sumPonderacionConformeActivos();

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo AND e.fechaEntrega <= :hoy")
    BigDecimal sumPonderacionEsperadaActivos(@Param("hoy") LocalDate hoy);

    @Query("SELECT SUM(e.ponderacion) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.estado = com.proyecta.api_gestion.model.enums.EstadoProyecto.activo")
    BigDecimal sumTotalPonderacionActivos();

    @Query("SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f JOIN f.proyecto p WHERE p.id = :proyectoId AND e.conforme = false AND e.fechaEntrega < :hoy")
    long countAtrasadosByProyecto(@Param("proyectoId") String proyectoId, @Param("hoy") LocalDate hoy);
    
    
}
