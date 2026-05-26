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
}
