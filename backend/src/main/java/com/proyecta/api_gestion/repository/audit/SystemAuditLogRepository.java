package com.proyecta.api_gestion.repository.audit;

import com.proyecta.api_gestion.model.audit.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID>, JpaSpecificationExecutor<SystemAuditLog> {

    @Query("SELECT COUNT(sal) FROM SystemAuditLog sal WHERE sal.eliminado = false")
    long countByEliminadoFalse();

    @Query("SELECT COUNT(sal) FROM SystemAuditLog sal WHERE sal.eliminado = false AND sal.estado = :estado")
    long countByEliminadoFalseAndEstado(@org.springframework.data.repository.query.Param("estado") String estado);

    @Query("SELECT sal.accion AS clave, COUNT(sal) AS total " +
            "FROM SystemAuditLog sal " +
            "WHERE sal.eliminado = false " +
            "GROUP BY sal.accion")
    java.util.List<Object[]> countTotalByAccion();

    @Query("SELECT sal.codigoEstado AS clave, COUNT(sal) AS total " +
            "FROM SystemAuditLog sal " +
            "WHERE sal.eliminado = false " +
            "GROUP BY sal.codigoEstado")
    java.util.List<Object[]> countTotalByCodigoEstado();
}