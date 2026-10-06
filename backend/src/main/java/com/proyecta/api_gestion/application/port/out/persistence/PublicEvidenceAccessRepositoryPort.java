package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.PublicEvidenceAccess;
import java.util.Optional;

public interface PublicEvidenceAccessRepositoryPort {

    Optional<PublicEvidenceAccess> findByTokenAndActivoTrueWithEntregable(String token);

    Optional<PublicEvidenceAccess> findByEntregableIdAndActivoTrue(Integer entregableId);

    <E extends PublicEvidenceAccess> E save(E entity);
}

