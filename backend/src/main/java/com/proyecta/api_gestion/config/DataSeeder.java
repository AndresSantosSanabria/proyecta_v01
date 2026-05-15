package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.*;
import com.proyecta.api_gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            ProyectoRepository proyectoRepository,
            PatrocinadorRepository patrocinadorRepository,
            UsuarioRepository usuarioRepository,
            SystemParameterRepository systemParameterRepository,
            ReporteConfigRepository reporteConfigRepository,
            RiesgoRepository riesgoRepository
    ) {
        return args -> {
            // Limpiar datos existentes para forzar recarga con el proyecto masivo
            System.out.println(">>> SEEDER: Limpiando datos previos...");
            riesgoRepository.deleteAll();
            proyectoRepository.deleteAll();
            patrocinadorRepository.deleteAll();

            // 1. Usuarios
            if (usuarioRepository.findByCorreo("admin@proyecta.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador Proyecta");
                admin.setCorreo("admin@proyecta.com");
                admin.setContrasenaHash("hash_simulado");
                admin.setRol(Rol.ADMINISTRADOR);
                admin.setActivo(true);
                usuarioRepository.save(admin);
            }

            // 2. Parámetros
            if (!systemParameterRepository.existsById("ventana_vencimiento_dias")) {
                systemParameterRepository.save(new SystemParameter("ventana_vencimiento_dias", "8", "Días ventana de vencimiento"));
            }

            // 3. Reportes Config
            if (!reporteConfigRepository.existsById("ESTADO_PROYECTO")) {
                reporteConfigRepository.save(new ReporteConfig("ESTADO_PROYECTO", "Estado de Proyecto", "Resumen ejecutivo del avance.", 1));
                reporteConfigRepository.save(new ReporteConfig("TODOS_LOS_PROYECTOS", "Estado de todos los proyectos", "Lista resumida.", 2));
                reporteConfigRepository.save(new ReporteConfig("RIESGOS", "Matriz de Riesgos", "Visualización de amenazas.", 3));
            }

            // 4. Patrocinadores
            Patrocinador p1 = new Patrocinador();
            p1.setNombre("Secretaría de TIC");
            p1.setCargo("Secretario");
            p1.setDependencia("Despacho");
            p1.setEntidad("Gobernación de Cundinamarca");
            p1 = patrocinadorRepository.save(p1);

            Patrocinador p2 = new Patrocinador();
            p2.setNombre("Dirección de Infraestructura");
            p2.setCargo("Director TIC");
            p2.setDependencia("TIC");
            p2.setEntidad("Gobernación de Cundinamarca");
            p2 = patrocinadorRepository.save(p2);

            // ====================================================
            // HELPER: crea una Fase con un Hito y un Entregable
            // ====================================================

            // ============ PROYECTOS VENCIDOS (3) ================

            // PROY-001: Vencido — entregable 30 días atrasado
            Proyecto p001 = new Proyecto();
            p001.setId("IS-PROY-CUN-001");
            p001.setNombre("Modernización del Data Center Principal");
            p001.setDependencia("Infraestructura");
            p001.setDirector("Andrés Santos");
            p001.setCorreoDirector("andres.santos@cundinamarca.gov.co");
            p001.setFechaInicio(LocalDate.now().minusMonths(5));
            p001.setEstado(EstadoProyecto.CON_RETRASOS);
            p001.setAvanceTotal(new BigDecimal("20.00"));
            p001.setPeti(true);
            p001.setPatrocinador(p2);

            Fase f001 = buildFase("Ejecución", "50.00", "10.00", p001);
            Hito h001 = buildHito("Adquisición de Servidores", "100.00", f001);
            addEntregable("Orden de Compra Aprobada", "100.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().minusDays(30), h001);
            p001.getFases().add(f001);
            proyectoRepository.save(p001);

            Riesgo r001 = buildRiesgo("R001", "Retraso en importación de hardware.", Probabilidad.ALTA, Impacto.ALTO, NivelRiesgo.CRITICO, p001);
            riesgoRepository.save(r001);

            // PROY-002: Vencido — entregable 15 días atrasado
            Proyecto p002 = new Proyecto();
            p002.setId("IS-PROY-CUN-002");
            p002.setNombre("Sistema de PQRS Ciudadano");
            p002.setDependencia("Atención al Ciudadano");
            p002.setDirector("Carolina Gómez");
            p002.setCorreoDirector("carolina.gomez@cundinamarca.gov.co");
            p002.setFechaInicio(LocalDate.now().minusMonths(6));
            p002.setEstado(EstadoProyecto.CON_RETRASOS);
            p002.setAvanceTotal(new BigDecimal("15.00"));
            p002.setPeti(true);
            p002.setPatrocinador(p1);

            Fase f002 = buildFase("Análisis", "100.00", "15.00", p002);
            Hito h002 = buildHito("Documento de Requerimientos", "100.00", f002);
            addEntregable("Acta de Inicio", "50.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().minusDays(15), h002);
            addEntregable("Diagrama de Casos de Uso", "50.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().minusDays(5), h002);
            p002.getFases().add(f002);
            proyectoRepository.save(p002);

            Riesgo r002 = buildRiesgo("R002", "Falta de alineación con usuarios finales.", Probabilidad.MEDIA, Impacto.ALTO, NivelRiesgo.ALTO, p002);
            riesgoRepository.save(r002);

            // PROY-003: Vencido — entregable 7 días atrasado
            Proyecto p003 = new Proyecto();
            p003.setId("IS-PROY-CUN-003");
            p003.setNombre("Migración SAP S/4HANA");
            p003.setDependencia("Finanzas");
            p003.setDirector("Martha Lucía Ríos");
            p003.setCorreoDirector("martha.rios@cundinamarca.gov.co");
            p003.setFechaInicio(LocalDate.now().minusMonths(8));
            p003.setEstado(EstadoProyecto.CON_RETRASOS);
            p003.setAvanceTotal(new BigDecimal("35.00"));
            p003.setPeti(true);
            p003.setPatrocinador(p2);

            Fase f003 = buildFase("Desarrollo", "100.00", "35.00", p003);
            Hito h003 = buildHito("Módulo de Contabilidad", "100.00", f003);
            addEntregable("Pruebas Unitarias Completadas", "100.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().minusDays(7), h003);
            p003.getFases().add(f003);
            proyectoRepository.save(p003);

            Riesgo r003a = buildRiesgo("R003", "Incompatibilidad de módulos legados.", Probabilidad.ALTA, Impacto.ALTO, NivelRiesgo.CRITICO, p003);
            Riesgo r003b = buildRiesgo("R004", "Falta de personal técnico SAP.", Probabilidad.MEDIA, Impacto.ALTO, NivelRiesgo.ALTO, p003);
            riesgoRepository.save(r003a);
            riesgoRepository.save(r003b);

            // ============ PROYECTO PRÓXIMO A VENCER (1) ================

            // PROY-004: Vence en 2 días
            Proyecto p004 = new Proyecto();
            p004.setId("IS-PROY-CUN-004");
            p004.setNombre("Portal Web Gobernación 2.0");
            p004.setDependencia("Prensa y Comunicaciones");
            p004.setDirector("Felipe Rojas");
            p004.setCorreoDirector("felipe.rojas@cundinamarca.gov.co");
            p004.setFechaInicio(LocalDate.now().minusMonths(3));
            p004.setEstado(EstadoProyecto.ACTIVO);
            p004.setAvanceTotal(new BigDecimal("72.00"));
            p004.setPatrocinador(p1);

            Fase f004 = buildFase("Diseño y Desarrollo UI", "100.00", "72.00", p004);
            Hito h004 = buildHito("Maquetas Validadas", "100.00", f004);
            addEntregable("Prototipo Interactivo Aprobado", "60.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().plusDays(2), h004); // Vence en 2 días
            addEntregable("Manual de Usuario", "40.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().plusDays(10), h004);
            p004.getFases().add(f004);
            proyectoRepository.save(p004);

            // ============ PROYECTOS AL DÍA (3) ================

            // PROY-005: Al día — avance 80%
            Proyecto p005 = new Proyecto();
            p005.setId("IS-PROY-CUN-005");
            p005.setNombre("Capacitación en Ciberseguridad 2026");
            p005.setDependencia("Seguridad de la Información");
            p005.setDirector("Roberto Díaz");
            p005.setCorreoDirector("roberto.diaz@cundinamarca.gov.co");
            p005.setFechaInicio(LocalDate.now().minusMonths(2));
            p005.setEstado(EstadoProyecto.ACTIVO);
            p005.setAvanceTotal(new BigDecimal("80.00"));
            p005.setPatrocinador(p1);

            Fase f005 = buildFase("Formación", "100.00", "80.00", p005);
            Hito h005 = buildHito("Talleres Realizados", "100.00", f005);
            addEntregable("Módulo 1 - Fundamentos", "50.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().plusDays(30), h005);
            addEntregable("Módulo 2 - Amenazas", "50.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().plusDays(45), h005);
            p005.getFases().add(f005);
            proyectoRepository.save(p005);

            // PROY-006: Al día — proyecto nuevo, avance 5%
            Proyecto p006 = new Proyecto();
            p006.setId("IS-PROY-CUN-006");
            p006.setNombre("App Móvil Trámites Ciudadanos");
            p006.setDependencia("Innovación y Tecnología");
            p006.setDirector("Luisa Fernanda Mora");
            p006.setCorreoDirector("luisa.mora@cundinamarca.gov.co");
            p006.setFechaInicio(LocalDate.now().minusWeeks(3));
            p006.setEstado(EstadoProyecto.ACTIVO);
            p006.setAvanceTotal(new BigDecimal("5.00"));
            p006.setPatrocinador(p2);

            Fase f006 = buildFase("Planificación", "100.00", "5.00", p006);
            Hito h006 = buildHito("Kickoff del Proyecto", "100.00", f006);
            addEntregable("Acta de Inicio Firmada", "100.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().plusDays(60), h006);
            p006.getFases().add(f006);
            proyectoRepository.save(p006);

            // PROY-007: Al día — proyecto cerrado 100%
            Proyecto p007 = new Proyecto();
            p007.setId("IS-PROY-CUN-007");
            p007.setNombre("Infraestructura de Red LAN Sede Central");
            p007.setDependencia("Infraestructura");
            p007.setDirector("Andrés Santos");
            p007.setCorreoDirector("andres.santos@cundinamarca.gov.co");
            p007.setFechaInicio(LocalDate.now().minusYears(1));
            p007.setEstado(EstadoProyecto.CERRADO);
            p007.setAvanceTotal(new BigDecimal("100.00"));
            p007.setPatrocinador(p2);

            Fase f007 = buildFase("Implementación", "100.00", "100.00", p007);
            Hito h007 = buildHito("Red Instalada y Certificada", "100.00", f007);
            addEntregable("Informe Final de Implementación", "100.00", EstadoEntregable.PENDIENTE,
                    LocalDate.now().minusMonths(2), h007);
            p007.getFases().add(f007);
            proyectoRepository.save(p007);

            // ============ PROY-008: PROYECTO MASIVO DE PRUEBA DE RENDIMIENTO UI ================
            Proyecto p008 = new Proyecto();
            p008.setId("IS-PROY-CUN-008");
            p008.setNombre("Proyecto Masivo de Pruebas de Estrés UI");
            p008.setDependencia("Calidad de Software");
            p008.setDirector("Usuario de Pruebas");
            p008.setCorreoDirector("qa@cundinamarca.gov.co");
            p008.setFechaInicio(LocalDate.now());
            p008.setEstado(EstadoProyecto.ACTIVO);
            p008.setAvanceTotal(new BigDecimal("0.00"));
            p008.setPatrocinador(p1);

            // 5 Fases (20% cada una)
            for (int f = 1; f <= 5; f++) {
                Fase fase = buildFase("Fase de Prueba " + f, "20.00", "0.00", p008);
                
                // 3 Hitos por fase (33.33% cada uno)
                for (int h = 1; h <= 3; h++) {
                    Hito hito = buildHito("Hito de Control " + f + "." + h, "33.33", fase);
                    
                    // 4 Entregables por hito (25% cada uno)
                    for (int e = 1; e <= 4; e++) {
                        // Mezclamos un poco los estados para ver variedad
                        EstadoEntregable estado = (e % 2 == 0) ? EstadoEntregable.PENDIENTE : EstadoEntregable.PENDIENTE;
                        LocalDate fechaLimite = LocalDate.now().plusDays(f * h * e); 
                        
                        addEntregable("Entregable Documental " + f + "." + h + "." + e, "25.00", estado, fechaLimite, hito);
                    }
                }
                p008.getFases().add(fase);
            }
            proyectoRepository.save(p008);

            System.out.println(">>> SEEDER: 8 proyectos de prueba cargados exitosamente (Incluido el masivo).");
        };
    }

    // ============================================================
    // Métodos helper privados
    // ============================================================

    private Fase buildFase(String nombre, String ponderacion, String avance, Proyecto proyecto) {
        Fase f = new Fase();
        f.setNombre(nombre);
        f.setPonderacion(new BigDecimal(ponderacion));
        f.setAvanceCalculado(new BigDecimal(avance));
        f.setProyecto(proyecto);
        return f;
    }

    private Hito buildHito(String nombre, String ponderacion, Fase fase) {
        Hito h = new Hito();
        h.setNombre(nombre);
        h.setPonderacion(new BigDecimal(ponderacion));
        h.setFase(fase);
        fase.getHitos().add(h);
        return h;
    }

    private void addEntregable(String nombre, String ponderacion, EstadoEntregable estado,
                               LocalDate fechaLimite, Hito hito) {
        Entregable e = new Entregable();
        e.setNombre(nombre);
        e.setPonderacion(new BigDecimal(ponderacion));
        e.setEstado(estado);
        e.setFechaLimite(fechaLimite);
        e.setHito(hito);
        hito.getEntregables().add(e);
    }

    private Riesgo buildRiesgo(String codigo, String descripcion, Probabilidad prob,
                               Impacto impacto, NivelRiesgo nivel, Proyecto proyecto) {
        Riesgo r = new Riesgo();
        r.setCodigo(codigo);
        r.setDescripcion(descripcion);
        r.setProbabilidad(prob);
        r.setImpacto(impacto);
        r.setNivel(nivel);
        r.setProyecto(proyecto);
        return r;
    }
}
