package com.proyecta.api_gestion.service.validator.impl;

import com.proyecta.api_gestion.exception.PonderacionInvalidaException;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.service.validator.IPonderacionValidator;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de validación y normalización de ponderaciones.
 * 
 * Reglas de negocio:
 * - La suma de ponderaciones de fases debe ser <= 100%
 * - Si la suma < 100%, se normalizan proporcionalmente
 * - Si la suma > 100%, se lanza excepción (error de configuración)
 * - Cada ponderación debe estar entre 0.01% y 100%
 * 
 * SOLID - SRP: Solo responsable de validar ponderaciones
 * SOLID - OCP: Fácil de extender con nuevas estrategias de normalización
 */
@Service
public class PonderacionValidator implements IPonderacionValidator {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");
    private static final BigDecimal MIN_PONDERACION = new BigDecimal("0.01");
    private static final BigDecimal MAX_PONDERACION = new BigDecimal("100");

    /**
     * Valida que la suma de ponderaciones no supere 100%.
     * Permite sumas menores (para normalización posterior).
     */
    @Override
    public void validarPonderacionesGlobales(List<Fase> fases, String proyectoId) {
        BigDecimal suma = calcularSumaPonderaciones(fases);
        
        // Si suma > 100% + tolerancia, es error de configuración
        if (suma.compareTo(CIEN.add(TOLERANCIA)) > 0) {
            throw new PonderacionInvalidaException(
                    String.format(
                            "Las ponderaciones del proyecto %s suman %.2f%%, excediendo el máximo permitido de 100%%",
                            proyectoId, suma
                    ),
                    suma,
                    proyectoId
            );
        }
    }

    /**
     * Normaliza ponderaciones si la suma < 100%.
     * Redistribuye proporcionalmente para que sumen exactamente 100%.
     * 
     * Algoritmo:
     * 1. Calcula suma actual
     * 2. Si suma = 100%, retorna sin cambios
     * 3. Si suma < 100%, calcula factor de escala: 100 / suma
     * 4. Multiplica cada ponderación por el factor
     */
    @Override
    public List<Fase> normalizarPonderaciones(List<Fase> fases) {
        if (fases == null || fases.isEmpty()) {
            return fases;
        }

        BigDecimal sumaActual = calcularSumaPonderaciones(fases);

        // Si ya suma 100%, no hay nada que normalizar
        if (sumaActual.compareTo(CIEN) == 0) {
            return fases;
        }

        // Si suma es 0, no se puede normalizar
        if (sumaActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No se pueden normalizar fases sin ponderaciones");
        }

        // Factor de escala: 100 / sumaActual
        BigDecimal factorEscala = CIEN.divide(sumaActual, 10, RoundingMode.HALF_UP);

        // Aplicar factor a cada fase
        List<Fase> fasesNormalizadas = fases.stream()
                .peek(fase -> {
                    BigDecimal nuevaPonderacion = fase.getPonderacion()
                            .multiply(factorEscala)
                            .setScale(2, RoundingMode.HALF_UP);
                    fase.setPonderacion(nuevaPonderacion);
                })
                .collect(Collectors.toList());

        // Ajuste de redondeo: si la suma aún no es exacto 100, ajustar la última fase
        BigDecimal sumaFinal = calcularSumaPonderaciones(fasesNormalizadas);
        if (sumaFinal.compareTo(CIEN) != 0) {
            BigDecimal diferencia = CIEN.subtract(sumaFinal);
            if (!fasesNormalizadas.isEmpty()) {
                Fase ultimaFase = fasesNormalizadas.get(fasesNormalizadas.size() - 1);
                ultimaFase.setPonderacion(
                        ultimaFase.getPonderacion().add(diferencia).setScale(2, RoundingMode.HALF_UP)
                );
            }
        }

        return fasesNormalizadas;
    }

    /**
     * Calcula la suma de ponderaciones de un conjunto de fases.
     */
    @Override
    public BigDecimal calcularSumaPonderaciones(List<Fase> fases) {
        if (fases == null || fases.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return fases.stream()
                .map(Fase::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valida que una ponderación individual esté dentro del rango permitido.
     */
    @Override
    public void validarPonderacionIndividual(BigDecimal ponderacion, BigDecimal min, BigDecimal max) {
        if (ponderacion == null) {
            throw new IllegalArgumentException("La ponderación no puede ser nula");
        }

        if (ponderacion.compareTo(min) < 0) {
            throw new IllegalArgumentException(
                    String.format("La ponderación %.2f%% es menor que el mínimo permitido %.2f%%",
                            ponderacion, min)
            );
        }

        if (ponderacion.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    String.format("La ponderación %.2f%% supera el máximo permitido %.2f%%",
                            ponderacion, max)
            );
        }
    }
}
