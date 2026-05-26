package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.service.impl.PonderacionGestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controlador administrativo para auditoría y gestión de ponderaciones.
 * 
 * Endpoints:
 * - GET /admin/ponderaciones/auditoria/{proyectoId} - Auditar proyecto específico
 * - POST /admin/ponderaciones/normalizar/{proyectoId} - Normalizar proyecto específico
 * - POST /admin/ponderaciones/normalizar-todos - Normalizar todos los proyectos
 * - POST /admin/ponderaciones/recalcular/{proyectoId} - Recalcular avances
 * 
 * Acceso: Solo administradores
 */
@RestController
@RequestMapping("/api/v1/admin/ponderaciones")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication) and @localUserAuthorization.hasAnyRole(authentication, 'ADMINISTRADOR')")
public class PonderacionAdministracionController {

    private final PonderacionGestionService ponderacionGestionService;

    public PonderacionAdministracionController(PonderacionGestionService ponderacionGestionService) {
        this.ponderacionGestionService = ponderacionGestionService;
    }

    /**
     * Audita la consistencia de ponderaciones de un proyecto.
     * 
     * @param proyectoId ID del proyecto
     * @return Reporte de auditoría
     */
    @GetMapping("/auditoria/{proyectoId}")
    public ResponseEntity<Map<String, Object>> auditarConsistencia(@PathVariable String proyectoId) {
        Map<String, Object> reporte = ponderacionGestionService.auditarConsistencia(proyectoId);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Normaliza automáticamente las ponderaciones de un proyecto.
     * Si suman < 100%, redistribuye proporcionalmente.
     * 
     * @param proyectoId ID del proyecto
     * @return Reporte de cambios realizados
     */
    @PostMapping("/normalizar/{proyectoId}")
    public ResponseEntity<Map<String, Object>> normalizarProyecto(@PathVariable String proyectoId) {
        Map<String, Object> reporte = ponderacionGestionService.normalizarProyecto(proyectoId);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Normaliza automáticamente todos los proyectos con ponderaciones inconsistentes.
     * Retorna reporte global de cambios.
     * 
     * @return Reporte global de normalización
     */
    @PostMapping("/normalizar-todos")
    public ResponseEntity<Map<String, Object>> normalizarTodosLosProyectos() {
        Map<String, Object> reporte = ponderacionGestionService.normalizarTodosLosProyectos();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Recalcula los avances del proyecto basado en ponderaciones actuales.
     * Útil después de cambios significativos en estructura.
     * 
     * @param proyectoId ID del proyecto
     * @return Reporte de recálculo
     */
    @PostMapping("/recalcular/{proyectoId}")
    public ResponseEntity<Map<String, Object>> recalcularAvances(@PathVariable String proyectoId) {
        Map<String, Object> reporte = ponderacionGestionService.recalcularAvances(proyectoId);
        return ResponseEntity.ok(reporte);
    }
}
