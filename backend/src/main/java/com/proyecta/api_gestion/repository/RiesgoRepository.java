package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;

@Repository
public interface RiesgoRepository extends JpaRepository<Riesgo, Integer> {
    List<Riesgo> findByProyectoId(String proyectoId);

    long countByProyecto_Id(String proyectoId);

    long countByProyecto_IdAndEstado(String proyectoId, EstadoRiesgo estado);
}
