package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.PonderacionInvalidaException;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.audit.FaseAuditoriaService;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.validator.IPonderacionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de gestión integral de ponderaciones y recálculo de avances.
 * 
 * Responsabilidades:
 * - Gestionar ciclo completo de operaciones sobre ponderaciones
 * - Coordinar entre auditoría, validación y recálculo de avances
 * - Proporcionar operaciones para administración
 * 
 * SOLID - SRP: Coordinar orquestación sin duplicar responsabilidades
 * SOLID - OCP: Extendible para nuevos tipos de auditoría
 */
@Service
public class PonderacionGestionService {

    private final FaseAuditoriaService faseAuditoriaService;
    private final IPonderacionValidator ponderacionValidator;
    private final FaseRepository faseRepository;
    private final ProyectoRepository proyectoRepository;
    private final IProgressCalculator progressCalculator;

    public PonderacionGestionService(
            FaseAuditoriaService faseAuditoriaService,
            IPonderacionValidator ponderacionValidator,
            FaseRepository faseRepository,
            ProyectoRepository proyectoRepository,
            IProgressCalculator progressCalculator) {
        this.faseAuditoriaService = faseAuditoriaService;
        this.ponderacionValidator = ponderacionValidator;
        this.faseRepository = faseRepository;
        this.proyectoRepository = proyectoRepository;
        this.progressCalculator = progressCalculator;
    }

    /**
     * Realiza auditoría completa de consistencia de ponderaciones.
     * Retorna reporte detallado.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> auditarConsistencia(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);
        BigDecimal suma = ponderacionValidator.calcularSumaPonderaciones(fases);

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("proyecto_id", proyectoId);
        reporte.put("proyecto_nombre", proyecto.getNombre());
        reporte.put("cantidad_fases", fases.size());
        reporte.put("suma_ponderaciones", suma);
        reporte.put("es_consistente", suma.compareTo(new BigDecimal("100")) == 0);
        reporte.put("estado", determinarEstado(suma));
        reporte.put("necesita_normalizacion", suma.compareTo(BigDecimal.ZERO) > 0 
                && suma.compareTo(new BigDecimal("100")) < 0);
        
        if (!fases.isEmpty()) {
            reporte.put("fases", fases.stream()
                    .map(fase -> Map.of(
                            "id", fase.getId(),
                            "nombre", fase.getNombre(),
                            "ponderacion", fase.getPonderacion()
                    ))
                    .toList());
        }

        return reporte;
    }

    /**
     * Normaliza automáticamente las ponderaciones de un proyecto.
     * Retorna reporte de cambios realizados.
     */
    @Transactional
    public Map<String, Object> normalizarProyecto(String proyectoId) {
        boolean seNormalizó = faseAuditoriaService.normalizarSiNecesario(proyectoId);

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("proyecto_id", proyectoId);
        reporte.put("se_normalizó", seNormalizó);

        if (seNormalizó) {
            // Recalcular avances después de normalizar
            progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);
            reporte.put("mensaje", "Ponderaciones normalizadas y avances recalculados exitosamente");
            reporte.put("estado_nuevo", auditarConsistencia(proyectoId));
        } else {
            reporte.put("mensaje", "El proyecto ya tiene ponderaciones consistentes");
            reporte.put("estado_actual", auditarConsistencia(proyectoId));
        }

        return reporte;
    }

    /**
     * Normaliza automáticamente todos los proyectos inconsistentes.
     * Retorna reporte global de cambios.
     */
    @Transactional
    public Map<String, Object> normalizarTodosLosProyectos() {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        Map<String, Object> reporteGlobal = new HashMap<>();

        int totalProyectos = proyectos.size();
        int normalizados = 0;
        int consistentes = 0;
        int conError = 0;
        Map<String, Map<String, Object>> detalles = new HashMap<>();

        for (Proyecto proyecto : proyectos) {
            List<Fase> fases = faseRepository.findByProyectoId(proyecto.getId());
            BigDecimal suma = ponderacionValidator.calcularSumaPonderaciones(fases);

            if (suma.compareTo(new BigDecimal("100")) == 0) {
                consistentes++;
            } else if (suma.compareTo(new BigDecimal("100")) > 0) {
                conError++;
                detalles.put(proyecto.getId(), Map.of(
                        "nombre", proyecto.getNombre(),
                        "estado", "ERROR: suma > 100%",
                        "suma", suma
                ));
            } else if (suma.compareTo(BigDecimal.ZERO) > 0) {
                boolean seNormalizó = faseAuditoriaService.normalizarSiNecesario(proyecto.getId());
                if (seNormalizó) {
                    normalizados++;
                    progressCalculator.calcularYActualizarAvanceProyecto(proyecto.getId());
                    detalles.put(proyecto.getId(), Map.of(
                            "nombre", proyecto.getNombre(),
                            "estado", "Normalizado",
                            "suma_anterior", suma
                    ));
                }
            }
        }

        reporteGlobal.put("total_proyectos", totalProyectos);
        reporteGlobal.put("consistentes", consistentes);
        reporteGlobal.put("normalizados", normalizados);
        reporteGlobal.put("con_error", conError);
        reporteGlobal.put("detalles", detalles);
        reporteGlobal.put("timestamp", System.currentTimeMillis());

        return reporteGlobal;
    }

    /**
     * Recalcula avances de un proyecto después de cambios en ponderaciones.
     */
    @Transactional
    public Map<String, Object> recalcularAvances(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        BigDecimal avanceAnterior = proyecto.getAvanceTotal();
        
        // Recalcular
        BigDecimal avanceNuevo = progressCalculator.calcularYActualizarAvanceProyecto(proyectoId);

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("proyecto_id", proyectoId);
        reporte.put("proyecto_nombre", proyecto.getNombre());
        reporte.put("avance_anterior", avanceAnterior);
        reporte.put("avance_nuevo", avanceNuevo);
        reporte.put("cambio", avanceNuevo.subtract(avanceAnterior));
        reporte.put("se_modificó", !avanceAnterior.equals(avanceNuevo));

        return reporte;
    }

    private String determinarEstado(BigDecimal suma) {
        if (suma.compareTo(new BigDecimal("100")) == 0) {
            return "✓ CONSISTENTE";
        } else if (suma.compareTo(new BigDecimal("100")) > 0) {
            return "✗ ERROR: SUMA EXCEDIDA";
        } else if (suma.compareTo(BigDecimal.ZERO) > 0) {
            return "⚠ INCOMPLETA: Necesita normalización";
        } else {
            return "⚠ SIN FASES";
        }
    }
}
