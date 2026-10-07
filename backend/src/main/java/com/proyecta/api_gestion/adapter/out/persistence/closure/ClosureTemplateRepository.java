package com.proyecta.api_gestion.adapter.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureTemplateRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
@Repository
public interface ClosureTemplateRepository extends JpaRepository<ClosureTemplate, Long>, ClosureTemplateRepositoryPort {
    @Override
    default PageResult<ClosureTemplate> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }


    Optional<ClosureTemplate> findByActivoTrue();

    @Modifying
    @Transactional
    @Query("UPDATE ClosureTemplate t SET t.activo = false WHERE t.activo = true")
    void deactivateAll();
}
