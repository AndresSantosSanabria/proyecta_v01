package com.proyecta.api_gestion.adapter.out.persistence.audit;

import com.proyecta.api_gestion.domain.model.audit.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import com.proyecta.api_gestion.domain.value.AuditLogFilter;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
import com.proyecta.api_gestion.application.port.out.persistence.audit.SystemAuditLogRepositoryPort;
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID>, JpaSpecificationExecutor<SystemAuditLog>, SystemAuditLogRepositoryPort {
    @Override
    default PageResult<SystemAuditLog> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }


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

    String CAMPO_FECHA_CREACION = "fechaCreacion";

    @Override
    default PageResult<SystemAuditLog> buscarConFiltros(AuditLogFilter filtros, PageQuery query) {
        Specification<SystemAuditLog> spec = buildFiltros(filtros);
        return PageBridge.toResult(findAll(spec, PageBridge.toPageable(query)), query);
    }

    static Specification<SystemAuditLog> buildFiltros(AuditLogFilter filtros) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.isFalse(root.get("eliminado")));

            addFiltroAccionEstado(predicates, cb, root, filtros.accion(), filtros.estado());
            addFiltroUsuarioModulo(predicates, cb, root, filtros.usuarioId(), filtros.modulo());
            addFiltroMetodoCodigo(predicates, cb, root, filtros.metodoHttp(), filtros.codigoEstado());
            addFiltroBusqueda(predicates, cb, root, filtros.search());
            addFiltroRangoFechas(predicates, cb, root, filtros.desde(), filtros.hasta());

            query.orderBy(cb.desc(root.get(CAMPO_FECHA_CREACION)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addFiltroAccionEstado(List<Predicate> predicates, CriteriaBuilder cb,
                                       Root<SystemAuditLog> root, String accion, String estado) {
        if (accion != null && !accion.isBlank()) {
            predicates.add(cb.equal(root.get("accion"), accion));
        }
        if (estado != null && !estado.isBlank()) {
            predicates.add(cb.equal(root.get("estado"), estado));
        }
    }

    private static void addFiltroUsuarioModulo(List<Predicate> predicates, CriteriaBuilder cb,
                                        Root<SystemAuditLog> root, String usuarioId, String modulo) {
        if (usuarioId != null && !usuarioId.isBlank()) {
            String usuarioPattern = "%" + usuarioId.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("usuarioId")), usuarioPattern),
                    cb.like(cb.lower(root.get("usuarioNombre")), usuarioPattern)));
        }
        if (modulo != null && !modulo.isBlank()) {
            predicates.add(cb.like(
                    cb.lower(root.get("modulo")),
                    "%" + modulo.toLowerCase() + "%"));
        }
    }

    private static void addFiltroMetodoCodigo(List<Predicate> predicates, CriteriaBuilder cb,
                                       Root<SystemAuditLog> root, String metodoHttp, Integer codigoEstado) {
        if (metodoHttp != null && !metodoHttp.isBlank()) {
            predicates.add(cb.equal(root.get("metodoHttp"), metodoHttp));
        }
        if (codigoEstado != null) {
            predicates.add(cb.equal(root.get("codigoEstado"), codigoEstado));
        }
    }

    private static void addFiltroBusqueda(List<Predicate> predicates, CriteriaBuilder cb,
                                   Root<SystemAuditLog> root, String search) {
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("detalle")), pattern),
                    cb.like(cb.lower(root.get("modulo")), pattern),
                    cb.like(cb.lower(root.get("recurso")), pattern),
                    cb.like(cb.lower(root.get("usuarioNombre")), pattern),
                    cb.like(cb.lower(root.get("usuarioId")), pattern),
                    cb.like(cb.lower(root.get("accion")), pattern),
                    cb.like(
                            cb.lower(root.get("codigoEstado").as(String.class)),
                            pattern)));
        }
    }

    private static void addFiltroRangoFechas(List<Predicate> predicates, CriteriaBuilder cb,
                                      Root<SystemAuditLog> root, LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(CAMPO_FECHA_CREACION), desde));
        }
        if (hasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(CAMPO_FECHA_CREACION), hasta));
        }
    }

}