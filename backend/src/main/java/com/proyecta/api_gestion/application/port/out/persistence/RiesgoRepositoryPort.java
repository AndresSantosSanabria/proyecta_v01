package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Riesgo;
import java.util.List;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface RiesgoRepositoryPort {

    List<Riesgo> findByProyectoId(String proyectoId);

    long countByProyectoId(String proyectoId);

    long countByProyectoIdAndEstado(String proyectoId, EstadoRiesgo estado);

    <E extends Riesgo> E save(E entity);

    java.util.Optional<Riesgo> findById(Integer id);

    java.util.List<Riesgo> findAll();

    void delete(Riesgo entity);

    PageResult<Riesgo> findAll(PageQuery query);
}
