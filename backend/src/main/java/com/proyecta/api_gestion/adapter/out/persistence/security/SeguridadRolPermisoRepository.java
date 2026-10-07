package com.proyecta.api_gestion.adapter.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadRolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadRolPermisoRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface SeguridadRolPermisoRepository extends JpaRepository<SeguridadRolPermiso, Long>, SeguridadRolPermisoRepositoryPort {
    @Override
    default PageResult<SeguridadRolPermiso> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    @Query("""
        SELECT rp FROM SeguridadRolPermiso rp
        JOIN FETCH rp.rol r
        JOIN FETCH rp.permiso p
        WHERE r.codigo IN :roleCodes AND rp.activo = true AND r.activo = true AND p.activo = true
    """)
    List<SeguridadRolPermiso> findActiveByRoleCodes(@Param("roleCodes") Collection<String> roleCodes);

    @Query("""
        SELECT rp FROM SeguridadRolPermiso rp
        JOIN FETCH rp.rol r
        JOIN FETCH rp.permiso p
        WHERE rp.activo = true
        ORDER BY r.codigo ASC, p.codigo ASC
    """)
    List<SeguridadRolPermiso> findAllActiveWithRelations();
}
