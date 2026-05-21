package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.*;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder especializado para proyectos y su estructura jerárquica.
 * 
 * Responsabilidad: Crear proyectos con fases, hitos, entregables y riesgos (SRP).
 * Patrón: Builder pattern para construir estructuras complejas.
 */
@Service
public class ProyectoSeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(ProyectoSeeder.class);
    private final ProyectoRepository proyectoRepository;
    private final RiesgoRepository riesgoRepository;
    private final PatrocinadorSeeder patrocinadorSeeder;

    public ProyectoSeeder(ProyectoRepository proyectoRepository,
                          RiesgoRepository riesgoRepository,
                          PatrocinadorSeeder patrocinadorSeeder) {
        this.proyectoRepository = proyectoRepository;
        this.riesgoRepository = riesgoRepository;
        this.patrocinadorSeeder = patrocinadorSeeder;
    }

    public void seedProyectos() {
        logger.info("Cargando proyectos semilla...");
        
        Patrocinador ticSecretary = patrocinadorSeeder.obtenerPatrocinador("Secretaría de TIC");
        Patrocinador infraDir = patrocinadorSeeder.obtenerPatrocinador("Dirección de Infraestructura");
        
        // Proyectos con retrasos
        crearProyecto(
            "IS-PROY-CUN-001",
            "Modernización del Data Center Principal",
            "Infraestructura",
            "Andrés Santos",
            "andres.santos@cundinamarca.gov.co",
            LocalDate.now().minusMonths(5),
            EstadoProyecto.CON_RETRASOS,
            "20.00",
            true,
            infraDir
        );
        
        crearProyecto(
            "IS-PROY-CUN-002",
            "Sistema de PQRS Ciudadano",
            "Atención al Ciudadano",
            "Carolina Gómez",
            "carolina.gomez@cundinamarca.gov.co",
            LocalDate.now().minusMonths(6),
            EstadoProyecto.CON_RETRASOS,
            "15.00",
            true,
            ticSecretary
        );
        
        crearProyecto(
            "IS-PROY-CUN-003",
            "Migración SAP S/4HANA",
            "Finanzas",
            "Martha Lucía Ríos",
            "martha.rios@cundinamarca.gov.co",
            LocalDate.now().minusMonths(8),
            EstadoProyecto.CON_RETRASOS,
            "35.00",
            true,
            infraDir
        );
        
        // Proyectos activos
        crearProyecto(
            "IS-PROY-CUN-004",
            "Portal Web Gobernación 2.0",
            "Prensa y Comunicaciones",
            "Felipe Rojas",
            "felipe.rojas@cundinamarca.gov.co",
            LocalDate.now().minusMonths(3),
            EstadoProyecto.ACTIVO,
            "72.00",
            false,
            ticSecretary
        );
        
        crearProyecto(
            "IS-PROY-CUN-005",
            "Capacitación en Ciberseguridad 2026",
            "Seguridad de la Información",
            "Roberto Díaz",
            "roberto.diaz@cundinamarca.gov.co",
            LocalDate.now().minusMonths(2),
            EstadoProyecto.ACTIVO,
            "80.00",
            false,
            ticSecretary
        );
        
        crearProyecto(
            "IS-PROY-CUN-006",
            "App Móvil Trámites Ciudadanos",
            "Innovación y Tecnología",
            "Luisa Fernanda Mora",
            "luisa.mora@cundinamarca.gov.co",
            LocalDate.now().minusWeeks(3),
            EstadoProyecto.ACTIVO,
            "5.00",
            false,
            infraDir
        );
        
        // Proyectos cerrados
        crearProyecto(
            "IS-PROY-CUN-007",
            "Infraestructura de Red LAN Sede Central",
            "Infraestructura",
            "Andrés Santos",
            "andres.santos@cundinamarca.gov.co",
            LocalDate.now().minusYears(1),
            EstadoProyecto.CERRADO,
            "100.00",
            false,
            infraDir
        );
        
        crearProyecto(
            "IS-PROY-CUN-015",
            "Portal de Datos Abiertos de Cundinamarca",
            "Innovación",
            "Luisa Fernanda Mora",
            "luisa.mora@cundinamarca.gov.co",
            LocalDate.now().minusMonths(5),
            EstadoProyecto.CERRADO,
            "100.00",
            false,
            ticSecretary
        );
        
        // Proyecto masivo para testing
        crearProyectoMasivo();
        
        logger.info("✓ Proyectos semilla cargados");
    }

    private void crearProyecto(String id, String nombre, String dependencia,
                              String director, String correoDirector,
                              LocalDate fechaInicio, EstadoProyecto estado,
                              String avance, boolean peti,
                              Patrocinador patrocinador) {
        
        if (proyectoRepository.findById(id).isPresent()) {
            return;
        }
        
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setNombre(nombre);
        p.setDependencia(dependencia);
        p.setDirector(director);
        p.setCorreoDirector(correoDirector);
        p.setFechaInicio(fechaInicio);
        p.setEstado(estado);
        p.setAvanceTotal(new BigDecimal(avance));
        p.setPeti(peti);
        p.setPatrocinador(patrocinador);
        
        // Agregar estructura: fase -> hito -> entregable
        agregarEstructuraProyecto(p, fechaInicio);
        
        proyectoRepository.save(p);
        logger.debug("Proyecto creado: {} ({})", id, nombre);
    }

    private void agregarEstructuraProyecto(Proyecto proyecto, LocalDate fechaBase) {
        // Crear fase
        Fase fase = new Fase();
        fase.setNombre("Fase Inicial");
        fase.setDescripcion("Fase inicial del proyecto");
        fase.setPonderacion(new BigDecimal("100.00"));
        fase.setAvanceCalculado(proyecto.getAvanceTotal());
        fase.setProyecto(proyecto);
        
        // Crear hito
        Hito hito = new Hito();
        hito.setNombre("Hito de Control");
        hito.setDescripcion("Hito inicial del proyecto");
        hito.setPonderacion(new BigDecimal("100.00"));
        hito.setAvanceCalculado(proyecto.getAvanceTotal());
        hito.setEstadoRevision("PENDIENTE");
        hito.setFase(fase);
        
        // Crear entregable
        Entregable entregable = new Entregable();
        entregable.setNombre("Entregable Principal");
        entregable.setDescripcion("Entregable principal del proyecto");
        entregable.setPonderacion(new BigDecimal("100.00"));
        entregable.setEstado(EstadoEntregable.PENDIENTE);
        entregable.setFechaLimite(fechaBase.plusMonths(3));
        entregable.setHito(hito);
        
        hito.getEntregables().add(entregable);
        fase.getHitos().add(hito);
        proyecto.getFases().add(fase);
    }

    private void crearProyectoMasivo() {
        if (proyectoRepository.findById("IS-PROY-CUN-008").isPresent()) {
            return;
        }
        
        Proyecto p = new Proyecto();
        p.setId("IS-PROY-CUN-008");
        p.setNombre("Proyecto Masivo de Pruebas de Estrés UI");
        p.setDependencia("Calidad de Software");
        p.setDirector("Usuario de Pruebas");
        p.setCorreoDirector("qa@cundinamarca.gov.co");
        p.setFechaInicio(LocalDate.now().minusMonths(4));
        p.setEstado(EstadoProyecto.ACTIVO);
        p.setAvanceTotal(BigDecimal.ZERO);
        p.setPatrocinador(patrocinadorSeeder.obtenerPatrocinador("Secretaría de TIC"));
        
        LocalDate fechaBase = LocalDate.now().minusMonths(3);
        
        for (int f = 1; f <= 5; f++) {
            Fase fase = new Fase();
            fase.setNombre("Fase de Prueba " + f);
            fase.setPonderacion(new BigDecimal("20.00"));
            fase.setAvanceCalculado(BigDecimal.ZERO);
            fase.setProyecto(p);
            
            for (int h = 1; h <= 3; h++) {
                Hito hito = new Hito();
                hito.setNombre("Hito de Control " + f + "." + h);
                hito.setPonderacion(new BigDecimal("33.33"));
                hito.setAvanceCalculado(BigDecimal.ZERO);
                hito.setEstadoRevision("PENDIENTE");
                hito.setFase(fase);
                
                LocalDate hitoInicio = fechaBase.plusWeeks((long) f * 3).plusWeeks(h);
                
                for (int e = 1; e <= 4; e++) {
                    Entregable entregable = new Entregable();
                    entregable.setNombre("Entregable " + f + "." + h + "." + e);
                    entregable.setPonderacion(new BigDecimal("25.00"));
                    entregable.setEstado(EstadoEntregable.PENDIENTE);
                    entregable.setFechaLimite(hitoInicio.plusDays((long) e * 4 + 2));
                    entregable.setHito(hito);
                    hito.getEntregables().add(entregable);
                }
                
                fase.getHitos().add(hito);
            }
            
            p.getFases().add(fase);
        }
        
        proyectoRepository.save(p);
        logger.debug("Proyecto masivo creado: IS-PROY-CUN-008");
    }
}
