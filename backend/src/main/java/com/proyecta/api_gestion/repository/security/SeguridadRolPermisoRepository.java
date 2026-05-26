package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadRolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SeguridadRolPermisoRepository extends JpaRepository<SeguridadRolPermiso, Long> {
    @Query("""
        SELECT rp FROM SeguridadRolPermiso rp
        JOIN FETCH rp.rol r
        JOIN FETCH rp.permiso p
        WHERE r.codigo IN :roleCodes AND rp.activo = true AND r.activo = true AND p.activo = true
    """)
    List<SeguridadRolPermiso> findActiveByRoleCodes(@Param("roleCodes") Collection<String> roleCodes);

    @Modifying
    @Query("DELETE FROM SeguridadRolPermiso rp WHERE rp.rol.codigo IN :roleCodes")
    void deleteByRoleCodes(@Param("roleCodes") Set<String> roleCodes);

    @Query("""
        SELECT rp FROM SeguridadRolPermiso rp
        JOIN FETCH rp.rol r
        JOIN FETCH rp.permiso p
        WHERE rp.activo = true
        ORDER BY r.codigo ASC, p.codigo ASC
    """)
    List<SeguridadRolPermiso> findAllActiveWithRelations();
}
