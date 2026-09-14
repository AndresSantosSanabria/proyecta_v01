package com.proyecta.api_gestion.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntregableRetroactivoTest {

    @Test
    void usaLaFechaLimiteComoEntregaEfectivaCuandoEsRetroactivo() {
        Entregable entregable = new Entregable();
        entregable.setFechaLimite(LocalDate.of(2026, 9, 13));
        entregable.setFechaEntregaReal(LocalDate.of(2026, 9, 14));
        entregable.setRetroactivo(true);

        assertEquals(LocalDate.of(2026, 9, 13), entregable.getFechaEntregaEfectiva());
    }

    @Test
    void conservaLaFechaRealCuandoLaEntregaNoEsRetroactiva() {
        Entregable entregable = new Entregable();
        entregable.setFechaLimite(LocalDate.of(2026, 9, 13));
        entregable.setFechaEntregaReal(LocalDate.of(2026, 9, 14));
        entregable.setRetroactivo(false);

        assertEquals(LocalDate.of(2026, 9, 14), entregable.getFechaEntregaEfectiva());
    }
}
