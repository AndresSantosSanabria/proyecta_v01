package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByProyectoIdOrderByFechaCargaDesc(String proyectoId);

    @Query("SELECT d FROM Documento d WHERE d.proyectoId = :proyectoId AND d.tipoDocumentoConfig.codigo = :codigo")
    Optional<Documento> findByProyectoIdAndTipoDocumentoConfigCodigo(@Param("proyectoId") String proyectoId, @Param("codigo") String codigo);

    boolean existsByNombreAlmacenado(String nombreAlmacenado);
}
