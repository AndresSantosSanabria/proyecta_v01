package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    @EntityGraph(attributePaths = "rolConfig")
    Optional<Usuario> findByKeycloakSubIgnoreCase(String keycloakSub);

    @EntityGraph(attributePaths = "rolConfig")
    Optional<Usuario> findByCorreo(String correo);

    @EntityGraph(attributePaths = "rolConfig")
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
