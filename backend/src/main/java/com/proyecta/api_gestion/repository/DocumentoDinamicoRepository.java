package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.DocumentoDinamico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoDinamicoRepository extends JpaRepository<DocumentoDinamico, Long> {
    Optional<DocumentoDinamico> findByProyectoIdAndTipoDocumento(String proyectoId, String tipoDocumento);
    List<DocumentoDinamico> findByProyectoIdOrderByFechaCargaDesc(String proyectoId);
}
