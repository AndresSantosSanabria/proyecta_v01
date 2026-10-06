package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.FuragRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.FuragRespuestaRepositoryPort;
public interface FuragRespuestaRepository extends JpaRepository<FuragRespuesta, Long>, FuragRespuestaRepositoryPort {
    long countByProyectoIdAndObligatoriaTrue(String proyectoId);

    long countByProyectoIdAndObligatoriaTrueAndRespuestaIsNotNull(String proyectoId);

    List<FuragRespuesta> findByProyectoIdOrderByCodigoPreguntaAsc(String proyectoId);

    List<FuragRespuesta> findByProyectoIdIn(java.util.Collection<String> proyectoIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FuragRespuesta fr WHERE fr.proyecto.id = :proyectoId")
    void deleteByProyectoId(@Param("proyectoId") String proyectoId);
}

