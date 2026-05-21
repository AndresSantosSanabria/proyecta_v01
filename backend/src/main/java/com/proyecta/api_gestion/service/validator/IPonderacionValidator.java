package com.proyecta.api_gestion.service.validator;

import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Proyecto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Contrato para validación y normalización de ponderaciones de fases.
 * Implementa el patrón Strategy para diferentes reglas de validación.
 * 
 * SOLID - ISP: Interfaz específica para responsabilidad de validación
 * SOLID - DIP: Dependencias inyectadas para cambiar estrategias sin tocar código
 */
public interface IPonderacionValidator {

    BigDecimal CIEN = new BigDecimal("100");
    BigDecimal CERO = BigDecimal.ZERO;

    /**
     * Valida que la suma de ponderaciones de fases sea 100%.
     * Lanza PonderacionInvalidaException si supera 100%.
     * 
     * @param fases Lista de fases a validar
     * @param proyectoId ID del proyecto para mensajes de error
     * @throws PonderacionInvalidaException si la suma > 100%
     */
    void validarPonderacionesGlobales(List<Fase> fases, String proyectoId);

    /**
     * Normaliza automáticamente las ponderaciones si no suman 100%.
     * Redistribuye proporcionalmente si la suma < 100%.
     * 
     * @param fases Lista de fases a normalizar
     * @return Lista de fases con ponderaciones normalizadas
     */
    List<Fase> normalizarPonderaciones(List<Fase> fases);

    /**
     * Calcula la suma actual de ponderaciones.
     * 
     * @param fases Lista de fases
     * @return BigDecimal con la suma de ponderaciones
     */
    BigDecimal calcularSumaPonderaciones(List<Fase> fases);

    /**
     * Valida una ponderación individual antes de asignarla a una fase.
     * 
     * @param ponderacion Valor a validar
     * @param min Valor mínimo permitido
     * @param max Valor máximo permitido
     * @throws IllegalArgumentException si no está en rango
     */
    void validarPonderacionIndividual(BigDecimal ponderacion, BigDecimal min, BigDecimal max);

    /**
     * Calcula la ponderación efectiva de un elemento normalizando
     * contra la suma total del grupo. Si el grupo suma 100%, retorna
     * la ponderación original. Si suma más o menos, normaliza.
     *
     * Ejemplo: fase con ponderación 50 en un proyecto cuya suma total
     * de ponderaciones de fases es 50 → retorna 100 (la fase es el 100%
     * del proyecto).
     *
     * @param ponderacion Ponderación individual del elemento
     * @param sumaTotalGrupo Suma de todas las ponderaciones del grupo
     * @return Ponderación efectiva normalizada
     */
    default BigDecimal calcularPonderacionEfectiva(BigDecimal ponderacion, BigDecimal sumaTotalGrupo) {
        if (ponderacion == null || sumaTotalGrupo == null || sumaTotalGrupo.compareTo(CERO) <= 0) {
            return CERO;
        }
        if (sumaTotalGrupo.compareTo(CIEN) == 0) {
            return ponderacion;
        }
        return ponderacion.multiply(CIEN).divide(sumaTotalGrupo, 10, RoundingMode.HALF_UP);
    }

}
