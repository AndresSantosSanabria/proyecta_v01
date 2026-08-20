package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.PublicEvidenceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicEvidenceAccessRepository extends JpaRepository<PublicEvidenceAccess, Long> {

    @Query("SELECT pea FROM PublicEvidenceAccess pea JOIN FETCH pea.entregable WHERE pea.token = :token AND pea.activo = true")
    Optional<PublicEvidenceAccess> findByTokenAndActivoTrueWithEntregable(@Param("token") String token);

    Optional<PublicEvidenceAccess> findByTokenAndActivoTrue(String token);

    Optional<PublicEvidenceAccess> findByEntregable_IdAndActivoTrue(Integer entregableId);
}
