package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Furag;
import com.proyecta.api_gestion.model.FuragRespuesta;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import com.proyecta.api_gestion.service.report.SimplePdfReportBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Date;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final RiesgoRepository riesgoRepository;
    private final ReporteConfigRepository reporteConfigRepository;
    private final SystemParameterService systemParameterService;
    private final ProjectProgressMetricsService progressMetricsService;
    private final FuragRespuestaRepository furagRespuestaRepository;
    private final SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ProyectoSecurity proyectoSecurity;

    public ReporteServiceImpl(ProyectoRepository proyectoRepository,
                              EntregableRepository entregableRepository,
                              RiesgoRepository riesgoRepository,
                              ReporteConfigRepository reporteConfigRepository,
                              SystemParameterService systemParameterService,
                              ProjectProgressMetricsService progressMetricsService,
                              FuragRespuestaRepository furagRespuestaRepository,
                              SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository,
                              KeycloakIdentityExtractor identityExtractor,
                              ProyectoSecurity proyectoSecurity) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.reporteConfigRepository = reporteConfigRepository;
        this.systemParameterService = systemParameterService;
        this.progressMetricsService = progressMetricsService;
        this.furagRespuestaRepository = furagRespuestaRepository;
        this.seguridadUsuarioProyectoRepository = seguridadUsuarioProyectoRepository;
        this.identityExtractor = identityExtractor;
        this.proyectoSecurity = proyectoSecurity;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteConfigDTO> obtenerConfiguracionReportes() {
        return reporteConfigRepository.findAllByActivoTrueOrderByOrdenAsc().stream()
                .map(c -> new ReporteConfigDTO(c.getId(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReporteVistaPreviaDTO> obtenerVistaPrevia(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(proyecto -> {
            BigDecimal avancePromedio = progressMetricsService
                    .construir(proyecto, LocalDate.now())
                    .avanceTotal()
                    .setScale(2, RoundingMode.HALF_UP);

            List<EntregablePendienteDTO> entregablesVencidos =
                    entregableRepository.findPendientesVencidosByProyecto(proyectoId, LocalDate.now());

            String directorNombre = proyecto.getDirector() != null ? proyecto.getDirector() : "No asignado";
            String patrocinadorNombre = proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : "No asignado";

            return new ReporteVistaPreviaDTO(
                    proyecto.getId(),
                    proyecto.getNombre(),
                    avancePromedio,
                    directorNombre,
                    proyecto.getDependencia(),
                    patrocinadorNombre,
                    entregablesVencidos
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerTodosLosProyectos() {
        return proyectoRepository.getProyectosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos() {
        return proyectoRepository.getProyectosConAtrasosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanComunicacionesDTO> obtenerPlanComunicaciones(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p ->
                new PlanComunicacionesDTO(p.getId(), p.getNombre(), p.getPlanComunicacionesPdf(), p.getDependencia())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuragReporteDTO> obtenerFurag(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p -> {
            validarFuragObligatorio(p.getFurag(), proyectoId);
            return new FuragReporteDTO(
                    p.getId(),
                    p.getNombre(),
                    p.getPeti(),
                    p.getEstrategiaPeti() != null ? p.getEstrategiaPeti().name() : null,
                    p.getVigenciaPeti(),
                    p.getObjetivoGeneral()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoReporteDTO> obtenerRiesgos(String proyectoId) {
        return riesgoRepository.findRiesgosReporteByProyecto(proyectoId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteProyectoPdf(String id) {
        Proyecto proyecto = cargarProyecto(id);
        List<Fase> fases = ordenarFases(proyecto);
        List<ObjetivoEspecifico> objetivos = ordenarObjetivos(proyecto);
        List<Entregable> entregables = obtenerEntregablesProyecto(proyecto.getId());
        List<Entregable> pendientesVencidos = entregables.stream()
                .filter(this::esEntregablePendienteVencido)
                .toList();
        List<Entregable> conformes = entregables.stream()
                .filter(this::esEntregableConforme)
                .toList();

        List<String> lines = new ArrayList<>();
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + vigenciaDelProyecto(proyecto));
        lines.add("SEC|1. INFORMACION GENERAL DEL PROYECTO");
        lines.add("KV|Codigo del proyecto|" + safe(proyecto.getId()));
        lines.add("KV|Nombre del proyecto|" + safe(proyecto.getNombre()));
        lines.add("KV|Dependencia|" + safe(proyecto.getDependencia()));
        lines.add("KV|Patrocinador del proyecto|" + safe(proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : null));
        lines.add("KV|Director del proyecto|" + safe(proyecto.getDirector()));

        lines.add("SEC|2. OBJETIVOS");
        lines.add("SUB|2.1 Objetivo general");
        lines.add("TXT|" + safe(proyecto.getObjetivoGeneral()));
        lines.add("SUB|2.2 Objetivos especificos");
        if (objetivos.isEmpty()) {
            lines.add("BUL|No hay objetivos especificos registrados.");
        } else {
            objetivos.forEach(obj -> lines.add("BUL|" + safe(obj.getDescripcion())));
        }

        lines.add("PAGEBREAK");
        lines.add("SEC|3. AVANCE DEL PROYECTO");
        lines.add("SUB|3.1 Avance total del proyecto");
        lines.add("CARD|Porcentaje de avance general|" + formatPercent(proyecto.getAvanceTotal()) + "|blue");
        lines.add("CHK|Activo|" + (proyecto.getEstado() == EstadoProyecto.ACTIVO));
        lines.add("CHK|Con retrasos|" + (proyecto.getEstado() == EstadoProyecto.CON_RETRASOS));
        lines.add("CHK|Finalizado|" + (proyecto.getEstado() == EstadoProyecto.CERRADO));

        lines.add("SUB|3.2 Avance por fases");
        lines.add("TABLE|26,34,20,20|Fase|Descripcion|% Avance|Estado");
        fases.forEach(fase -> lines.add("ROW|"
                + safe(fase.getNombre()) + "|"
                + safe(fase.getDescripcion()) + "|"
                + formatPercent(fase.getAvanceCalculado()) + "|"
                + estadoFase(fase)));

        lines.add("SUB|3.3 Avance por hitos");
        lines.add("TABLE|28,34,18,20|Hito|Descripcion|Estado|% Avance");
        fases.forEach(fase -> ordenarHitos(fase).forEach(hito -> lines.add("ROW|"
                + safe(hito.getNombre()) + "|"
                + safe(hito.getDescripcion()) + "|"
                + estadoHito(hito) + "|"
                + formatPercent(hito.getAvanceCalculado()))));

        lines.add("PAGEBREAK");
        lines.add("SEC|4. ENTREGABLES");
        lines.add("SUB|4.1 Entregables pendientes de acuerdo a la fecha del reporte");
        lines.add("TABLE|24,22,16,16,10,12|Entregable|Fase / Hito asociado|Fecha planificada|Responsable|Estado actual|Fecha de entrega estimada");
        if (pendientesVencidos.isEmpty()) {
            lines.add("ROW|Sin entregables vencidos|No aplica|No aplica|No aplica|No aplica|No aplica");
        } else {
            pendientesVencidos.forEach(entregable -> lines.add("ROW|"
                    + safe(entregable.getNombre()) + "|"
                    + safe(entregable.getHito() != null && entregable.getHito().getFase() != null
                    ? entregable.getHito().getFase().getNombre() + " / " + entregable.getHito().getNombre()
                    : null) + "|"
                    + formatDate(entregable.getFechaLimite()) + "|"
                    + safe(proyecto.getDirector()) + "|"
                    + estadoEntregable(entregable) + "|"
                    + formatDate(entregable.getFechaEntregaReal())));
        }

        lines.add("SUB|4.2 Entregables a conformidad (Ok)");
        lines.add("TABLE|28,24,16,16,16|Entregable|Fase / Hito asociado|Fecha de entrega|Fecha de aprobacion|Aprobado por");
        if (conformes.isEmpty()) {
            lines.add("ROW|Sin entregables a conformidad|No aplica|No aplica|No aplica|No aplica");
        } else {
            conformes.forEach(entregable -> lines.add("ROW|"
                    + safe(entregable.getNombre()) + "|"
                    + safe(entregable.getHito() != null && entregable.getHito().getFase() != null
                    ? entregable.getHito().getFase().getNombre() + " / " + entregable.getHito().getNombre()
                    : null) + "|"
                    + formatDate(entregable.getFechaEntregaReal()) + "|"
                    + formatDate(entregable.getFechaEntregaReal()) + "|"
                    + safe(proyecto.getDirector())));
        }

        return SimplePdfReportBuilder.build("REPORTE: ESTADO DE PROYECTO EN ESPECIFICO.", lines);
    }

    @Override
    public byte[] generarReportePortafolioPdf() {
        List<ProyectoReporteResumenDTO> proyectos = obtenerTodosLosProyectos();
        List<String> lines = new ArrayList<>();
        lines.add("HDR|Filtro: Proyectos PETI");
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + vigenciaGlobal());
        lines.add("CARD|Total proyectos|" + proyectoRepository.countTotal() + "|blue");
        lines.add("CARD|Proyectos activos|" + proyectoRepository.countActivos() + "|green");
        lines.add("CARD|Proyectos cerrados|" + proyectoRepository.countCerrados() + "|yellow");
        lines.add("CARD|Entregables atrasados|" + proyectoRepository.countEntregablesAtrasados(LocalDate.now()) + "|red");
        lines.add("SEC|Resumen ejecutivo por proyecto");
        lines.add("TABLE|18,34,16,16,16|Codigo|Nombre del proyecto|Avance total (%)|Estado del proyecto|Atrasados");
        proyectos.forEach(proyecto -> lines.add("ROW|"
                + safe(proyecto.id()) + "|"
                + safe(proyecto.nombre()) + "|"
                + formatPercent(proyecto.avance()) + "|"
                + safe(proyecto.estado()) + "|"
                + proyecto.entregablesAtrasados()));
        return SimplePdfReportBuilder.build("REPORTE: ESTADO DE TODOS LOS PROYECTOS.", lines);
    }

    @Override
    public byte[] generarReporteProyectosConRetrasosPdf() {
        List<ProyectoReporteResumenDTO> proyectos = obtenerProyectosConRetrasos();
        List<String> lines = new ArrayList<>();
        lines.add("HDR|Filtro: Todos los proyectos");
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + vigenciaGlobal());
        lines.add("CARD|Proyectos con retrasos|" + proyectos.size() + "|red");
        if (proyectos.isEmpty()) {
            lines.add("TXT|No se encontraron proyectos con entregables atrasados.");
        } else {
            lines.add("SEC|Proyectos con entregables vencidos");
            lines.add("TABLE|18,34,16,16,16|Codigo|Nombre del proyecto|Dependencia|Atrasados|Avance (%)");
            proyectos.forEach(proyecto -> lines.add("ROW|"
                    + safe(proyecto.id()) + "|"
                    + safe(proyecto.nombre()) + "|"
                    + safe(proyecto.dependencia()) + "|"
                    + proyecto.entregablesAtrasados() + "|"
                    + formatPercent(proyecto.avance())));
        }
        return SimplePdfReportBuilder.build("REPORTE: PROYECTOS CON RETRASOS EN LA FECHA DE ENTREGA.", lines);
    }

    @Override
    public byte[] generarReportePlanComunicacionesPdf(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        List<String> lines = new ArrayList<>();
        lines.add("HDR|Filtro: Proyecto especifico");
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + vigenciaDelProyecto(proyecto));
        lines.add("CARD|Proyecto con plan de comunicaciones|" + (proyecto.getPlanComunicacionesPdf() != null ? "SI" : "NO") + "|blue");
        lines.add("CARD|Dependencia|" + safe(proyecto.getDependencia()) + "|green");
        lines.add("SEC|Reporte de plan de comunicaciones");
        lines.add("KV|Codigo del proyecto|" + safe(proyecto.getId()));
        lines.add("KV|Nombre del proyecto|" + safe(proyecto.getNombre()));
        lines.add("KV|Archivo de evidencia|" + safe(proyecto.getPlanComunicacionesPdf()));
        lines.add("SUB|Observaciones");
        lines.add("BUL|El plan de comunicaciones debe cargarse, versionarse y mantener evidencia institucional.");
        lines.add("BUL|La descarga permite previsualizar el archivo antes de reemplazarlo o archivarlo.");
        return SimplePdfReportBuilder.build("REPORTE: PLAN DE COMUNICACIONES.", lines);
    }

    @Override
    public byte[] generarReporteFuragPdf(String proyectoId) {
        FuragReporteDTO dto = obtenerFurag(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<FuragRespuesta> respuestas = furagRespuestaRepository.findByProyecto_IdOrderByCodigoPreguntaAsc(proyectoId);
        long respuestasCompletas = respuestas.stream().filter(r -> r.getRespuesta() != null).count();
        long preguntasObligatorias = respuestas.stream().filter(r -> Boolean.TRUE.equals(r.getObligatoria())).count();

        List<String> lines = new ArrayList<>();
        lines.add("HDR|Filtro: Proyecto especifico");
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + safe(dto.vigenciaPeti()));
        lines.add("CARD|Respuestas completas|" + respuestasCompletas + "|green");
        lines.add("CARD|Preguntas obligatorias|" + preguntasObligatorias + "|blue");
        lines.add("CARD|Es PETI|" + (Boolean.TRUE.equals(dto.esPeti()) ? "SI" : "NO") + "|yellow");
        lines.add("SEC|Reporte FURAG");
        lines.add("KV|Codigo del proyecto|" + safe(dto.proyectoId()));
        lines.add("KV|Nombre del proyecto|" + safe(dto.nombre()));
        lines.add("KV|Estrategia PETI|" + safe(dto.estrategiaPeti()));
        lines.add("KV|Vigencia PETI|" + safe(dto.vigenciaPeti()));
        lines.add("SUB|Objetivo general");
        lines.add("TXT|" + safe(dto.objetivoGeneral()));
        lines.add("SUB|Detalle de respuestas");
        lines.add("TABLE|18,16,18,48|Codigo|Respuesta|Obligatoria|Pregunta");
        respuestas.forEach(respuesta -> lines.add("ROW|"
                + safe(respuesta.getCodigoPregunta()) + "|"
                + safe(respuesta.getRespuesta() != null ? respuesta.getRespuesta().name() : "SIN_RESPUESTA") + "|"
                + (Boolean.TRUE.equals(respuesta.getObligatoria()) ? "SI" : "NO") + "|"
                + safe(respuesta.getPregunta())));
        return SimplePdfReportBuilder.build("REPORTE: FURAG.", lines);
    }

    @Override
    public byte[] generarReporteRiesgosPdf(String proyectoId) {
        List<RiesgoReporteDTO> riesgos = obtenerRiesgos(proyectoId);
        Map<String, Long> porNivel = riesgos.stream()
                .collect(Collectors.groupingBy(r -> r.nivel() != null ? r.nivel().name() : "SIN_NIVEL", LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> porEstado = riesgos.stream()
                .collect(Collectors.groupingBy(r -> r.estado() != null ? r.estado().name() : "SIN_ESTADO", LinkedHashMap::new, Collectors.counting()));

        List<String> lines = new ArrayList<>();
        lines.add("HDR|Filtro: Proyecto especifico");
        lines.add("HDR|Fecha del reporte: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lines.add("HDR|Vigencia: " + vigenciaGlobal());
        lines.add("CARD|Total riesgos|" + riesgos.size() + "|blue");
        lines.add("CARD|Riesgos tratados|" + riesgos.stream().filter(r -> r.estado() == EstadoRiesgo.TRATADO).count() + "|green");
        lines.add("CARD|Pendientes|" + riesgos.stream().filter(r -> r.estado() == EstadoRiesgo.PENDIENTE).count() + "|yellow");
        lines.add("SEC|Verificacion de tratamiento a riesgos");
        lines.add("SUB|Distribucion por nivel");
        porNivel.forEach((nivel, count) -> lines.add("BUL|" + nivel + ": " + count));
        lines.add("SUB|Distribucion por estado");
        porEstado.forEach((estado, count) -> lines.add("BUL|" + estado + ": " + count));
        lines.add("SUB|Listado resumido");
        lines.add("TABLE|14,42,16,16,12|Codigo|Descripcion|Nivel|Tratamiento|Estado");
        riesgos.forEach(riesgo -> lines.add("ROW|"
                + safe(riesgo.codigo()) + "|"
                + safe(riesgo.descripcion()) + "|"
                + safe(riesgo.nivel() != null ? riesgo.nivel().name() : null) + "|"
                + safe(riesgo.tratamiento()) + "|"
                + safe(riesgo.estado() != null ? riesgo.estado().name() : null)));
        return SimplePdfReportBuilder.build("REPORTE: VERIFICACION TRATAMIENTO A RIESGOS.", lines);
    }

    @Override
    public byte[] generarReportePortafolioExcel() {
        return construirExcelPortafolio(
                proyectoRepository.findAll().stream()
                        .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .toList(),
                LocalDate.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePortafolioExcel(Authentication authentication, String query, String dependency, String status, String peti) {
        List<Proyecto> proyectos = obtenerProyectosPortafolio(authentication);
        List<Proyecto> filtrados = aplicarFiltrosPortafolio(proyectos, query, dependency, status, peti);
        return construirExcelPortafolio(filtrados, LocalDate.now());
    }

    private List<Proyecto> obtenerProyectosPortafolio(Authentication authentication) {
        if (authentication != null) {
            try {
                if (proyectoSecurity.canAccessGlobal("PROYECTO:VER", authentication)) {
                    return proyectoRepository.findAll().stream()
                            .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                            .toList();
                }
            } catch (RuntimeException ex) {
                // Si no tiene acceso global, continuamos con el alcance de proyectos asignados.
            }
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<String> proyectoIds = seguridadUsuarioProyectoRepository.findProyectoIdsByUsername(username).stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (proyectoIds.isEmpty()) {
            return List.of();
        }

        return proyectoRepository.findAllById(proyectoIds).stream()
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<Proyecto> aplicarFiltrosPortafolio(List<Proyecto> proyectos, String query, String dependency, String status, String peti) {
        String normalizedQuery = normalizeText(query);
        String normalizedDependency = normalizeText(dependency);
        String normalizedStatus = normalizeText(status);
        String normalizedPeti = normalizeText(peti);

        return proyectos.stream()
                .filter(proyecto -> {
                    String projectStatus = proyecto.getEstado() != null ? proyecto.getEstado().name() : proyecto.getEstadoCodigo();
                    String searchHaystack = String.join(" ",
                            safe(proyecto.getId()),
                            safe(proyecto.getNombre()),
                            safe(proyecto.getObjetivoGeneral()),
                            safe(proyecto.getDependencia()),
                            safe(proyecto.getDirector()),
                            safe(projectStatus),
                            Boolean.TRUE.equals(proyecto.getPeti()) ? "PETI" : "NO PETI");

                    boolean matchesQuery = normalizedQuery.isBlank() || normalizeText(searchHaystack).contains(normalizedQuery);
                    boolean matchesDependency = normalizedDependency.isBlank()
                            || normalizeText(proyecto.getDependencia()).equals(normalizedDependency);
                    boolean matchesStatus = normalizedStatus.isBlank()
                            || normalizeText(projectStatus).equals(normalizedStatus)
                            || normalizeText(proyecto.getEstadoCodigo()).equals(normalizedStatus);
                    boolean matchesPeti = normalizedPeti.isBlank()
                            || ("peti".equals(normalizedPeti) && Boolean.TRUE.equals(proyecto.getPeti()))
                            || ("no_peti".equals(normalizedPeti) && !Boolean.TRUE.equals(proyecto.getPeti()));

                    return matchesQuery && matchesDependency && matchesStatus && matchesPeti;
                })
                .toList();
    }

    private byte[] construirExcelPortafolio(List<Proyecto> proyectos, LocalDate corte) {
        try (
                InputStream templateStream = new ClassPathResource("report-assets/Consolidado Seguimiento Proyectos PETI.xlsx").getInputStream();
                Workbook workbook = new XSSFWorkbook(templateStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            List<RowSnapshot> snapshots = new ArrayList<>();
            for (Proyecto proyecto : proyectos) {
                snapshots.add(buildRowSnapshot(proyecto, corte));
            }

            populateTemplateSheet(sheet, snapshots, corte);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el archivo Excel del portafolio.", ex);
        }
    }

    private void populateTemplateSheet(Sheet sheet, List<RowSnapshot> snapshots, LocalDate corte) {
        Row titleRow = ensureRow(sheet, 1);
        setCellText(titleRow, 2, "SEGUIMIENTO PROYECTOS PETI 2024 - 2028");
        setCellText(titleRow, 3, "FECHA DE CORTE");

        Row dateRow = ensureRow(sheet, 2);
        setCellBlank(dateRow, 2);
        setCellDate(dateRow, 3, Date.from(corte.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        Row headerRow = ensureRow(sheet, 3);
        String[] headers = {
                "Proy",
                "Proyecto Nombre",
                "Meta",
                "Programado",
                "Avance",
                "Diferencia",
                "Estado",
                "Total entregables",
                "Entregables programados al corte",
                "Entregados al corte",
                "Eficacia",
                "Eficiencia",
                "Dependencia",
                "Responsable"
        };
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow, i + 1, headers[i]);
        }

        int dataStartRow = 4;
        int templateRows = 16;
        int visibleRows = Math.min(templateRows, snapshots.size());
        for (int index = 0; index < templateRows; index++) {
            Row row = ensureRow(sheet, dataStartRow + index);
            if (index < visibleRows) {
                RowSnapshot snapshot = snapshots.get(index);
                setCellText(row, 1, snapshot.codigo());
                setCellText(row, 2, snapshot.nombre());
                setCellText(row, 3, snapshot.meta());
                setCellPercent(row, 4, snapshot.programado());
                setCellPercent(row, 5, snapshot.avance());
                setCellPercent(row, 6, snapshot.diferencia());
                setCellText(row, 7, snapshot.estado());
                setCellNumber(row, 8, snapshot.totalEntregables());
                setCellNumber(row, 9, snapshot.entregablesProgramadosAlCorte());
                setCellNumber(row, 10, snapshot.entregablesEntregadosAlCorte());
                setCellPercent(row, 11, snapshot.eficacia());
                setCellPercent(row, 12, snapshot.eficiencia());
                setCellText(row, 13, snapshot.dependencia());
                setCellText(row, 14, snapshot.responsable());
            } else {
                clearTemplateRow(row, 1, 14);
            }
        }

        Row summaryRow = ensureRow(sheet, 20);
        setCellText(summaryRow, 1, "Promedios");
        setCellPercent(summaryRow, 2, promedio(snapshots.stream().map(RowSnapshot::programado).toList()));
        setCellPercent(summaryRow, 3, promedio(snapshots.stream().map(RowSnapshot::avance).toList()));
        setCellPercent(summaryRow, 4, promedio(snapshots.stream().map(RowSnapshot::diferencia).toList()));
        setCellNumber(summaryRow, 5, sumaEntera(snapshots.stream().map(RowSnapshot::totalEntregables).toList()));
        setCellNumber(summaryRow, 6, sumaEntera(snapshots.stream().map(RowSnapshot::entregablesProgramadosAlCorte).toList()));
        setCellNumber(summaryRow, 7, sumaEntera(snapshots.stream().map(RowSnapshot::entregablesEntregadosAlCorte).toList()));
        setCellPercent(summaryRow, 8, promedio(snapshots.stream().map(RowSnapshot::eficacia).toList()));
        setCellPercent(summaryRow, 9, promedio(snapshots.stream().map(RowSnapshot::eficiencia).toList()));
    }

    private Row ensureRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    private void clearTemplateRow(Row row, int fromColumnInclusive, int toColumnInclusive) {
        for (int col = fromColumnInclusive; col <= toColumnInclusive; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
            }
            cell.setBlank();
        }
    }

    private void setCellText(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private void setCellNumber(Row row, int columnIndex, Number value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value.doubleValue());
    }

    private void setCellPercent(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value.doubleValue());
    }

    private void setCellDate(Row row, int columnIndex, Date value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value);
    }

    private void setCellBlank(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setBlank();
    }

    private RowSnapshot buildRowSnapshot(Proyecto proyecto, LocalDate corte) {
        var metrics = progressMetricsService.construir(proyecto, corte);
        BigDecimal divisor = BigDecimal.valueOf(100);
        String estado = metrics.estado() != null ? metrics.estado().replace('_', ' ') : safe(proyecto.getEstadoCodigo());
        return new RowSnapshot(
                safe(proyecto.getId()),
                safe(proyecto.getNombre()),
                safe(proyecto.getObjetivoGeneral()),
                metrics.progresoProgramado() != null ? metrics.progresoProgramado().divide(divisor, 16, RoundingMode.HALF_UP) : null,
                metrics.avanceTotal() != null ? metrics.avanceTotal().divide(divisor, 16, RoundingMode.HALF_UP) : null,
                metrics.diferencia() != null ? metrics.diferencia().divide(divisor, 16, RoundingMode.HALF_UP) : null,
                estado,
                metrics.entregablesTotal(),
                metrics.entregablesProgramadosAlCorte(),
                metrics.entregablesEntregadosAlCorte(),
                metrics.eficacia(),
                metrics.eficiencia(),
                safe(proyecto.getDependencia()),
                safe(proyecto.getDirector())
        );
    }

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloSubtitulo(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloTexto(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloTextoNegrita(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloNumero(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.################"));
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloPorcentaje(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloResumenEtiqueta(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorders(style);
        return style;
    }

    private CellStyle crearEstiloResumenNumero(Workbook workbook) {
        CellStyle style = crearEstiloNumero(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloResumenPorcentaje(Workbook workbook) {
        CellStyle style = crearEstiloPorcentaje(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloFecha(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style);
        return style;
    }

    private void applyThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createStringCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private BigDecimal promedio(List<BigDecimal> values) {
        List<BigDecimal> safeValues = values.stream().filter(value -> value != null).toList();
        if (safeValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = safeValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(safeValues.size()), 16, RoundingMode.HALF_UP);
    }

    private Long sumaEntera(List<? extends Number> values) {
        long total = 0L;
        for (Number value : values) {
            if (value != null) {
                total += value.longValue();
            }
        }
        return total;
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RowSnapshot(
            String codigo,
            String nombre,
            String meta,
            BigDecimal programado,
            BigDecimal avance,
            BigDecimal diferencia,
            String estado,
            Long totalEntregables,
            Long entregablesProgramadosAlCorte,
            Long entregablesEntregadosAlCorte,
            BigDecimal eficacia,
            BigDecimal eficiencia,
            String dependencia,
            String responsable
    ) {}

    private Proyecto cargarProyecto(String proyectoId) {
        return proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
    }

    private List<ObjetivoEspecifico> ordenarObjetivos(Proyecto proyecto) {
        return proyecto.getObjetivosEspecificos() == null ? List.of() :
                proyecto.getObjetivosEspecificos().stream()
                        .sorted((a, b) -> {
                            Short oa = a.getOrden();
                            Short ob = b.getOrden();
                            if (oa == null && ob == null) return 0;
                            if (oa == null) return 1;
                            if (ob == null) return -1;
                            return Short.compare(oa, ob);
                        })
                        .toList();
    }

    private List<Fase> ordenarFases(Proyecto proyecto) {
        return proyecto.getFases() == null ? List.of() :
                proyecto.getFases().stream()
                        .sorted((a, b) -> {
                            if (a.getId() == null && b.getId() == null) return 0;
                            if (a.getId() == null) return 1;
                            if (b.getId() == null) return -1;
                            return Integer.compare(a.getId(), b.getId());
                        })
                        .toList();
    }

    private List<Hito> ordenarHitos(Fase fase) {
        return fase.getHitos() == null ? List.of() :
                fase.getHitos().stream()
                        .sorted((a, b) -> {
                            if (a.getId() == null && b.getId() == null) return 0;
                            if (a.getId() == null) return 1;
                            if (b.getId() == null) return -1;
                            return Integer.compare(a.getId(), b.getId());
                        })
                        .toList();
    }

    private List<Entregable> obtenerEntregablesProyecto(String proyectoId) {
        return entregableRepository.findByProyectoId(proyectoId).stream()
                .sorted((a, b) -> {
                    if (a.getFechaLimite() == null && b.getFechaLimite() == null) return 0;
                    if (a.getFechaLimite() == null) return 1;
                    if (b.getFechaLimite() == null) return -1;
                    int cmp = a.getFechaLimite().compareTo(b.getFechaLimite());
                    if (cmp != 0) return cmp;
                    if (a.getNombre() == null && b.getNombre() == null) return 0;
                    if (a.getNombre() == null) return 1;
                    if (b.getNombre() == null) return -1;
                    return a.getNombre().compareToIgnoreCase(b.getNombre());
                })
                .toList();
    }

    private boolean esEntregablePendienteVencido(Entregable entregable) {
        if (entregable == null || entregable.getFechaLimite() == null) {
            return false;
        }
        if (esEntregableConforme(entregable)) {
            return false;
        }
        return entregable.getFechaLimite().isBefore(LocalDate.now());
    }

    private boolean esEntregableConforme(Entregable entregable) {
        if (entregable == null) {
            return false;
        }
        return EstadoEntregable.COMPLETADO.equals(entregable.getEstado())
                || EstadoEntregable.A_CONFORMIDAD.equals(entregable.getEstado())
                || Boolean.TRUE.equals(entregable.getConforme());
    }

    private String estadoEntregable(Entregable entregable) {
        if (entregable == null) {
            return "No disponible";
        }
        if (EstadoEntregable.A_CONFORMIDAD.equals(entregable.getEstado()) || Boolean.TRUE.equals(entregable.getConforme())) {
            return "A conformidad";
        }
        if (EstadoEntregable.COMPLETADO.equals(entregable.getEstado())) {
            return "Completado";
        }
        if (entregable.getFechaLimite() != null && entregable.getFechaLimite().isBefore(LocalDate.now())) {
            return "Vencido";
        }
        return "Pendiente";
    }

    private String estadoFase(Fase fase) {
        if (fase == null || fase.getAvanceCalculado() == null) {
            return "Pendiente";
        }
        if (fase.getAvanceCalculado().compareTo(new BigDecimal("100")) >= 0) {
            return "Cumplido";
        }
        if (fase.getAvanceCalculado().compareTo(BigDecimal.ZERO) > 0) {
            return "En progreso";
        }
        return "Pendiente";
    }

    private String estadoHito(Hito hito) {
        if (hito == null || hito.getAvanceCalculado() == null) {
            return "Pendiente";
        }
        if (hito.getAvanceCalculado().compareTo(new BigDecimal("100")) >= 0) {
            return "Cumplido";
        }
        if (hito.getAvanceCalculado().compareTo(BigDecimal.ZERO) > 0) {
            return "En progreso";
        }
        return "Pendiente";
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "No disponible";
        }
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String vigenciaDelProyecto(Proyecto proyecto) {
        if (proyecto == null) {
            return vigenciaGlobal();
        }
        String vigencia = proyecto.getVigenciaPeti();
        if (vigencia != null && !vigencia.isBlank()) {
            return vigencia;
        }
        return vigenciaGlobal();
    }

    private String vigenciaGlobal() {
        int year = LocalDate.now().getYear();
        return year + "-" + (year + 3);
    }

    private void validarFuragObligatorio(Furag furag, String proyectoId) {
        if (furag == null) {
            throw new ForbiddenException("El proyecto " + proyectoId + " no tiene respuestas FURAG suficientes para generar el reporte.");
        }

        int minimoRespuestas = systemParameterService.getInt(SystemParameterKeys.FURAG_RESPUESTAS_OBLIGATORIAS, 7);
        long respuestasCompletas = furagRespuestaRepository.countByProyecto_IdAndRespuestaIsNotNull(proyectoId);
        if (respuestasCompletas == 0) {
            if (furag.getInfraestructuraDatos() != null) respuestasCompletas++;
            if (furag.getInteroperabilidad() != null) respuestasCompletas++;
            if (furag.getDigitalizacionAutomatizacion() != null) respuestasCompletas++;
            if (furag.getContratacionPublica() != null) respuestasCompletas++;
            if (furag.getServiciosNube() != null) respuestasCompletas++;
            if (furag.getSandbox() != null) respuestasCompletas++;
            if (furag.getTecnologiasEmergentes() != null) respuestasCompletas++;
        }

        if (respuestasCompletas < minimoRespuestas) {
            throw new ForbiddenException("El proyecto " + proyectoId + " no tiene completas las respuestas FURAG obligatorias para generar el reporte.");
        }
    }

    private static String formatPercent(BigDecimal value) {
        if (value == null) {
            return "0.00%";
        }
        BigDecimal normalized = value;
        if (normalized.compareTo(BigDecimal.ONE) <= 0) {
            normalized = normalized.multiply(BigDecimal.valueOf(100));
        }
        return normalized.setScale(2, RoundingMode.HALF_UP) + "%";
    }
}
