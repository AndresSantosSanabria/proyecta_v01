package com.proyecta.api_gestion.repository.closure;

import com.proyecta.api_gestion.model.closure.ProjectClosureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectClosureRecordRepository extends JpaRepository<ProjectClosureRecord, Long> {
    Optional<ProjectClosureRecord> findByProyectoId(String proyectoId);
}
