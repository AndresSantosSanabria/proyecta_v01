package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.PublicEvidenceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicEvidenceAccessRepository extends JpaRepository<PublicEvidenceAccess, Long> {

    Optional<PublicEvidenceAccess> findByTokenAndActivoTrue(String token);

    Optional<PublicEvidenceAccess> findByEntregable_IdAndActivoTrue(Integer entregableId);
}
