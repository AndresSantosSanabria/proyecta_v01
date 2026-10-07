package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.FuragRespuesta;
import java.util.List;

public interface FuragRespuestaRepositoryPort {

    long countByProyectoIdAndObligatoriaTrue(String proyectoId);

    long countByProyectoIdAndObligatoriaTrueAndRespuestaIsNotNull(String proyectoId);

    List<FuragRespuesta> findByProyectoIdOrderByCodigoPreguntaAsc(String proyectoId);

    List<FuragRespuesta> findByProyectoIdIn(java.util.Collection<String> proyectoIds);

    void deleteByProyectoId(String proyectoId);

    <E extends FuragRespuesta> java.util.List<E> saveAll(Iterable<E> entities);
}

