package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.DocumentoInterno;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoInternoRepository extends JpaRepository<DocumentoInterno, Long> {

    boolean existsByCodigo(String codigo);

    Optional<DocumentoInterno> findByCodigo(String codigo);

    @Query("""
            SELECT d FROM DocumentoInterno d
            WHERE (:nombrePattern IS NULL OR LOWER(d.nombre) LIKE :nombrePattern ESCAPE '\\')
              AND (:anio IS NULL OR YEAR(d.fechaCreacion) = :anio)
              AND (:descripcionPattern IS NULL OR LOWER(d.descripcion) LIKE :descripcionPattern ESCAPE '\\')
            ORDER BY d.fechaCreacion DESC, d.creadoEn DESC
            """)
    List<DocumentoInterno> buscarConFiltros(
            @Param("nombrePattern") String nombrePattern,
            @Param("anio") Integer anio,
            @Param("descripcionPattern") String descripcionPattern,
            Pageable pageable);

    @Query("""
            SELECT COUNT(d) FROM DocumentoInterno d
            WHERE (:nombrePattern IS NULL OR LOWER(d.nombre) LIKE :nombrePattern ESCAPE '\\')
              AND (:anio IS NULL OR YEAR(d.fechaCreacion) = :anio)
              AND (:descripcionPattern IS NULL OR LOWER(d.descripcion) LIKE :descripcionPattern ESCAPE '\\')
            """)
    long contarConFiltros(
            @Param("nombrePattern") String nombrePattern,
            @Param("anio") Integer anio,
            @Param("descripcionPattern") String descripcionPattern);
}
