package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.ReporteConfig;
import com.proyecta.api_gestion.repository.ReporteConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        
        crearReporteSiNoExiste(
            "ESTADO_PROYECTO",
            "Estado de Proyecto",
            "Resumen ejecutivo del avance y estado actual del proyecto",
            1
        );
        
        crearReporteSiNoExiste(
            "TODOS_LOS_PROYECTOS",
            "Estado de Todos los Proyectos",
            "Lista resumida con KPIs de todos los proyectos activos",
            2
        );
        
        crearReporteSiNoExiste(
            "RIESGOS",
            "Matriz de Riesgos",
            "Visualización de amenazas y matriz de probabilidad-impacto",
            3
        );
        
        crearReporteSiNoExiste(
            "CRONOGRAMA",
            "Cronograma de Proyectos",
            "Diagrama de Gantt y fases del proyecto",
            4
        );
        
        crearReporteSiNoExiste(
            "ENTREGABLES",
            "Estado de Entregables",
            "Detalle de todos los entregables por proyecto",
            5
        );
        
        logger.info("✓ Configuraciones de reportes cargadas");
    }

    private void crearReporteSiNoExiste(String codigo, String nombre, String descripcion, Integer orden) {
        if (!reporteConfigRepository.existsById(codigo)) {
            ReporteConfig reporte = new ReporteConfig(codigo, nombre, descripcion, orden);
            reporteConfigRepository.save(reporte);
            logger.debug("Reporte creado: {}", codigo);
        }
    }
}
