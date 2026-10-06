package com.proyecta.api_gestion.application.port.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.AdvanceReportUpload;
import java.util.List;
import java.util.Optional;

public interface AdvanceReportUploadRepositoryPort {

    Optional<AdvanceReportUpload> findByProjectIdAndPeriodo(String projectId, String periodo);

    boolean existsByProjectIdAndPeriodo(String projectId, String periodo);

    List<AdvanceReportUpload> findByProjectIdOrderByUploadedAtDesc(String projectId);

    <E extends AdvanceReportUpload> E save(E entity);
}

