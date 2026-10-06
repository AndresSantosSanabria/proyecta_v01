package com.proyecta.api_gestion.adapter.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.AdvanceReportUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportUploadRepositoryPort;
@Repository
public interface AdvanceReportUploadRepository extends JpaRepository<AdvanceReportUpload, Long>, AdvanceReportUploadRepositoryPort {
    Optional<AdvanceReportUpload> findByProjectIdAndPeriodo(String projectId, String periodo);
    boolean existsByProjectIdAndPeriodo(String projectId, String periodo);
    List<AdvanceReportUpload> findByProjectIdOrderByUploadedAtDesc(String projectId);
}

