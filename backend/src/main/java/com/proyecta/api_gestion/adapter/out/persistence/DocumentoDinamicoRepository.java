package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.DocumentoDinamico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.DocumentoDinamicoRepositoryPort;
@Repository
public interface DocumentoDinamicoRepository extends JpaRepository<DocumentoDinamico, Long>, DocumentoDinamicoRepositoryPort {
    Optional<DocumentoDinamico> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);
    List<DocumentoDinamico> findByProyectoIdOrderByFechaCargaDesc(String proyectoId);
}

