package com.proyecta.api_gestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.application.port.out.persistence.DocumentoVersionRepositoryPort;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.dto.avance.EntregableAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Fija la regla del tablero de seguimiento: el entregable permanece "en plazo"
 * durante todo el dia de su fecha limite y el estado ATRASO se activa a partir
 * del dia siguiente, unicamente si continua pendiente.
 */
class ProjectProgressMetricsServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 11, 18);

    private ProjectProgressMetricsService service;

    @BeforeEach
    void setUp() {
        service = new ProjectProgressMetricsService(new ObjectMapper(),
                mock(DocumentoVersionRepositoryPort.class));
    }

    @Test
    void elDiaDelVencimientoElProyectoSigueEnTiempo() {
        LocalDate corte = VENCIMIENTO;
        ProyectoAvanceResponseDTO avance = construirAvance(corte, null, false);

        EntregableAvanceDTO entregable = entregableDe(avance);
        assertEquals(0, entregable.progresoProgramado().compareTo(BigDecimal.ZERO),
                "el dia del vencimiento el entregable aun no se considera programado/vencido");
        assertEquals("ALERTA", entregable.estado(), "el dia del vencimiento se avisa pero no se marca atraso");
        assertEquals(0, entregable.diasAtraso());

        assertEquals("EN_TIEMPO", hitoDe(avance).estado());
        assertEquals("EN_TIEMPO", avance.fases().get(0).estado());
        assertEquals("EN_TIEMPO", avance.estado());

        assertEquals(0L, avance.entregablesAtrasados());
        assertEquals(1L, avance.proximosAVencer());
        assertEquals(1L, avance.entregablesProgramadosAlCorte(),
                "el indicador programados al corte sigue contando la fecha limite");
    }

    @Test
    void alDiaSiguienteYPendienteElProyectoEntraEnAtraso() {
        LocalDate corte = VENCIMIENTO.plusDays(1);
        ProyectoAvanceResponseDTO avance = construirAvance(corte, null, false);

        EntregableAvanceDTO entregable = entregableDe(avance);
        assertEquals(-1L, entregable.diasAtraso(), "el dia siguiente se registra el primer dia de atraso");
        assertEquals("ATRASO", entregable.estado());

        assertEquals("ATRASO", hitoDe(avance).estado());
        assertEquals("ATRASO", avance.fases().get(0).estado());
        assertEquals("ATRASO", avance.estado());
        assertEquals(1L, avance.entregablesAtrasados());
    }

    @Test
    void alDiaSiguienteYaEntregadoElProyectoNoEntraEnAtraso() {
        LocalDate corte = VENCIMIENTO.plusDays(1);
        ProyectoAvanceResponseDTO avance = construirAvance(corte, VENCIMIENTO.plusDays(1), true);

        EntregableAvanceDTO entregable = entregableDe(avance);
        assertEquals(0, entregable.progresoEjecutado().compareTo(new BigDecimal("100.00")));
        assertEquals(0, entregable.diferencia().compareTo(BigDecimal.ZERO),
                "sin atraso en los niveles superiores cuando el entregable ya fue entregado");

        // El atraso de una entrega tardia se conserva como registro en el entregable.
        assertEquals("ATRASO", entregable.estado());
        assertEquals("EN_TIEMPO", hitoDe(avance).estado());
        assertEquals("EN_TIEMPO", avance.fases().get(0).estado());
        assertEquals("EN_TIEMPO", avance.estado());
        assertEquals(0L, avance.entregablesAtrasados(),
                "el indicador de atrasados solo cuenta entregables pendientes");
    }

    @Test
    void antesDelVencimientoElProyectoSigueEnTiempo() {
        LocalDate corte = VENCIMIENTO.minusDays(9);
        ProyectoAvanceResponseDTO avance = construirAvance(corte, null, false);

        EntregableAvanceDTO entregable = entregableDe(avance);
        assertEquals(0, entregable.progresoProgramado().compareTo(BigDecimal.ZERO));
        assertEquals("PENDIENTE", entregable.estado());
        assertEquals("EN_TIEMPO", avance.estado());
        assertEquals(0L, avance.entregablesAtrasados());
    }

    private ProyectoAvanceResponseDTO construirAvance(LocalDate corte, LocalDate fechaEntrega, boolean conforme) {
        Entregable entregable = new Entregable();
        entregable.setId(1);
        entregable.setNombre("E01");
        entregable.setPonderacion(BigDecimal.valueOf(50));
        entregable.setFechaInicio(INICIO);
        entregable.setFechaLimite(VENCIMIENTO);
        entregable.setFechaEntregaReal(fechaEntrega);
        entregable.setConforme(conforme);

        Hito hito = new Hito();
        hito.setId(1);
        hito.setNombre("H01");
        hito.setPonderacion(BigDecimal.valueOf(100));
        hito.setEntregables(new ArrayList<>(List.of(entregable)));

        Fase fase = new Fase();
        fase.setId(1);
        fase.setNombre("F01");
        fase.setPonderacion(BigDecimal.valueOf(100));
        fase.setHitos(new ArrayList<>(List.of(hito)));

        Proyecto proyecto = new Proyecto();
        proyecto.setId("PROY-CUN-2026-009");
        proyecto.setNombre("Proyecto de prueba");
        proyecto.setFases(new ArrayList<>(List.of(fase)));

        return service.construir(proyecto, corte);
    }

    private EntregableAvanceDTO entregableDe(ProyectoAvanceResponseDTO avance) {
        return hitoDe(avance).entregables().get(0);
    }

    private com.proyecta.api_gestion.dto.avance.HitoAvanceDTO hitoDe(ProyectoAvanceResponseDTO avance) {
        return avance.fases().get(0).hitos().get(0);
    }
}
