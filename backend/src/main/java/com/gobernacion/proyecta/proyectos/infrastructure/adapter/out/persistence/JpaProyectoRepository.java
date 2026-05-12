package com.gobernacion.proyecta.proyectos.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProyectoRepository extends JpaRepository<ProyectoEntity, String> {
}
