package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoPreWizardRevision;
import java.util.List;
import java.util.Optional;

public interface DocumentoPreWizardRevisionRepositoryPort {

    Optional<DocumentoPreWizardRevision> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);

    List<DocumentoPreWizardRevision> findByProyectoId(String proyectoId);

    <E extends DocumentoPreWizardRevision> E save(E entity);
}

