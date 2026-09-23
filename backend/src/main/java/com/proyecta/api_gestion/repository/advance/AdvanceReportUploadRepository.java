package com.proyecta.api_gestion.repository.advance;

import com.proyecta.api_gestion.model.advance.AdvanceReportUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvanceReportUploadRepository extends JpaRepository<AdvanceReportUpload, Long> {
    Optional<AdvanceReportUpload> findByProjectIdAndPeriodo(String projectId, String periodo);
    boolean existsByProjectIdAndPeriodo(String projectId, String periodo);
}
