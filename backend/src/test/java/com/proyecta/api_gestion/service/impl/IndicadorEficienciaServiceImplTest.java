package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.IndicadoresEficienciaDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.config.EstadoEntregableConfig;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadorEficienciaServiceImplTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @InjectMocks
    private IndicadorEficienciaServiceImpl service;

    @Test
    void calcular_cuatroProgramadosCuatroATiempo_eficiencia100() {
        Proyecto proyecto = buildProyecto("P1", List.of(
                buildEntregable("E1", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 14), true),
                buildEntregable("E2", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 19), true),
                buildEntregable("E3", LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 25), true),
                buildEntregable("E4", LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 28), true)
        ));
        when(proyectoRepository.findById("P1")).thenReturn(Optional.of(proyecto));

        IndicadoresEficienciaDTO resultado = service.calcular("P1", LocalDate.of(2026, 6, 30));

        assertEquals(4, resultado.programadosAlCorte());
        assertEquals(4, resultado.entregadosAlCorte());
        assertEquals(4, resultado.entregadosATiempo());
        assertEquals(new BigDecimal("100.0000"), resultado.eficacia());
        assertEquals(new BigDecimal("100.0000"), resultado.eficiencia());
        assertEquals(4, resultado.totalEntregables());
    }

    @Test
    void calcular_ceroProgramados_eficienciaCero() {
        Proyecto proyecto = buildProyecto("P2", List.of(
                buildEntregable("E1", LocalDate.of(2026, 12, 31), null, false)
        ));
        when(proyectoRepository.findById("P2")).thenReturn(Optional.of(proyecto));

        IndicadoresEficienciaDTO resultado = service.calcular("P2", LocalDate.of(2026, 6, 30));

        assertEquals(0, resultado.programadosAlCorte());
        assertEquals(0, resultado.entregadosAlCorte());
        assertEquals(0, resultado.entregadosATiempo());
        assertEquals(new BigDecimal("0.0000"), resultado.eficacia());
        assertEquals(new BigDecimal("0.0000"), resultado.eficiencia());
    }

    @Test
    void calcular_tresProgramadosDosATiempo_eficiencia66() {
        Proyecto proyecto = buildProyecto("P3", List.of(
                buildEntregable("E1", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 9), true),
                buildEntregable("E2", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 16), true),
                buildEntregable("E3", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 19), true),
                buildEntregable("E4", LocalDate.of(2026, 9, 1), null, false)
        ));
        when(proyectoRepository.findById("P3")).thenReturn(Optional.of(proyecto));

        IndicadoresEficienciaDTO resultado = service.calcular("P3", LocalDate.of(2026, 6, 30));

        assertEquals(3, resultado.programadosAlCorte());
        assertEquals(3, resultado.entregadosAlCorte());
        assertEquals(2, resultado.entregadosATiempo());
        assertEquals(new BigDecimal("100.0000"), resultado.eficacia());
        assertEquals(new BigDecimal("66.6667"), resultado.eficiencia());
    }

    @Test
    void calcular_proyectoNoExiste_lanzaExcepcion() {
        when(proyectoRepository.findById("NOEXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.calcular("NOEXISTE", LocalDate.of(2026, 6, 30)));
    }

    @Test
    void calcular_entregableSinFechaEntregaReal_noCuentaATiempo() {
        Proyecto proyecto = buildProyecto("P4", List.of(
                buildEntregable("E1", LocalDate.of(2026, 6, 15), null, true),
                buildEntregable("E2", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 18), true)
        ));
        when(proyectoRepository.findById("P4")).thenReturn(Optional.of(proyecto));

        IndicadoresEficienciaDTO resultado = service.calcular("P4", LocalDate.of(2026, 6, 30));

        assertEquals(2, resultado.programadosAlCorte());
        assertEquals(2, resultado.entregadosAlCorte());
        assertEquals(1, resultado.entregadosATiempo());
        assertEquals(new BigDecimal("50.0000"), resultado.eficiencia());
    }

    private Proyecto buildProyecto(String id, List<Entregable> entregables) {
        Hito hito = new Hito();
        hito.setEntregables(entregables);

        Fase fase = new Fase();
        fase.setHitos(List.of(hito));

        Proyecto proyecto = new Proyecto();
        proyecto.setId(id);
        proyecto.setNombre("Proyecto " + id);
        proyecto.setFases(List.of(fase));
        return proyecto;
    }

    private Entregable buildEntregable(String nombre, LocalDate fechaLimite, LocalDate fechaEntregaReal, boolean conforme) {
        Entregable e = new Entregable();
        e.setNombre(nombre);
        e.setFechaLimite(fechaLimite);
        e.setFechaEntregaReal(fechaEntregaReal);
        if (conforme) {
            EstadoEntregableConfig config = new EstadoEntregableConfig();
            config.setEsConforme(true);
            e.setEstadoConfig(config);
        }
        return e;
    }
}
