package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.PublicEvidenceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.PublicEvidenceAccessRepositoryPort;
@Repository
public interface PublicEvidenceAccessRepository extends JpaRepository<PublicEvidenceAccess, Long>, PublicEvidenceAccessRepositoryPort {

    @Query("SELECT pea FROM PublicEvidenceAccess pea JOIN FETCH pea.entregable WHERE pea.token = :token AND pea.activo = true")
    Optional<PublicEvidenceAccess> findByTokenAndActivoTrueWithEntregable(@Param("token") String token);

    Optional<PublicEvidenceAccess> findByEntregableIdAndActivoTrue(Integer entregableId);
}

