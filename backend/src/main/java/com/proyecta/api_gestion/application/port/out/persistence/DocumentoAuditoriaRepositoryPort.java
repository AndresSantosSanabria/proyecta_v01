package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoAuditoria;

public interface DocumentoAuditoriaRepositoryPort {

    <E extends DocumentoAuditoria> E save(E entity);
}

