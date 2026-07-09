package com.proyecta.api_gestion.repository.closure;

import com.proyecta.api_gestion.model.closure.ClosureTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ClosureTemplateRepository extends JpaRepository<ClosureTemplate, Long> {

    Optional<ClosureTemplate> findByActivoTrue();

    @Modifying
    @Transactional
    @Query("UPDATE ClosureTemplate t SET t.activo = false WHERE t.activo = true")
    void deactivateAll();
}
