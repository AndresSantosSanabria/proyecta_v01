package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import java.util.Optional;
import java.util.List;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface SeguridadUsuarioRepositoryPort {

    Optional<SeguridadUsuario> findByUsernameIgnoreCase(String username);

    Optional<SeguridadUsuario> findByCorreoIgnoreCase(String correo);

    Optional<SeguridadUsuario> findByKeycloakSubIgnoreCase(String keycloakSub);

    List<SeguridadUsuario> findByNombreIgnoreCase(String nombre);

    List<SeguridadUsuario> findByRecibirNotificacionesGlobalesTrue();

    List<SeguridadUsuario> findAssignableProjectDirectors();

    PageResult<SeguridadUsuario> search(String search, String rol, PageQuery pageable);

    <E extends SeguridadUsuario> E save(E entity);

    <E extends SeguridadUsuario> java.util.List<E> saveAll(Iterable<E> entities);

    java.util.Optional<SeguridadUsuario> findById(Long id);

    java.util.List<SeguridadUsuario> findAll();

    <E extends SeguridadUsuario> E saveAndFlush(E entity);

    PageResult<SeguridadUsuario> findAll(PageQuery query);
}
