package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecta.api_gestion.application.port.out.persistence.DocumentoAuditoriaRepositoryPort;
public interface DocumentoAuditoriaRepository extends JpaRepository<DocumentoAuditoria, Long>, DocumentoAuditoriaRepositoryPort {
}

