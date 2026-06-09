package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.ReporteConfig;
import com.proyecta.api_gestion.repository.ReporteConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Seeder especializado para configuraciones de reportes.
 *
 * Responsabilidad: Inicializar plantillas y configuraciones de reportes (SRP).
 */
@Service
public class ReporteConfigSeeder {

    private static final Logger logger = LoggerFactory.getLogger(ReporteConfigSeeder.class);
    private final ReporteConfigRepository reporteConfigRepository;

    public ReporteConfigSeeder(ReporteConfigRepository reporteConfigRepository) {
        this.reporteConfigRepository = reporteConfigRepository;
    }

    public void seedReporteConfigs() {
        logger.info("Cargando configuraciones de reportes...");

        Map<String, ReporteConfig> deseados = new LinkedHashMap<>();
        deseados.put("ESTADO_PROYECTO", new ReporteConfig(
                "ESTADO_PROYECTO",
                "Estado de Proyecto Especifico",
                "Vista ejecutiva del avance, entregables vencidos y estado del proyecto",
                1
        ));
        deseados.put("TODOS_LOS_PROYECTOS", new ReporteConfig(
                "TODOS_LOS_PROYECTOS",
                "Estado de Todos los Proyectos",
                "Resumen consolidado con KPIs del portafolio activo",
                2
        ));
        deseados.put("RETRASOS_ENTREGA", new ReporteConfig(
                "RETRASOS_ENTREGA",
                "Proyectos con Retrasos en la Fecha de Entrega",
                "Listado ejecutivo de proyectos con entregables fuera de plazo",
                3
        ));
        deseados.put("PLAN_COMUNICACIONES", new ReporteConfig(
                "PLAN_COMUNICACIONES",
                "Plan de Comunicaciones",
                "Detalle del plan de comunicaciones asociado a un proyecto",
                4
        ));
        deseados.put("FURAG", new ReporteConfig(
                "FURAG",
                "Preguntas FURAG",
                "Consolidado institucional de respuestas FURAG por dependencia",
                5
        ));
        deseados.put("VERIFICACION_RIESGOS", new ReporteConfig(
                "VERIFICACION_RIESGOS",
                "Verificacion de Tratamiento a Riesgos",
                "Reporte de riesgos y su estado de tratamiento",
                6
        ));

        Set<String> permitidos = deseados.keySet();
        reporteConfigRepository.findAll().forEach(config -> {
            if (!permitidos.contains(config.getId()) && Boolean.TRUE.equals(config.getActivo())) {
                config.setActivo(false);
                reporteConfigRepository.save(config);
            }
        });

        deseados.values().forEach(this::upsertReporte);

        logger.info("Configuraciones de reportes cargadas");
    }

    private void upsertReporte(ReporteConfig deseado) {
        ReporteConfig reporte = reporteConfigRepository.findById(deseado.getId())
                .map(actual -> {
                    actual.setNombre(deseado.getNombre());
                    actual.setDescripcion(deseado.getDescripcion());
                    actual.setOrden(deseado.getOrden());
                    actual.setActivo(true);
                    return actual;
                })
                .orElseGet(() -> {
                    ReporteConfig nuevo = new ReporteConfig(
                            deseado.getId(),
                            deseado.getNombre(),
                            deseado.getDescripcion(),
                            deseado.getOrden()
                    );
                    nuevo.setActivo(true);
                    return nuevo;
                });

        reporteConfigRepository.save(reporte);
        logger.debug("Reporte configurado: {}", reporte.getId());
    }
}
