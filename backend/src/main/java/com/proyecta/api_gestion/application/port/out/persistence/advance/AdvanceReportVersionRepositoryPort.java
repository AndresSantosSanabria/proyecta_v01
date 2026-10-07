package com.proyecta.api_gestion.application.port.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.AdvanceReportVersion;
import java.util.List;
import java.util.Optional;

public interface AdvanceReportVersionRepositoryPort {

    List<AdvanceReportVersion> findByUploadIdOrderByNumeroVersionDesc(Long uploadId);

    Optional<AdvanceReportVersion> findByUploadIdAndEstado(Long uploadId, String estado);

    Optional<AdvanceReportVersion> findByUploadIdAndNumeroVersion(Long uploadId, Integer numeroVersion);

    int findMaxNumeroVersion(Long uploadId);

    List<AdvanceReportVersion> findByProjectIdAndPeriodoOrderByNumeroVersionDesc(String projectId, String periodo);

    <E extends AdvanceReportVersion> E save(E entity);
}

