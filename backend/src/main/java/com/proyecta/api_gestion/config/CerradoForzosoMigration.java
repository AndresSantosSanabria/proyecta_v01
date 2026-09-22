package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migra proyectos cerrados forzosamente al nuevo estado CERRADO_FORZOSO.
 * Ejecuta una sola vez al arrancar la aplicación.
 */
@Component
public class CerradoForzosoMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CerradoForzosoMigration.class);

    private final ProyectoRepository proyectoRepository;

    public CerradoForzosoMigration(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        int migrated = 0;

        for (Proyecto p : proyectos) {
            if (Boolean.TRUE.equals(p.getCierreForzoso()) && EstadoProyecto.CERRADO.equals(p.getEstado())) {
                p.setEstado(EstadoProyecto.CERRADO_FORZOSO);
                p.setEstadoConfig(null);
                proyectoRepository.save(p);
                migrated++;
                log.info("Migrado proyecto {} ({}) a CERRADO_FORZOSO", p.getId(), p.getNombre());
            }
        }

        if (migrated > 0) {
            log.info("✓ Migración CERRADO_FORZOSO completada: {} proyecto(s) actualizado(s)", migrated);
        }
    }
}
