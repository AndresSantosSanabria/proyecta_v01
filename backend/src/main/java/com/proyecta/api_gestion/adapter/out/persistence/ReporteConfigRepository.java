package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.ReporteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.ReporteConfigRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
@Repository
public interface ReporteConfigRepository extends JpaRepository<ReporteConfig, String>, ReporteConfigRepositoryPort {
    @Override
    default PageResult<ReporteConfig> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    List<ReporteConfig> findAllByActivoTrueOrderByOrdenAsc();
}

