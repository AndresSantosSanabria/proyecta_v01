package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
@Repository
public interface RiesgoRepository extends JpaRepository<Riesgo, Integer>, RiesgoRepositoryPort {
    @Override
    default PageResult<Riesgo> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    List<Riesgo> findByProyectoId(String proyectoId);

    long countByProyectoId(String proyectoId);

    long countByProyectoIdAndEstado(String proyectoId, EstadoRiesgo estado);
}

