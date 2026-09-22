package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.DocumentoPreWizardRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoPreWizardRevisionRepository extends JpaRepository<DocumentoPreWizardRevision, Long> {

    Optional<DocumentoPreWizardRevision> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);

    List<DocumentoPreWizardRevision> findByProyectoId(String proyectoId);
}
