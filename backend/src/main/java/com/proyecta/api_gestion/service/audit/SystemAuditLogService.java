package com.proyecta.api_gestion.service.audit;

import com.proyecta.api_gestion.dto.audit.AuditLogPageResponse;
import com.proyecta.api_gestion.dto.audit.AuditLogStatsDTO;
import com.proyecta.api_gestion.dto.audit.SystemAuditLogDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.audit.SystemAuditLog;
import com.proyecta.api_gestion.repository.audit.SystemAuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemAuditLogService.class);

    private final SystemAuditLogRepository repository;

    public SystemAuditLogService(SystemAuditLogRepository repository) {
        this.repository = repository;
    }

    @Async
    @Transactional
    public void registrar(SystemAuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            log.error("Error guardando registro de auditoria: {}", ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse listarConFiltros(
            String accion, String estado, String usuarioId, String modulo,
            String metodoHttp, Integer codigoEstado, String search,
            LocalDateTime desde, LocalDateTime hasta,
            int page, int size) {

        Specification<SystemAuditLog> spec = buildFiltros(
                accion, estado, usuarioId, modulo, metodoHttp,
                codigoEstado, search, desde, hasta);

        Page<SystemAuditLog> result = repository.findAll(spec, PageRequest.of(page, size));

        List<SystemAuditLogDTO> entries = result.getContent().stream()
                .map(SystemAuditLogService::toDTO)
                .toList();

        return new AuditLogPageResponse(
                entries, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private Specification<SystemAuditLog> buildFiltros(
            String accion, String estado, String usuarioId, String modulo,
            String metodoHttp, Integer codigoEstado, String search,
            LocalDateTime desde, LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.isFalse(root.get("eliminado")));

            if (accion != null && !accion.isBlank()) {
                predicates.add(cb.equal(root.get("accion"), accion));
            }
            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (usuarioId != null && !usuarioId.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("usuarioId")),
                        cb.lower(cb.literal(usuarioId))));
            }
            if (modulo != null && !modulo.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("modulo")),
                        "%" + modulo.toLowerCase() + "%"));
            }
            if (metodoHttp != null && !metodoHttp.isBlank()) {
                predicates.add(cb.equal(root.get("metodoHttp"), metodoHttp));
            }
            if (codigoEstado != null) {
                predicates.add(cb.equal(root.get("codigoEstado"), codigoEstado));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("detalle")), pattern),
                        cb.like(cb.lower(root.get("modulo")), pattern),
                        cb.like(
                                cb.lower(root.get("codigoEstado").as(String.class)),
                                pattern)));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
            }

            query.orderBy(cb.desc(root.get("fechaCreacion")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public SystemAuditLogDTO obtenerDetalle(UUID id) {
        SystemAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoria no encontrado: " + id));
        return toDTO(entry);
    }

    @Transactional(readOnly = true)
    public AuditLogStatsDTO obtenerEstadisticas() {
        long total = repository.countByEliminadoFalse();
        long exitosos = repository.countByEliminadoFalseAndEstado("SUCCESS");
        long errores = repository.countByEliminadoFalseAndEstado("ERROR");

        log.debug("Estadisticas auditoria - total: {}, exitosos: {}, errores: {}", total, exitosos, errores);

        Map<String, Long> porAccion = new LinkedHashMap<>();
        for (Object[] row : repository.countTotalByAccion()) {
            porAccion.put((String) row[0], (Long) row[1]);
        }

        Map<Integer, Long> porCodigoEstado = new LinkedHashMap<>();
        for (Object[] row : repository.countTotalByCodigoEstado()) {
            porCodigoEstado.put((Integer) row[0], (Long) row[1]);
        }

        return new AuditLogStatsDTO(total, exitosos, errores, porAccion, porCodigoEstado);
    }

    @Transactional
    public void softDelete(UUID id) {
        SystemAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoria no encontrado: " + id));
        entry.setEliminado(true);
        entry.setFechaEliminacion(LocalDateTime.now());
        repository.save(entry);
    }

    @Transactional
    public void restaurar(UUID id) {
        SystemAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoria no encontrado: " + id));
        entry.setEliminado(false);
        entry.setFechaEliminacion(null);
        repository.save(entry);
    }

    private static SystemAuditLogDTO toDTO(SystemAuditLog e) {
        return new SystemAuditLogDTO(
                e.getId(), e.getUsuarioId(), e.getUsuarioNombre(), e.getUsuarioRol(),
                e.getAccion(), e.getModulo(), e.getMetodoHttp(), e.getRecurso(),
                e.getCodigoEstado(), e.getEstado(), e.getDetalle(), e.getTrazaError(),
                e.getIpOrigen(), e.getUserAgent(), e.getRequestBody(),
                e.getEntidadTipo(), e.getEntidadId(), e.getRespuestaBody(),
                e.getDuracionMs(), e.getFechaCreacion());
    }
}