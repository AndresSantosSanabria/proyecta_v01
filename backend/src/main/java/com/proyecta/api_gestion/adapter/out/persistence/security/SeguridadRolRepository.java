package com.proyecta.api_gestion.adapter.out.persistence.security;

import com.proyecta.api_gestion.domain.model.security.SeguridadRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadRolRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
import com.proyecta.api_gestion.domain.value.SortOrder;
public interface SeguridadRolRepository extends JpaRepository<SeguridadRol, Long>, SeguridadRolRepositoryPort {
    @Override
    default PageResult<SeguridadRol> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    Optional<SeguridadRol> findByCodigoIgnoreCase(String codigo);
    List<SeguridadRol> findAllByActivoTrueOrderByCodigoAsc();

    @Override
    default java.util.List<SeguridadRol> findAll(java.util.List<SortOrder> sorts) {
        return findAll(PageBridge.toSort(sorts));
    }

}
