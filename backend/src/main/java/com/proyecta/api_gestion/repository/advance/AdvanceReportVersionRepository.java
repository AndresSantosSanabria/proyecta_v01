package com.proyecta.api_gestion.repository.advance;

import com.proyecta.api_gestion.model.advance.AdvanceReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvanceReportVersionRepository extends JpaRepository<AdvanceReportVersion, Long> {

    List<AdvanceReportVersion> findByUploadIdOrderByNumeroVersionDesc(Long uploadId);

    Optional<AdvanceReportVersion> findByUploadIdAndEstado(Long uploadId, String estado);

    Optional<AdvanceReportVersion> findByUploadIdAndNumeroVersion(Long uploadId, Integer numeroVersion);

    @Query("SELECT COALESCE(MAX(v.numeroVersion), 0) FROM AdvanceReportVersion v WHERE v.uploadId = :uploadId")
    int findMaxNumeroVersion(@Param("uploadId") Long uploadId);

    List<AdvanceReportVersion> findByProjectIdAndPeriodoOrderByNumeroVersionDesc(String projectId, String periodo);
}
