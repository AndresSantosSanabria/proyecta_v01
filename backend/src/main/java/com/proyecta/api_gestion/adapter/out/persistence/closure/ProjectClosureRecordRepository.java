package com.proyecta.api_gestion.adapter.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ProjectClosureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.closure.ProjectClosureRecordRepositoryPort;
@Repository
public interface ProjectClosureRecordRepository extends JpaRepository<ProjectClosureRecord, Long>, ProjectClosureRecordRepositoryPort {
    Optional<ProjectClosureRecord> findByProyectoId(String proyectoId);
}

