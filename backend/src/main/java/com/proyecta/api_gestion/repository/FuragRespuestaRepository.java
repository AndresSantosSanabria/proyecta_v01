package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.FuragRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FuragRespuestaRepository extends JpaRepository<FuragRespuesta, Long> {
    long countByProyecto_IdAndRespuestaIsNotNull(String proyectoId);

    long countByProyecto_IdAndObligatoriaTrue(String proyectoId);

    long countByProyecto_IdAndObligatoriaTrueAndRespuestaIsNotNull(String proyectoId);

    List<FuragRespuesta> findByProyecto_IdOrderByCodigoPreguntaAsc(String proyectoId);

    @Modifying
    void deleteByProyecto_Id(@Param("proyectoId") String proyectoId);
}
