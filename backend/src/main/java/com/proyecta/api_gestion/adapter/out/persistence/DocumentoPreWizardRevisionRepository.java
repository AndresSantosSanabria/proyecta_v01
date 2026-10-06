package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoPreWizardRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.DocumentoPreWizardRevisionRepositoryPort;
@Repository
public interface DocumentoPreWizardRevisionRepository extends JpaRepository<DocumentoPreWizardRevision, Long>, DocumentoPreWizardRevisionRepositoryPort {

    Optional<DocumentoPreWizardRevision> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);

    List<DocumentoPreWizardRevision> findByProyectoId(String proyectoId);
}

