package com.proyecta.api_gestion.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El badge del listado de proyectos distingue tres estados de PETI:
 * "Sin definir" cuando la informacion nunca fue completada por el director,
 * y "Si"/"No" cuando la informacion ya existe. Este predicado es el que decide
 * esa distincion, por lo que se cubre aqui para que no regrese el bug del
 * "No" incorrecto en proyectos a medias.
 */
class ProyectoPetiDefinidoTest {

    @Test
    void noDefinidoCuandoElDirectorTodaviaNoCompletaLaInformacion() {
        Proyecto proyecto = new Proyecto();
        proyecto.setRequiereCompletitudDirector(true);
        proyecto.setCompletadoPorDirectorAt(null);
        proyecto.setPeti(false);

        assertFalse(proyecto.esPetiDefinido());
    }

    @Test
    void definidoCuandoElDirectorCompletoLaInformacionAunqueNoSeaPeti() {
        Proyecto proyecto = new Proyecto();
        proyecto.setRequiereCompletitudDirector(true);
        proyecto.setCompletadoPorDirectorAt(LocalDateTime.of(2026, 10, 13, 15, 19));
        proyecto.setPeti(false);

        assertTrue(proyecto.esPetiDefinido());
    }

    @Test
    void definidoParaProyectosQueNoRequierenCompletitudDeDirector() {
        Proyecto proyecto = new Proyecto();
        proyecto.setRequiereCompletitudDirector(false);
        proyecto.setCompletadoPorDirectorAt(null);
        proyecto.setPeti(true);

        assertTrue(proyecto.esPetiDefinido());
    }
}
