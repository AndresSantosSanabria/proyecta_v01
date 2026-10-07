package com.proyecta.api_gestion.adapter.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.AdvanceReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportVersionRepositoryPort;
@Repository
public interface AdvanceReportVersionRepository extends JpaRepository<AdvanceReportVersion, Long>, AdvanceReportVersionRepositoryPort {

    List<AdvanceReportVersion> findByUploadIdOrderByNumeroVersionDesc(Long uploadId);

    Optional<AdvanceReportVersion> findByUploadIdAndEstado(Long uploadId, String estado);

    Optional<AdvanceReportVersion> findByUploadIdAndNumeroVersion(Long uploadId, Integer numeroVersion);

    @Query("SELECT COALESCE(MAX(v.numeroVersion), 0) FROM AdvanceReportVersion v WHERE v.uploadId = :uploadId")
    int findMaxNumeroVersion(@Param("uploadId") Long uploadId);

    List<AdvanceReportVersion> findByProjectIdAndPeriodoOrderByNumeroVersionDesc(String projectId, String periodo);
}

