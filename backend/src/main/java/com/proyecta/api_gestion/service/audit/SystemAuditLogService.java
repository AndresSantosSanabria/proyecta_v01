package com.proyecta.api_gestion.service.audit;

import com.proyecta.api_gestion.dto.audit.AuditLogPageResponse;
import com.proyecta.api_gestion.dto.audit.AuditLogStatsDTO;
import com.proyecta.api_gestion.dto.audit.SystemAuditLogDTO;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.value.AuditLogFilter;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.domain.model.audit.SystemAuditLog;
import com.proyecta.api_gestion.application.port.out.persistence.audit.SystemAuditLogRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemAuditLogService.class);
        private static final String MSG_REGISTRO_AUDITORIA_NO_ENCONTRADO = "Registro de auditoria no encontrado: ";

    private final SystemAuditLogRepositoryPort repository;

    public SystemAuditLogService(SystemAuditLogRepositoryPort repository) {
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
    public AuditLogPageResponse listarConFiltros(AuditLogFilter filtros, int page, int size) {

        PageResult<SystemAuditLog> result = repository.buscarConFiltros(filtros, PageQuery.of(page, size));

        List<SystemAuditLogDTO> entries = result.content().stream()
                .map(SystemAuditLogService::toDTO)
                .toList();

        return new AuditLogPageResponse(
                entries, result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }

    @Transactional(readOnly = true)
    public SystemAuditLogDTO obtenerDetalle(UUID id) {
        SystemAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_REGISTRO_AUDITORIA_NO_ENCONTRADO + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_REGISTRO_AUDITORIA_NO_ENCONTRADO + id));
        entry.setEliminado(true);
        entry.setFechaEliminacion(LocalDateTime.now(ZoneId.systemDefault()));
        repository.save(entry);
    }

    @Transactional
    public void restaurar(UUID id) {
        SystemAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_REGISTRO_AUDITORIA_NO_ENCONTRADO + id));
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
