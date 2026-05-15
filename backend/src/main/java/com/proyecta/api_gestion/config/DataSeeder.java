package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.*;
import com.proyecta.api_gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

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
            System.out.println(">>> SEEDER: Verificando datos...");

            // 1. Usuario admin (idempotente)
            if (usuarioRepository.findByCorreo("admin@proyecta.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador Proyecta");
                admin.setCorreo("admin@proyecta.com");
                admin.setContrasenaHash("hash_simulado");
                admin.setRol(Rol.ADMINISTRADOR);
                admin.setActivo(true);
                usuarioRepository.save(admin);
                System.out.println(">>> SEEDER: Usuario admin creado.");
            }

            // 2. Parámetros (idempotente)
            if (!systemParameterRepository.existsById("ventana_vencimiento_dias")) {
                systemParameterRepository.save(new SystemParameter("ventana_vencimiento_dias", "8", "Días ventana de vencimiento"));
            }

            // 3. Reportes Config (idempotente)
            if (!reporteConfigRepository.existsById("ESTADO_PROYECTO")) {
                reporteConfigRepository.save(new ReporteConfig("ESTADO_PROYECTO", "Estado de Proyecto", "Resumen ejecutivo del avance.", 1));
                reporteConfigRepository.save(new ReporteConfig("TODOS_LOS_PROYECTOS", "Estado de todos los proyectos", "Lista resumida.", 2));
                reporteConfigRepository.save(new ReporteConfig("RIESGOS", "Matriz de Riesgos", "Visualización de amenazas.", 3));
            }

            // 4. Patrocinadores (idempotente por nombre)
            Patrocinador p1 = patrocinadorRepository.findByNombre("Secretaría de TIC").orElseGet(() -> {
                Patrocinador p = new Patrocinador();
                p.setNombre("Secretaría de TIC");
                p.setCargo("Secretario");
                p.setDependencia("Despacho");
                p.setEntidad("Gobernación de Cundinamarca");
                return patrocinadorRepository.save(p);
            });

            Patrocinador p2 = patrocinadorRepository.findByNombre("Dirección de Infraestructura").orElseGet(() -> {
                Patrocinador p = new Patrocinador();
                p.setNombre("Dirección de Infraestructura");
                p.setCargo("Director TIC");
                p.setDependencia("TIC");
                p.setEntidad("Gobernación de Cundinamarca");
                return patrocinadorRepository.save(p);
            });

            // 5. Proyectos (idempotente - solo inserta si no existe)
            seedProyecto("IS-PROY-CUN-001", "Modernización del Data Center Principal",
                    "Infraestructura", "Andrés Santos", "andres.santos@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(5), EstadoProyecto.CON_RETRASOS, "20.00", true, p2,
                    "Ejecución", "50.00", "10.00",
                    "Adquisición de Servidores", "Orden de Compra Aprobada", EstadoEntregable.PENDIENTE, LocalDate.now().minusDays(30),
                    "R001", "Retraso en importación de hardware.", Probabilidad.ALTA, Impacto.ALTO, NivelRiesgo.CRITICO,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-002", "Sistema de PQRS Ciudadano",
                    "Atención al Ciudadano", "Carolina Gómez", "carolina.gomez@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(6), EstadoProyecto.CON_RETRASOS, "15.00", true, p1,
                    "Análisis", "100.00", "15.00",
                    "Documento de Requerimientos", "Acta de Inicio", EstadoEntregable.PENDIENTE, LocalDate.now().minusDays(15),
                    "R002", "Falta de alineación con usuarios finales.", Probabilidad.MEDIA, Impacto.ALTO, NivelRiesgo.ALTO,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-003", "Migración SAP S/4HANA",
                    "Finanzas", "Martha Lucía Ríos", "martha.rios@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(8), EstadoProyecto.CON_RETRASOS, "35.00", true, p2,
                    "Desarrollo", "100.00", "35.00",
                    "Módulo de Contabilidad", "Pruebas Unitarias Completadas", EstadoEntregable.PENDIENTE, LocalDate.now().minusDays(7),
                    "R003", "Incompatibilidad de módulos legados.", Probabilidad.ALTA, Impacto.ALTO, NivelRiesgo.CRITICO,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-004", "Portal Web Gobernación 2.0",
                    "Prensa y Comunicaciones", "Felipe Rojas", "felipe.rojas@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(3), EstadoProyecto.ACTIVO, "72.00", false, p1,
                    "Diseño y Desarrollo UI", "100.00", "72.00",
                    "Maquetas Validadas", "Prototipo Interactivo Aprobado", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(2),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-005", "Capacitación en Ciberseguridad 2026",
                    "Seguridad de la Información", "Roberto Díaz", "roberto.diaz@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(2), EstadoProyecto.ACTIVO, "80.00", false, p1,
                    "Formación", "100.00", "80.00",
                    "Talleres Realizados", "Módulo 1 - Fundamentos", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(30),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-006", "App Móvil Trámites Ciudadanos",
                    "Innovación y Tecnología", "Luisa Fernanda Mora", "luisa.mora@cundinamarca.gov.co",
                    LocalDate.now().minusWeeks(3), EstadoProyecto.ACTIVO, "5.00", false, p2,
                    "Planificación", "100.00", "5.00",
                    "Kickoff del Proyecto", "Acta de Inicio Firmada", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(60),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-007", "Infraestructura de Red LAN Sede Central",
                    "Infraestructura", "Andrés Santos", "andres.santos@cundinamarca.gov.co",
                    LocalDate.now().minusYears(1), EstadoProyecto.CERRADO, "100.00", false, p2,
                    "Implementación", "100.00", "100.00",
                    "Red Instalada y Certificada", "Informe Final de Implementación", EstadoEntregable.A_CONFORMIDAD, LocalDate.now().minusMonths(2),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-009", "Telemedicina para Centros de Salud Rurales",
                    "Salud", "Elena Vásquez", "elena.vasquez@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(3), EstadoProyecto.ACTIVO, "45.00", false, p1,
                    "Diagnóstico", "100.00", "100.00",
                    "Mapa de Conectividad", "Informe Técnico de Redes", EstadoEntregable.A_CONFORMIDAD, LocalDate.now().minusMonths(2),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-010", "Renovación de Infraestructura Tecnológica Educativa",
                    "Educación", "Jorge Iván Ruiz", "jorge.ruiz@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(7), EstadoProyecto.CON_RETRASOS, "60.00", false, p2,
                    "Distribución", "100.00", "60.00",
                    "Entrega de Laptops", "Lote 1: Sabana Centro", EstadoEntregable.PENDIENTE, LocalDate.now().minusDays(10),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-011", "Sistema de Catastro Multipropósito Regional",
                    "Hacienda", "Sandra Milena Torres", "sandra.torres@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(12), EstadoProyecto.ACTIVO, "88.00", false, p1,
                    "Consolidación de Datos", "100.00", "88.00",
                    "Carga de Predios Rurales", "Base de Datos Validada", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(20),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-012", "Implementación de Firma Digital y Cero Papel",
                    "Secretaría General", "Ricardo Méndez", "ricardo.mendez@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(2), EstadoProyecto.ACTIVO, "15.00", false, p1,
                    "Configuración", "100.00", "15.00",
                    "Certificados Emitidos", "Software de Firma Instalado", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(40),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-013", "Programa Departamental de Gestión de RAEE",
                    "Ambiente", "Claudia Pardo", "claudia.pardo@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(4), EstadoProyecto.ACTIVO, "40.00", false, p2,
                    "Recolección", "100.00", "40.00",
                    "Centros de Acopio Habilitados", "Convenios con Municipios", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(60),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-014", "Fortalecimiento de la Ciberseguridad Institucional",
                    "TIC", "Roberto Díaz", "roberto.diaz@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(1), EstadoProyecto.ACTIVO, "10.00", false, p2,
                    "Auditoría", "100.00", "10.00",
                    "Pentesting Finalizado", "Reporte de Vulnerabilidades", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(15),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-015", "Portal de Datos Abiertos de Cundinamarca",
                    "Innovación", "Luisa Fernanda Mora", "luisa.mora@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(5), EstadoProyecto.CERRADO, "100.00", false, p1,
                    "Cierre", "100.00", "100.00",
                    "Portal en Producción", "Lanzamiento Oficial", EstadoEntregable.A_CONFORMIDAD, LocalDate.now().minusDays(30),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-016", "Modernización del Sistema de Tránsito Departamental",
                    "Movilidad", "Andrés Santos", "andres.santos@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(9), EstadoProyecto.CON_RETRASOS, "55.00", false, p2,
                    "Desarrollo", "100.00", "55.00",
                    "Módulo de Infracciones", "Interfaz de Usuario", EstadoEntregable.PENDIENTE, LocalDate.now().minusDays(5),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-017", "Sistema Integrado de Gestión Documental Electrónica",
                    "Secretaría de Gobierno", "Claudia Pardo", "claudia.pardo@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(6), EstadoProyecto.ACTIVO, "75.00", false, p1,
                    "Migración", "100.00", "75.00",
                    "Digitalización de Archivo Histórico", "Expedientes Indexados", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(30),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-018", "Monitoreo Ambiental Satelital de Cuencas",
                    "Ambiente", "Felipe Rojas", "felipe.rojas@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(2), EstadoProyecto.ACTIVO, "30.00", false, p2,
                    "Configuración", "100.00", "30.00",
                    "Enlace con Satélite", "Pruebas de Transmisión", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(45),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-019", "Plataforma de Gestión Integral del Talento Humano",
                    "Función Pública", "Carolina Gómez", "carolina.gomez@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(4), EstadoProyecto.ACTIVO, "50.00", false, p1,
                    "Módulos", "100.00", "50.00",
                    "Módulo de Nómina", "Carga de Historias Laborales", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(20),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            seedProyecto("IS-PROY-CUN-020", "Plataforma de Turismo Digital 360",
                    "Cultura y Turismo", "Elena Vásquez", "elena.vasquez@cundinamarca.gov.co",
                    LocalDate.now().minusMonths(8), EstadoProyecto.ACTIVO, "65.00", false, p2,
                    "Contenido", "100.00", "65.00",
                    "Recorridos Virtuales", "Videos en 4K Terminados", EstadoEntregable.PENDIENTE, LocalDate.now().plusDays(10),
                    null, null, null, null, null,
                    proyectoRepository, riesgoRepository);

            // PROY-008: Proyecto masivo con múltiples fases (idempotente)
            if (proyectoRepository.findById("IS-PROY-CUN-008").isEmpty()) {
                Proyecto p008 = new Proyecto();
                p008.setId("IS-PROY-CUN-008");
                p008.setNombre("Proyecto Masivo de Pruebas de Estrés UI");
                p008.setDependencia("Calidad de Software");
                p008.setDirector("Usuario de Pruebas");
                p008.setCorreoDirector("qa@cundinamarca.gov.co");
                p008.setFechaInicio(LocalDate.now().minusMonths(4));
                p008.setEstado(EstadoProyecto.ACTIVO);
                p008.setAvanceTotal(new BigDecimal("0.00"));
                p008.setPatrocinador(p1);
                LocalDate fechaBase = LocalDate.now().minusMonths(3);
                for (int f = 1; f <= 5; f++) {
                    Fase fase = buildFase("Fase de Prueba " + f, "20.00", "0.00", p008);
                    for (int h = 1; h <= 3; h++) {
                        Hito hito = buildHito("Hito de Control " + f + "." + h, "33.33", fase);
                        LocalDate hitoInicio = fechaBase.plusWeeks((long) f * 3).plusWeeks(h);
                        for (int e = 1; e <= 4; e++) {
                            addEntregable("Entregable " + f + "." + h + "." + e, "25.00",
                                    EstadoEntregable.PENDIENTE, hitoInicio.plusDays((long) e * 4 + 2), hito);
                        }
                    }
                    p008.getFases().add(fase);
                }
                proyectoRepository.save(p008);
            }

            long total = proyectoRepository.count();
            System.out.println(">>> SEEDER: Finalizado. Total de proyectos en BD: " + total);
        };
    }

    // ── Helper: inserta proyecto solo si no existe ──────────────────────────
    private void seedProyecto(
            String id, String nombre, String dependencia, String director, String correo,
            LocalDate fechaInicio, EstadoProyecto estado, String avance, boolean peti,
            Patrocinador patrocinador,
            String faseNombre, String fasePond, String faseAvance,
            String hitoNombre, String entregableNombre, EstadoEntregable entregableEstado, LocalDate fechaLimite,
            String riesgoCodigo, String riesgoDesc, Probabilidad prob, Impacto impacto, NivelRiesgo nivel,
            ProyectoRepository proyectoRepo, RiesgoRepository riesgoRepo) {

        if (proyectoRepo.findById(id).isPresent()) return;

        Proyecto p = new Proyecto();
        p.setId(id);
        p.setNombre(nombre);
        p.setDependencia(dependencia);
        p.setDirector(director);
        p.setCorreoDirector(correo);
        p.setFechaInicio(fechaInicio);
        p.setEstado(estado);
        p.setAvanceTotal(new BigDecimal(avance));
        p.setPeti(peti);
        p.setPatrocinador(patrocinador);

        Fase fase = buildFase(faseNombre, fasePond, faseAvance, p);
        Hito hito = buildHito(hitoNombre, "100.00", fase);
        addEntregable(entregableNombre, "100.00", entregableEstado, fechaLimite, hito);
        p.getFases().add(fase);
        proyectoRepo.save(p);

        if (riesgoCodigo != null) {
            Riesgo r = new Riesgo();
            r.setCodigo(riesgoCodigo);
            r.setDescripcion(riesgoDesc);
            r.setProbabilidad(prob);
            r.setImpacto(impacto);
            r.setNivel(nivel);
            r.setProyecto(p);
            riesgoRepo.save(r);
        }
    }

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
}
