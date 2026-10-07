package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.ReporteConfig;
import java.util.List;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface ReporteConfigRepositoryPort {

    List<ReporteConfig> findAllByActivoTrueOrderByOrdenAsc();

    <E extends ReporteConfig> E save(E entity);

    java.util.Optional<ReporteConfig> findById(String id);

    java.util.List<ReporteConfig> findAll();

    PageResult<ReporteConfig> findAll(PageQuery query);
}
