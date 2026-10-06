package com.proyecta.api_gestion.application.port.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioProyecto;
import java.util.List;
import java.util.Optional;

public interface SeguridadUsuarioProyectoRepositoryPort {

    boolean existsByUsuarioUsernameIgnoreCaseAndProyectoIdIgnoreCaseAndActivoTrue(String username, String proyectoId);

    List<SeguridadUsuarioProyecto> findByUsername(String username);

    Optional<SeguridadUsuarioProyecto> findByUsuarioUsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(String username, String proyectoId, String cargo);

    List<SeguridadUsuarioProyecto> findActiveDirectorAssignmentsByProyectoId(String proyectoId);

    List<String> findProyectoIdsByUsername(String username);

    <E extends SeguridadUsuarioProyecto> E save(E entity);
}

