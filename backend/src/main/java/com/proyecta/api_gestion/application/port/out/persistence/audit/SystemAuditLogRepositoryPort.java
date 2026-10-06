package com.proyecta.api_gestion.application.port.out.persistence.audit;

import com.proyecta.api_gestion.domain.model.audit.SystemAuditLog;
import java.util.UUID;
import com.proyecta.api_gestion.domain.value.AuditLogFilter;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface SystemAuditLogRepositoryPort {

    PageResult<SystemAuditLog> buscarConFiltros(AuditLogFilter filtros, PageQuery query);


    long countByEliminadoFalse();

    long countByEliminadoFalseAndEstado(String estado);

    java.util.List<Object[]> countTotalByAccion();

    java.util.List<Object[]> countTotalByCodigoEstado();

    <E extends SystemAuditLog> E save(E entity);

    java.util.Optional<SystemAuditLog> findById(UUID id);

    java.util.List<SystemAuditLog> findAll();

    PageResult<SystemAuditLog> findAll(PageQuery query);
}

