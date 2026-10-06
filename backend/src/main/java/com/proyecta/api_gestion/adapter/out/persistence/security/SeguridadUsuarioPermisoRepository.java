package com.proyecta.api_gestion.adapter.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioPermisoRepositoryPort;
public interface SeguridadUsuarioPermisoRepository extends JpaRepository<SeguridadUsuarioPermiso, Long>, SeguridadUsuarioPermisoRepositoryPort {

    List<SeguridadUsuarioPermiso> findByUsuarioId(Long usuarioId);

    @Query("SELECT up.permiso.codigo FROM SeguridadUsuarioPermiso up WHERE up.usuario.id = :usuarioId AND up.concedido = true")
    Set<String> findGrantedPermissionCodesByUsuarioId(Long usuarioId);

    @Query("SELECT up.permiso.codigo FROM SeguridadUsuarioPermiso up WHERE up.usuario.id = :usuarioId AND up.concedido = false")
    Set<String> findDeniedPermissionCodesByUsuarioId(Long usuarioId);
}

