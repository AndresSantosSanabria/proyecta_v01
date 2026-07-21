package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeguridadUsuarioProyectoRepository extends JpaRepository<SeguridadUsuarioProyecto, Long> {
    boolean existsByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndActivoTrue(String username, String proyectoId);

    @Query("""
        SELECT up FROM SeguridadUsuarioProyecto up
        JOIN FETCH up.usuario u
        WHERE LOWER(u.username) = LOWER(:username)
        ORDER BY up.proyectoId ASC, up.cargo ASC
    """)
    List<SeguridadUsuarioProyecto> findByUsername(@Param("username") String username);

    Optional<SeguridadUsuarioProyecto> findByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(String username, String proyectoId, String cargo);

    @Query("""
        SELECT up FROM SeguridadUsuarioProyecto up
        JOIN FETCH up.usuario u
        WHERE LOWER(up.proyectoId) = LOWER(:proyectoId)
          AND LOWER(up.cargo) = LOWER(:cargo)
          AND up.activo = true
        ORDER BY up.fechaAsignacion DESC
    """)
    Optional<SeguridadUsuarioProyecto> findFirstByProyectoIdIgnoreCaseAndCargoIgnoreCaseAndActivoTrueOrderByFechaAsignacionDesc(
            @Param("proyectoId") String proyectoId,
            @Param("cargo") String cargo);

    @Query("""
        SELECT up FROM SeguridadUsuarioProyecto up
        JOIN FETCH up.usuario u
        WHERE LOWER(up.proyectoId) = LOWER(:proyectoId)
          AND up.activo = true
          AND LOWER(up.cargo) IN :cargos
        ORDER BY up.fechaAsignacion DESC
    """)
    List<SeguridadUsuarioProyecto> findActivasByProyectoIdAndCargoIn(
            @Param("proyectoId") String proyectoId,
            @Param("cargos") List<String> cargos);

    @Query("""
        SELECT up FROM SeguridadUsuarioProyecto up
        JOIN FETCH up.usuario u
        WHERE LOWER(up.proyectoId) = LOWER(:proyectoId)
          AND up.activo = true
        ORDER BY up.fechaAsignacion DESC
    """)
    List<SeguridadUsuarioProyecto> findActivasByProyectoId(@Param("proyectoId") String proyectoId);

    @Query("""
        SELECT up FROM SeguridadUsuarioProyecto up
        JOIN FETCH up.usuario u
        WHERE LOWER(up.proyectoId) = LOWER(:proyectoId)
          AND up.activo = true
          AND (
            LOWER(up.cargo) IN ('director_proyecto', 'director de proyecto', 'director_pro', 'lider_tecnico', 'lider tecnico', 'director_tecnico', 'director tecnico')
            OR LOWER(up.cargo) LIKE '%director%proyecto%'
            OR LOWER(up.cargo) LIKE '%director%tecnico%'
            OR LOWER(up.cargo) LIKE '%lider%tecnico%'
          )
        ORDER BY up.fechaAsignacion DESC
    """)
    List<SeguridadUsuarioProyecto> findActiveDirectorAssignmentsByProyectoId(@Param("proyectoId") String proyectoId);

    @Query("""
        SELECT DISTINCT up.proyectoId
        FROM SeguridadUsuarioProyecto up
        WHERE LOWER(up.usuario.username) = LOWER(:username)
          AND up.activo = true
        ORDER BY up.proyectoId ASC
    """)
    List<String> findProyectoIdsByUsername(@Param("username") String username);
}
