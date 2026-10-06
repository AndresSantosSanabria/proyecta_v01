package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoDinamico;
import java.util.List;
import java.util.Optional;

public interface DocumentoDinamicoRepositoryPort {

    Optional<DocumentoDinamico> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);

    List<DocumentoDinamico> findByProyectoIdOrderByFechaCargaDesc(String proyectoId);
}

