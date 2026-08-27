package com.proyecta.api_gestion.service.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.*;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

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
    private final ProyectoService proyectoService;
    private final ObjectMapper objectMapper;

    public ProyectoSeeder(ProyectoRepository proyectoRepository,
                          RiesgoRepository riesgoRepository,
                          PatrocinadorSeeder patrocinadorSeeder,
                          ProyectoService proyectoService,
                          ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.riesgoRepository = riesgoRepository;
        this.patrocinadorSeeder = patrocinadorSeeder;
        this.proyectoService = proyectoService;
        this.objectMapper = objectMapper;
    }

    public void seedProyectos() {
        logger.info("Cargando proyectos semilla...");
        
        Patrocinador ticSecretary = patrocinadorSeeder.obtenerPatrocinador("Secretaría de TIC");
        Patrocinador infraDir = patrocinadorSeeder.obtenerPatrocinador("Dirección de Infraestructura");
        
        // Proyectos con retrasos
        crearProyecto(
            "PROY-CUN-2026-001",
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
            "PROY-CUN-2026-002",
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
            "PROY-CUN-2026-003",
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
            "PROY-CUN-2026-004",
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
            "PROY-CUN-2026-005",
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
            "PROY-CUN-2026-006",
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
            "PROY-CUN-2026-007",
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
            "PROY-CUN-2026-015",
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

        // Proyecto realista para pruebas funcionales del flujo completo
        crearProyectoRealista();
        
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
        if (proyectoRepository.findById("PROY-CUN-2026-008").isPresent()) {
            proyectoRepository.findById("PROY-CUN-2026-008").ifPresent(this::agregarRiesgosMasivos);
            return;
        }
        
        Proyecto p = new Proyecto();
        p.setId("PROY-CUN-2026-008");
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
        agregarRiesgosMasivos(p);
        logger.debug("Proyecto masivo creado: PROY-CUN-2026-008");
    }

    private void crearProyectoRealista() {
        try {
            if (proyectoRepository.existsByNombreAndDependencia(
                    "Modernización Integral de la Atención Ciudadana y Gestión TIC",
                    "Secretaría de Transformación Digital")) {
                return;
            }

            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("examples/proyecto-realista.json")) {
                if (inputStream == null) {
                    logger.warn("No se encontró el archivo examples/proyecto-realista.json; se omite el proyecto realista.");
                    return;
                }

                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                ProyectoCreateDTO dto = objectMapper.readValue(normalizarProyectoRealistaJson(json), ProyectoCreateDTO.class);
                proyectoService.crearProyecto(dto);
                logger.debug("Proyecto realista sembrado desde examples/proyecto-realista.json");
            }
        } catch (Exception ex) {
            logger.error("No fue posible sembrar el proyecto realista de ejemplo", ex);
        }
    }

    private String normalizarProyectoRealistaJson(String json) {
        try {
            var root = objectMapper.readTree(json);
            LocalDate fechaInicio = root.hasNonNull("fechaInicio")
                    ? LocalDate.parse(root.get("fechaInicio").asText())
                    : LocalDate.now().plusMonths(6);

            var fases = root.path("fases");
            if (fases.isArray()) {
                int faseIndex = 0;
                for (JsonNode fase : fases) {
                    var hitos = fase.path("hitos");
                    if (!hitos.isArray()) continue;
                    int hitoIndex = 0;
                    for (JsonNode hito : hitos) {
                        var entregables = hito.path("entregables");
                        if (!entregables.isArray()) continue;
                        int entregableIndex = 0;
                        for (JsonNode entregable : entregables) {
                            if (entregable.isObject()) {
                                var obj = (com.fasterxml.jackson.databind.node.ObjectNode) entregable;
                                LocalDate fechaLimite = obj.hasNonNull("fechaLimite")
                                        ? LocalDate.parse(obj.get("fechaLimite").asText())
                                        : fechaInicio.plusMonths(1);
                                LocalDate fechaInicioEntregable = obj.hasNonNull("fechaInicio")
                                        ? LocalDate.parse(obj.get("fechaInicio").asText())
                                        : fechaLimite.minusDays(7L + entregableIndex);
                                if (fechaInicioEntregable.isBefore(fechaInicio)) {
                                    fechaInicioEntregable = fechaInicio;
                                }
                                if (fechaInicioEntregable.isAfter(fechaLimite)) {
                                    fechaInicioEntregable = fechaLimite.minusDays(1);
                                }
                                obj.put("fechaInicio", fechaInicioEntregable.toString());
                                if (!obj.hasNonNull("fechaLimite")) {
                                    obj.put("fechaLimite", fechaLimite.toString());
                                }
                            }
                            entregableIndex++;
                        }
                        hitoIndex++;
                    }
                    faseIndex++;
                }
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            logger.warn("No fue posible normalizar el JSON del proyecto realista, se usara original: {}", ex.getMessage());
            return json;
        }
    }

    private void agregarRiesgosMasivos(Proyecto proyecto) {
        if (!riesgoRepository.findByProyectoId(proyecto.getId()).isEmpty()) {
            return;
        }

        List<Riesgo> riesgos = new ArrayList<>();
        riesgos.add(crearRiesgo(proyecto, "R01", "Sobrecarga de demanda sobre la mesa de ayuda", "Operativo", "Alta rotación de solicitudes en temporadas de corte", "Aumento de tiempos de respuesta y saturación del equipo de soporte", Probabilidad.ALTA, Impacto.MEDIO, Probabilidad.MEDIA, Impacto.MEDIO, "Priorización por criticidad, colas de atención y refuerzo temporal de soporte", "Preventivo", "Alta", "Escalar a mesa de nivel 2 y redistribuir tickets por criticidad", "Dirección TIC", "Coordinador de Soporte", LocalDate.now().plusDays(15), "Indicador: tiempo promedio de respuesta", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R02", "Desalineación entre alcance y necesidades reales", "Funcional", "Cambios frecuentes en requisitos durante la ejecución", "Reprocesos, incremento de alcance y retrasos acumulados", Probabilidad.ALTA, Impacto.ALTO, Probabilidad.MEDIA, Impacto.ALTO, "Control de cambios con comité y actas de validación", "Preventivo", "Alta", "Registrar cambios en comité y ajustar backlog", "Oficina de Planeación TIC", "Analista Funcional", LocalDate.now().plusDays(20), "Indicador: número de cambios aprobados", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R03", "Fallas en autenticación o permisos", "Tecnológico", "Configuración incompleta de roles o expiración de credenciales", "Bloqueo de usuarios y detención de procesos críticos", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Monitoreo de logs, pruebas de acceso y respaldo de cuentas críticas", "Correctivo", "Alta", "Rotar credenciales y validar permisos por ambiente", "Seguridad de la Información", "Administrador de Seguridad", LocalDate.now().plusDays(12), "Indicador: accesos fallidos por hora", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R04", "Pérdida de integridad de datos", "Datos", "Errores de sincronización o validaciones incompletas", "Información inconsistente en reportes y trazabilidad", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Backups automáticos, validaciones de negocio y auditoría", "Preventivo", "Alta", "Ejecutar conciliación diaria y revisión de logs", "Gestión de Datos", "Arquitecto de Datos", LocalDate.now().plusDays(18), "Indicador: inconsistencias detectadas", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R05", "Retraso en entregas críticas de desarrollo", "Cronograma", "Dependencias técnicas y disponibilidad parcial del equipo", "Atraso de hitos y compresión de pruebas", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.MEDIA, Impacto.BAJO, "Plan de iteraciones, seguimiento semanal y tablero Kanban", "Preventivo", "Media", "Reasignar tareas y bloquear alcance no prioritario", "PMO TIC", "Líder de Proyecto", LocalDate.now().plusDays(10), "Indicador: tareas vencidas", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R06", "Caídas en ambientes de prueba", "Infraestructura", "Recursos limitados y pruebas concurrentes", "Interrupción de validaciones y pérdida de productividad", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Ventanas de prueba, monitoreo y recursos reservados", "Correctivo", "Media", "Reiniciar ambientes y validar estabilidad previa", "Infraestructura TIC", "Administrador de Ambientes", LocalDate.now().plusDays(14), "Indicador: disponibilidad de ambientes", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R07", "Rechazo del usuario final", "Adopción", "Pantallas poco familiares o capacitación insuficiente", "Baja utilización de la solución y regreso a procesos manuales", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Capacitación, acompañamiento y mejora UX iterativa", "Preventivo", "Alta", "Ajustar interfaz y reforzar formación", "Gestión del Cambio", "Líder de Adopción", LocalDate.now().plusDays(25), "Indicador: usuarios capacitados vs usuarios activos", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R08", "Desfase de fechas de entregables", "Cronograma", "Dependencias externas y validaciones tardías", "Atrasos acumulados y reprogramación de fases", Probabilidad.ALTA, Impacto.MEDIO, Probabilidad.MEDIA, Impacto.MEDIO, "Alertas tempranas y revisión de hitos semanales", "Preventivo", "Alta", "Escalar alertas y ajustar secuencias", "PMO TIC", "Gestor del Cronograma", LocalDate.now().plusDays(9), "Indicador: entregables vencidos", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R09", "Documentación incompleta", "Documental", "Falta de estandarización al registrar evidencias", "Dificultad para auditoría y cierre técnico", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Checklist documental y revisión de calidad", "Preventivo", "Media", "Completar evidencias faltantes antes de cierre", "Gestión Documental", "Analista Documental", LocalDate.now().plusDays(17), "Indicador: documentos pendientes", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R10", "Integración inestable con servicios externos", "Integración", "Cambios en contratos o latencia en APIs", "Errores intermitentes y reintentos fallidos", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Versionamiento de contratos y pruebas contractuales", "Preventivo", "Alta", "Ajustar timeouts y manejar fallback", "Arquitectura de Integración", "Líder Técnico", LocalDate.now().plusDays(13), "Indicador: porcentaje de fallos de integración", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R11", "Sobrecarga de reportes al corte", "Desempeño", "Consultas complejas sobre grandes volúmenes de datos", "Lentitud en tableros y reportes institucionales", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Índices de BD y caché de consultas", "Preventivo", "Media", "Optimizar consultas y programar generación nocturna", "Desarrollo", "Ingeniero Backend", LocalDate.now().plusDays(11), "Indicador: tiempo de generación de reportes", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R12", "Fuga de documentos sensibles", "Seguridad", "Permisos mal configurados o enlaces expuestos", "Exposición de información confidencial", Probabilidad.BAJA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Acceso por rol, auditoría y trazabilidad", "Preventivo", "Alta", "Revisar permisos y revocar accesos no autorizados", "Seguridad de la Información", "Oficial de Seguridad", LocalDate.now().plusDays(7), "Indicador: accesos inusuales", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R13", "Dependencia de una sola persona clave", "Gestión del talento", "Conocimiento concentrado en un miembro del equipo", "Riesgo de paro parcial ante ausencia", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Documentación técnica y pareamiento", "Preventivo", "Media", "Asignar suplente y transferir conocimiento", "PMO TIC", "Líder Funcional", LocalDate.now().plusDays(30), "Indicador: dependencia crítica", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R14", "Errores de configuración en ambientes", "Técnico", "Parámetros distintos entre desarrollo, pruebas y producción", "Comportamientos inconsistentes y fallos de despliegue", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Plantillas de configuración y revisión por pares", "Preventivo", "Alta", "Homologar variables de entorno", "DevOps", "Ingeniero DevOps", LocalDate.now().plusDays(16), "Indicador: incidencias por despliegue", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R15", "Cambio normativo o de lineamientos", "Normativo", "Nuevas directrices institucionales o regulatorias", "Reajuste de alcance, validaciones y tiempos", Probabilidad.BAJA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Seguimiento jurídico y revisión periódica", "Preventivo", "Media", "Ajustar el diseño a la nueva directriz", "Asesoría Jurídica", "Abogado TIC", LocalDate.now().plusDays(45), "Indicador: cambios regulatorios", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R16", "Baja calidad en datos de entrada", "Datos", "Cargas manuales con información incompleta o duplicada", "Resultados erróneos y reprocesos", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Validaciones de formato y catálogos maestros", "Preventivo", "Media", "Depurar datos y bloquear entradas inválidas", "Gestión de Datos", "Analista de Calidad", LocalDate.now().plusDays(21), "Indicador: registros rechazados", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R17", "Pérdida de evidencias de avance", "Control interno", "Carga incorrecta o borrado accidental de archivos", "Dificultad para demostrar cumplimiento", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Repositorio central y respaldo automático", "Preventivo", "Alta", "Restaurar evidencias desde backup", "Control Interno", "Gestor de Evidencias", LocalDate.now().plusDays(8), "Indicador: evidencias faltantes", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R18", "Atraso en aprobaciones internas", "Gobernanza", "Tiempos de revisión superiores a lo previsto", "Bloqueo de hitos y dependencia del comité", Probabilidad.ALTA, Impacto.MEDIO, Probabilidad.MEDIA, Impacto.BAJO, "Agenda de comité y tiempos máximos de respuesta", "Preventivo", "Media", "Escalar aprobaciones pendientes", "Comité TIC", "Secretaría Técnica", LocalDate.now().plusDays(6), "Indicador: aprobaciones pendientes", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R19", "Sobrecarga de soporte post-implementación", "Operación", "Alta demanda inicial tras la salida a producción", "Retrasos en atención y percepción negativa del usuario", Probabilidad.MEDIA, Impacto.MEDIO, Probabilidad.BAJA, Impacto.BAJO, "Plan de estabilización y mesa de ayuda reforzada", "Correctivo", "Media", "Ampliar cobertura temporal de soporte", "Soporte TIC", "Coordinador de Soporte", LocalDate.now().plusDays(18), "Indicador: tickets abiertos", EstadoRiesgo.PENDIENTE));
        riesgos.add(crearRiesgo(proyecto, "R20", "Inconsistencias entre avance físico y avance reportado", "Seguimiento", "Registro tardío de entregables o evidencia mal cargada", "Desfase entre la realidad del proyecto y el semáforo", Probabilidad.MEDIA, Impacto.ALTO, Probabilidad.BAJA, Impacto.MEDIO, "Validación semanal de evidencias y corte operativo", "Preventivo", "Alta", "Cruzar evidencia con avance y corregir el reporte", "PMO TIC", "Analista de Seguimiento", LocalDate.now().plusDays(5), "Indicador: diferencias entre reporte y ejecución", EstadoRiesgo.PENDIENTE));

        riesgoRepository.saveAll(riesgos);
        riesgos.forEach(riesgo -> {
            if (riesgo.getId() != null && (riesgo.getCodigo() == null || riesgo.getCodigo().isBlank())) {
                riesgo.setCodigo("R" + String.format("%02d", riesgo.getId()));
            }
        });
        riesgoRepository.saveAll(riesgos);
    }

    private Riesgo crearRiesgo(Proyecto proyecto,
                               String codigo,
                               String descripcion,
                               String categoria,
                               String causa,
                               String consecuencia,
                               Probabilidad probabilidad,
                               Impacto impacto,
                               Probabilidad probabilidadResidual,
                               Impacto impactoResidual,
                               String controlesExistentes,
                               String tipoControl,
                               String valoracionControl,
                               String tratamiento,
                               String entidadResponsable,
                               String rolResponsable,
                               LocalDate fechaAccion,
                               String evidenciaIndicador,
                               EstadoRiesgo estado) {
        Riesgo riesgo = new Riesgo();
        riesgo.setProyecto(proyecto);
        riesgo.setCodigo(codigo);
        riesgo.setDescripcion(descripcion);
        riesgo.setCategoriaRiesgo(categoria);
        riesgo.setCausa(causa);
        riesgo.setConsecuencia(consecuencia);
        riesgo.setProbabilidad(probabilidad);
        riesgo.setImpacto(impacto);
        riesgo.setNivel(calcularNivel(probabilidad, impacto));
        riesgo.setProbabilidadResidual(probabilidadResidual);
        riesgo.setImpactoResidual(impactoResidual);
        riesgo.setNivelResidual(calcularNivel(probabilidadResidual, impactoResidual));
        riesgo.setControlesExistentes(controlesExistentes);
        riesgo.setTipoControl(tipoControl);
        riesgo.setValoracionControl(valoracionControl);
        riesgo.setTratamiento(tratamiento);
        riesgo.setAccionesMitigacion("Implementar seguimiento semanal, bitácora de control y responsables por acción.");
        riesgo.setEntidadResponsable(entidadResponsable);
        riesgo.setRolResponsable(rolResponsable);
        riesgo.setFechaAccion(fechaAccion);
        riesgo.setEvidenciaIndicador(evidenciaIndicador);
        riesgo.setEstado(estado);
        riesgo.setFechaActualizacion(LocalDateTime.now());
        return riesgo;
    }

    private NivelRiesgo calcularNivel(Probabilidad probabilidad, Impacto impacto) {
        if (probabilidad == null || impacto == null) {
            return NivelRiesgo.BAJO;
        }
        String clave = probabilidad.name() + "-" + impacto.name();
        return switch (clave) {
            case "BAJA-BAJO", "BAJA-MEDIO", "MEDIA-BAJO" -> NivelRiesgo.BAJO;
            case "BAJA-ALTO", "MEDIA-MEDIO", "ALTA-BAJO" -> NivelRiesgo.MODERADO;
            case "MEDIA-ALTO", "ALTA-MEDIO" -> NivelRiesgo.ALTO;
            case "ALTA-ALTO" -> NivelRiesgo.EXTREMO;
            default -> NivelRiesgo.BAJO;
        };
    }
}
