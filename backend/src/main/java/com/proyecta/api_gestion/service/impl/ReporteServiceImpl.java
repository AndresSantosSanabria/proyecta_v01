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
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import com.proyecta.api_gestion.service.report.SimplePdfReportBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final RiesgoRepository riesgoRepository;
    private final ReporteConfigRepository reporteConfigRepository;
    private final SystemParameterService systemParameterService;
    private final ProjectProgressMetricsService progressMetricsService;
    private final FuragRespuestaRepository furagRespuestaRepository;

    public ReporteServiceImpl(ProyectoRepository proyectoRepository,
                              EntregableRepository entregableRepository,
                              RiesgoRepository riesgoRepository,
                              ReporteConfigRepository reporteConfigRepository,
                              SystemParameterService systemParameterService,
                              ProjectProgressMetricsService progressMetricsService,
                              FuragRespuestaRepository furagRespuestaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.reporteConfigRepository = reporteConfigRepository;
        this.systemParameterService = systemParameterService;
        this.progressMetricsService = progressMetricsService;
        this.furagRespuestaRepository = furagRespuestaRepository;
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
        return "Contenido Excel simulado para portafolio".getBytes();
    }

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

    private static String safe(String value) {
        return value == null || value.isBlank() ? "No disponible" : value;
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
