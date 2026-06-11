package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface SeguridadUsuarioRepository extends JpaRepository<SeguridadUsuario, Long> {
    Optional<SeguridadUsuario> findByUsernameIgnoreCase(String username);
    Optional<SeguridadUsuario> findByCorreoIgnoreCase(String correo);

    @Query("""
        SELECT u FROM SeguridadUsuario u
        WHERE u.activo = true
          AND (
            LOWER(COALESCE(u.rolCodigo, '')) IN ('director_proyecto', 'director_pro', 'director_tecnico', 'lider_tecnico')
            OR LOWER(COALESCE(u.rolNombre, '')) LIKE '%director%'
            OR LOWER(COALESCE(u.rolNombre, '')) LIKE '%lider%tecnico%'
          )
        ORDER BY u.nombre ASC
    """)
    List<SeguridadUsuario> findAssignableProjectDirectors();

    @Query("""
        SELECT u FROM SeguridadUsuario u
        WHERE (:search IS NULL OR :search = '' OR
            LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.correo) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(COALESCE(u.dependencia, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:rol IS NULL OR :rol = '' OR LOWER(COALESCE(u.rolCodigo, '')) = LOWER(:rol))
        ORDER BY u.nombre ASC
    """)
    Page<SeguridadUsuario> search(@Param("search") String search, @Param("rol") String rol, Pageable pageable);
}
