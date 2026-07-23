package com.proyecta.api_gestion.repository.security;

import com.proyecta.api_gestion.model.security.SeguridadUsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface SeguridadUsuarioPermisoRepository extends JpaRepository<SeguridadUsuarioPermiso, Long> {

    List<SeguridadUsuarioPermiso> findByUsuario_Id(Long usuarioId);

    Set<Long> findPermisoIdsByUsuario_Id(Long usuarioId);

    @Modifying
    @Transactional
    void deleteByUsuario_Id(Long usuarioId);

    @Query("SELECT up.permiso.codigo FROM SeguridadUsuarioPermiso up WHERE up.usuario.id = :usuarioId AND up.concedido = true")
    Set<String> findGrantedPermissionCodesByUsuarioId(Long usuarioId);

    @Query("SELECT up.permiso.codigo FROM SeguridadUsuarioPermiso up WHERE up.usuario.id = :usuarioId AND up.concedido = false")
    Set<String> findDeniedPermissionCodesByUsuarioId(Long usuarioId);

    boolean existsByUsuario_IdAndPermiso_CodigoIgnoreCase(Long usuarioId, String permisoCodigo);
}
