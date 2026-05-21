package com.proyecta.api_gestion.service.audit;

import com.proyecta.api_gestion.exception.PonderacionInvalidaException;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.validator.IPonderacionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de auditoría para cambios en fases.
 * 
 * Responsabilidades:
 * - Validar ponderaciones al crear/editar fases
 * - Normalizar automáticamente si es necesario
 * - Registrar cambios en auditoría (futura mejora)
 * - Garantizar consistencia de datos
 * 
 * SOLID - SRP: Solo auditaría de cambios en fases
 * SOLID - DIP: Inyecta validador y repositorios
 * SOLID - OCP: Fácil agregar auditoría, logs, eventos
 */
@Service
public class FaseAuditoriaService {

    private final IPonderacionValidator ponderacionValidator;
    private final FaseRepository faseRepository;
    private final ProyectoRepository proyectoRepository;

    public FaseAuditoriaService(
            IPonderacionValidator ponderacionValidator,
            FaseRepository faseRepository,
            ProyectoRepository proyectoRepository) {
        this.ponderacionValidator = ponderacionValidator;
        this.faseRepository = faseRepository;
        this.proyectoRepository = proyectoRepository;
    }

    /**
     * Audita la creación de una nueva fase.
     * - Valida ponderación individual
     * - Valida que no se supere 100% globalmente
     * - Normaliza si es necesario
     * 
     * @param proyectoId ID del proyecto
     * @param nuevaFase Fase a crear
     * @return Fase auditada y normalizada
     */
    @Transactional
    public Fase auditarCreacionFase(String proyectoId, Fase nuevaFase) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        // Validar ponderación individual
        ponderacionValidator.validarPonderacionIndividual(
                nuevaFase.getPonderacion(),
                new BigDecimal("0.01"),
                new BigDecimal("100")
        );

        // Obtener fases existentes
        List<Fase> fasesExistentes = faseRepository.findByProyectoId(proyecto.getId());

        // Agregar la nueva fase a la lista para validación
        fasesExistentes.add(nuevaFase);

        // Validar que no supere 100%
        ponderacionValidator.validarPonderacionesGlobales(fasesExistentes, proyectoId);

        // Si suma < 100%, normalizar
        BigDecimal sumaActual = ponderacionValidator.calcularSumaPonderaciones(fasesExistentes);
        if (sumaActual.compareTo(new BigDecimal("100")) < 0) {
            fasesExistentes = ponderacionValidator.normalizarPonderaciones(fasesExistentes);
        }

        // Guardar fases normalizadas
        faseRepository.saveAll(fasesExistentes);

        return nuevaFase;
    }

    /**
     * Audita la edición de una fase existente.
     * - Valida nueva ponderación
     * - Valida que no se supere 100% globalmente
     * - Normaliza si es necesario
     * 
     * @param proyectoId ID del proyecto
     * @param faseId ID de la fase a editar
     * @param nuevaPonderacion Nueva ponderación
     * @return Fase actualizada
     */
    @Transactional
    public Fase auditarEdicionFase(String proyectoId, Integer faseId, BigDecimal nuevaPonderacion) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        Fase faseAEditar = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no encontrada: " + faseId));

        // Validar ponderación individual
        ponderacionValidator.validarPonderacionIndividual(
                nuevaPonderacion,
                new BigDecimal("0.01"),
                new BigDecimal("100")
        );

        // Actualizar ponderación
        faseAEditar.setPonderacion(nuevaPonderacion);

        // Obtener todas las fases del proyecto
        List<Fase> todasLasFases = faseRepository.findByProyectoId(proyecto.getId());

        // Validar que no supere 100%
        ponderacionValidator.validarPonderacionesGlobales(todasLasFases, proyectoId);

        // Si suma < 100%, normalizar
        BigDecimal sumaActual = ponderacionValidator.calcularSumaPonderaciones(todasLasFases);
        if (sumaActual.compareTo(new BigDecimal("100")) < 0) {
            todasLasFases = ponderacionValidator.normalizarPonderaciones(todasLasFases);
        }

        // Guardar fases normalizadas
        faseRepository.saveAll(todasLasFases);

        return faseAEditar;
    }

    /**
     * Valida la consistencia general de ponderaciones de un proyecto.
     * Útil para chequeos periódicos o migración de datos.
     * 
     * @param proyectoId ID del proyecto
     * @return true si está consistente, false si necesita normalización
     */
    @Transactional(readOnly = true)
    public boolean validarConsistencia(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyecto.getId());
        
        if (fases.isEmpty()) {
            return true; // Proyecto sin fases es válido
        }

        BigDecimal suma = ponderacionValidator.calcularSumaPonderaciones(fases);
        return suma.compareTo(new BigDecimal("100")) == 0;
    }

    /**
     * Normaliza fases de un proyecto si están inconsistentes.
     * 
     * @param proyectoId ID del proyecto
     * @return true si se normalizaron, false si ya estaban correctas
     */
    @Transactional
    public boolean normalizarSiNecesario(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));

        List<Fase> fases = faseRepository.findByProyectoId(proyecto.getId());
        
        if (fases.isEmpty()) {
            return false;
        }

        BigDecimal sumaActual = ponderacionValidator.calcularSumaPonderaciones(fases);

        // Si está bien, no hacer nada
        if (sumaActual.compareTo(new BigDecimal("100")) == 0) {
            return false;
        }

        // Si supera 100%, lanzar excepción
        if (sumaActual.compareTo(new BigDecimal("100")) > 0) {
            throw new PonderacionInvalidaException(
                    String.format("No se puede normalizar proyecto %s: ponderaciones suman %.2f%%",
                            proyectoId, sumaActual),
                    sumaActual,
                    proyectoId
            );
        }

        // Normalizar
        List<Fase> fasesNormalizadas = ponderacionValidator.normalizarPonderaciones(fases);
        faseRepository.saveAll(fasesNormalizadas);
        return true;
    }
}
