package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioPermiso;
import java.util.List;
import java.util.Set;

public interface SeguridadUsuarioPermisoRepositoryPort {

    List<SeguridadUsuarioPermiso> findByUsuarioId(Long usuarioId);

    Set<String> findGrantedPermissionCodesByUsuarioId(Long usuarioId);

    Set<String> findDeniedPermissionCodesByUsuarioId(Long usuarioId);

    <E extends SeguridadUsuarioPermiso> java.util.List<E> saveAll(Iterable<E> entities);
}

