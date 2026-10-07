package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO;
import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import java.util.ArrayList;
import java.util.Collection;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.domain.model.enums.EstadoProyecto;
@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, String>, JpaSpecificationExecutor<Proyecto>, ProyectoRepositoryPort {
    @Override
    default PageResult<Proyecto> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }


    @Override
    @EntityGraph(attributePaths = {"objetivosEspecificos", "equipoTrabajo", "stakeholders", "patrocinador"})
    Optional<Proyecto> findById(String id);

    Page<Proyecto> findAll(Specification<Proyecto> spec, Pageable pageable);

    boolean existsByNombreAndDependencia(String nombre, String dependencia);

    @Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR'")
    long countTotal();

    @Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estadoConfig.codigo IN ('PLANIFICACION', 'ACTIVO', 'CON_RETRASOS', 'EN_REVISION')")
    long countActivos();

    @Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estadoConfig.codigo IN ('CERRADO', 'CERRADO_FORZOSO')")
    long countCerrados();

    @Query("SELECT AVG(p.avanceTotal) FROM Proyecto p WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR'")
    BigDecimal getAvancePromedio();

    @Query("""
        SELECT COUNT(e) FROM Entregable e 
        JOIN e.hito h 
        JOIN h.fase f 
        JOIN f.proyecto p 
        WHERE e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite < :hoy
    """)
    long countEntregablesAtrasados(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT COUNT(e) FROM Entregable e 
        JOIN e.hito h 
        JOIN h.fase f 
        JOIN f.proyecto p 
        WHERE e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO')
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
                ec.codigo AS estado,
                e.fecha_limite
            FROM proyecto p
            LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
            LEFT JOIN hito h ON f.fase_id = h.fase_id
            LEFT JOIN entregable e ON h.hito_id = e.hito_id
            LEFT JOIN estado_entregable_config ec ON e.estado_config_id = ec.estado_entregable_id
            WHERE p.proyecto_id = :proyectoId
        )
        SELECT
            COALESCE(MAX(avance_total), 0)                                               AS avance_total,
            COUNT(entregable_id) FILTER (WHERE estado IN ('COMPLETADO', 'APROBADO'))  AS entregables_conformes,
            COUNT(entregable_id)                                                             AS total_entregables,
            CONCAT(
                COUNT(entregable_id) FILTER (WHERE estado IN ('COMPLETADO', 'APROBADO')),
                '/',
                COUNT(entregable_id)
            )                                                                                AS entregables_label,
            COUNT(entregable_id) FILTER (
                WHERE estado NOT IN ('COMPLETADO', 'APROBADO') AND fecha_limite < CURRENT_DATE
            )                                                                                AS atrasados,
            COUNT(entregable_id) FILTER (
                WHERE estado NOT IN ('COMPLETADO', 'APROBADO')
                AND fecha_limite >= CURRENT_DATE
                AND fecha_limite <= (CURRENT_DATE + (
                    SELECT (param_value || ' days')::INTERVAL
                    FROM system_parameters
                    WHERE param_key = 'ventana_vencimiento_dias'
                ))
            )                                                                                AS proximos_vencer
        FROM metricas_base
        """, nativeQuery = true)
    Optional<com.proyecta.api_gestion.application.readmodel.ProyectoAvanceDetalleDTO> getAvanceProyecto(@Param("proyectoId") String proyectoId);

    @Query("""
        SELECT new com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO(
            p.id, p.nombre, p.avanceTotal, p.dependencia, p.estadoConfig.codigo,
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR'
    """)
    List<ProyectoReporteResumenDTO> getProyectosResumen(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO(
            p.id, p.nombre, p.avanceTotal, p.dependencia, p.estadoConfig.codigo,
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        WHERE p.estadoConfig.codigo = 'CON_RETRASOS'
    """)
    List<ProyectoReporteResumenDTO> getProyectosConAtrasosResumen(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO(
            p.id, 
            p.nombre, 
            p.dependencia, 
            p.avanceTotal, 
            p.estadoConfig.codigo, 
            (SELECT COUNT(e) FROM Entregable e JOIN e.hito h JOIN h.fase f WHERE f.proyecto.id = p.id AND e.estadoConfig.codigo NOT IN ('COMPLETADO', 'APROBADO') AND e.fechaLimite < :now)
        )
        FROM Proyecto p
        WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR'
    """)
    List<DashboardProjectSummaryDTO> getDashboardProjectSummary(@Param("now") LocalDate now);

    @Query("""
        SELECT new com.proyecta.api_gestion.application.readmodel.ProjectsByDependenciaDTO(
            p.dependencia, 
            COUNT(p), 
            AVG(p.avanceTotal)
        )
        FROM Proyecto p
        WHERE p.estadoConfig.codigo != 'PENDIENTE_COMPLETAR'
        GROUP BY p.dependencia
    """)
    List<com.proyecta.api_gestion.application.readmodel.ProjectsByDependenciaDTO> getProjectsByDependencia();

    List<Proyecto> findByDocumentosVerificadosTrueAndRequiereCompletitudDirectorTrueAndCierreForzosoFalse();

    /**
     * Verifica si un usuario es el director de un proyecto usando una consulta JPQL
     * directa. Esto evita el LazyInitializationException que ocurre al acceder
     * a la relacion directorUsuario fuera de un contexto transaccional activo
     * (por ejemplo, en los beans de seguridad de Spring Security).
     *
     * @param proyectoId ID del proyecto (case-insensitive)
     * @param username   Username del usuario a verificar (case-insensitive)
     * @return true si el usuario es el director asignado al proyecto
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Proyecto p
        WHERE UPPER(p.id) = UPPER(:proyectoId)
          AND p.directorUsuario IS NOT NULL
          AND LOWER(p.directorUsuario.username) = LOWER(:username)
    """)
    boolean existsDirectorByProyectoIdAndUsername(
            @Param("proyectoId") String proyectoId,
            @Param("username") String username);

    @Override
    default PageResult<Proyecto> listarProyectos(String nombre, String codigo, String dependencia,
            EstadoProyecto estado, Boolean peti, PageQuery query) {
        Specification<Proyecto> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (codigo != null && !codigo.isBlank()) {
                predicates.add(cb.equal(root.get("id"), codigo));
            }
            if (dependencia != null && !dependencia.isBlank()) {
                predicates.add(cb.equal(root.get("dependencia"), dependencia));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.join("estadoConfig").get("codigo"), estado.name()));
            }
            if (peti != null) {
                predicates.add(cb.equal(root.get("peti"), peti));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageBridge.toResult(findAll(spec, PageBridge.toPageable(query)), query);
    }

    @Override
    default List<Proyecto> findByIdsOrdenadoPorIdDesc(Collection<String> ids) {
        Specification<Proyecto> spec = (root, q, cb) -> root.get("id").in(ids);
        return findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
    }

}

