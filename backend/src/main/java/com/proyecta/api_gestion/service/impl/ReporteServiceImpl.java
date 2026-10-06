package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO;
import com.proyecta.api_gestion.application.readmodel.EntregablePendienteDTO;
import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.domain.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.application.port.out.persistence.*;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.service.report.ExcelSheetSupport;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import com.proyecta.api_gestion.service.report.EstadoTodosProyectosPdfGenerator;
import com.proyecta.api_gestion.service.report.FuragPdfGenerator;
import com.proyecta.api_gestion.service.report.ProyectosConRetrasosPdfGenerator;
import com.proyecta.api_gestion.service.report.PlanComunicacionesPdfGenerator;
import com.proyecta.api_gestion.service.report.RiesgosVerificacionPdfGenerator;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import com.proyecta.api_gestion.service.report.EstadoProyectoEspecificoPdfGenerator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

@Service
public class ReporteServiceImpl implements ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteServiceImpl.class);

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final EntregableRepositoryPort entregableRepositoryPort;
    private final RiesgoRepositoryPort riesgoRepositoryPort;
    private final ReporteConfigRepositoryPort reporteConfigRepositoryPort;
    private final ProjectProgressMetricsService progressMetricsService;
    private final SeguridadUsuarioProyectoRepositoryPort seguridadUsuarioProyectoRepositoryPort;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ProyectoSecurity proyectoSecurity;
    private final PublicEvidenceAccessService publicEvidenceAccessService;
    private final PublicUrlProperties publicUrlProperties;
    private final FuragRespuestaRepositoryPort furagRespuestaRepositoryPort;

    /** Proxy transaccional de esta misma bean; null en tests unitarios sin contexto Spring. */
    private final ReporteServiceImpl self;

    private ReporteServiceImpl selfProxy() {
        return self != null ? self : this;
    }

    private static final Set<String> FURAG_CLAVES = Set.of(
            "infraestructuraDatos", "interoperabilidad", "digitalizacionAutomatizacion",
            "contratacionPublica", "serviciosNube", "sandbox", "tecnologiasEmergentes");

    public ReporteServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                              EntregableRepositoryPort entregableRepositoryPort,
                              RiesgoRepositoryPort riesgoRepositoryPort,
                              ReporteConfigRepositoryPort reporteConfigRepositoryPort,
                              ProjectProgressMetricsService progressMetricsService,
                              SeguridadUsuarioProyectoRepositoryPort seguridadUsuarioProyectoRepositoryPort,
                              KeycloakIdentityExtractor identityExtractor,
                              ProyectoSecurity proyectoSecurity,
                              PublicEvidenceAccessService publicEvidenceAccessService,
                              PublicUrlProperties publicUrlProperties,
                              FuragRespuestaRepositoryPort furagRespuestaRepositoryPort,
                              @Lazy ReporteServiceImpl self) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.riesgoRepositoryPort = riesgoRepositoryPort;
        this.reporteConfigRepositoryPort = reporteConfigRepositoryPort;
        this.progressMetricsService = progressMetricsService;
        this.seguridadUsuarioProyectoRepositoryPort = seguridadUsuarioProyectoRepositoryPort;
        this.identityExtractor = identityExtractor;
        this.proyectoSecurity = proyectoSecurity;
        this.publicEvidenceAccessService = publicEvidenceAccessService;
        this.publicUrlProperties = publicUrlProperties;
        this.furagRespuestaRepositoryPort = furagRespuestaRepositoryPort;
        this.self = self;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteConfigDTO> obtenerConfiguracionReportes() {
        return reporteConfigRepositoryPort.findAllByActivoTrueOrderByOrdenAsc().stream()
                .map(c -> new ReporteConfigDTO(c.getId(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReporteVistaPreviaDTO> obtenerVistaPrevia(String proyectoId) {
        return proyectoRepositoryPort.findById(proyectoId).map(proyecto -> {
            BigDecimal avancePromedio = progressMetricsService
                    .construir(proyecto, LocalDate.now(ZoneId.systemDefault()))
                    .avanceTotal()
                    .setScale(2, RoundingMode.HALF_UP);

            List<EntregablePendienteDTO> entregablesVencidos =
                    entregableRepositoryPort.findPendientesVencidosByProyecto(proyectoId, LocalDate.now(ZoneId.systemDefault()));

            String directorNombre = resolveDirectorAsignado(proyecto);
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
        return proyectoRepositoryPort.getProyectosResumen(LocalDate.now(ZoneId.systemDefault()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos() {
        return proyectoRepositoryPort.getProyectosConAtrasosResumen(LocalDate.now(ZoneId.systemDefault()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuragReporteDTO> obtenerFurag(String proyectoId) {
        return proyectoRepositoryPort.findById(proyectoId).map(p -> new FuragReporteDTO(
                p.getId(),
                p.getNombre(),
                p.getPeti(),
                codigoEstrategiaPeti(p),
                p.getVigenciaPeti(),
                p.getObjetivoGeneral()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoVerificacionReporteDTO> obtenerVerificacionRiesgos() {
        List<Proyecto> proyectosCierre = proyectoRepositoryPort.findAll().stream()
                .filter(Proyecto::esEstadoTerminal)
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        Map<String, List<com.proyecta.api_gestion.domain.model.Riesgo>> riesgosPorProyecto = riesgoRepositoryPort.findAll().stream()
                .filter(r -> r.getProyecto() != null && r.getProyecto().getId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getProyecto().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return proyectosCierre.stream()
                .map(proyecto -> {
                    List<com.proyecta.api_gestion.domain.model.Riesgo> riesgosProyecto =
                            riesgosPorProyecto.getOrDefault(proyecto.getId(), List.of());
                    boolean diligencioTratamiento = !riesgosProyecto.isEmpty()
                            && riesgosProyecto.stream().allMatch(this::esTratamientoDiligenciado);
                    return new RiesgoVerificacionReporteDTO(
                            proyecto.getId(),
                            proyecto.getNombre(),
                            safe(resolveDirectorAsignado(proyecto)),
                            safeOrDefault(proyecto.getDependencia()),
                            diligencioTratamiento
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteProyectoPdf(String id, String detailMode) {
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
        return new EstadoProyectoEspecificoPdfGenerator().build(
                proyecto,
                fases,
                objetivos,
                pendientesVencidos,
                conformes,
                detailMode
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePortafolioPdf(String detailMode) {
        List<ProyectoReporteResumenDTO> proyectos = selfProxy().obtenerTodosLosProyectos();
        return new EstadoTodosProyectosPdfGenerator().build(proyectos, LocalDate.now(ZoneId.systemDefault()), detailMode);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteProyectosConRetrasosPdf(String detailMode) {
        List<ProyectoReporteResumenDTO> proyectos = selfProxy().obtenerProyectosConRetrasos();
        List<EntregablePendienteDTO> entregables = proyectos.isEmpty()
                ? List.of()
                : entregableRepositoryPort.findPendientesVencidosByProyecto(proyectos.get(0).id(), LocalDate.now(ZoneId.systemDefault())).stream()
                .limit(3)
                .toList();
        return new ProyectosConRetrasosPdfGenerator().build(proyectos, entregables, LocalDate.now(ZoneId.systemDefault()), detailMode);
    }

    @Override
    public byte[] generarReportePlanComunicacionesPdf(String detailMode) {
        List<Proyecto> proyectos = proyectoRepositoryPort.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getPeti()))
                .filter(p -> !esPendienteCompletar(p))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new PlanComunicacionesPdfGenerator().build(proyectos, LocalDate.now(ZoneId.systemDefault()), detailMode);
    }

    @Override
    public byte[] generarReporteFuragPdf(String proyectoId, String detailMode) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        String dependencia = safe(proyecto.getDependencia());

        List<Proyecto> proyectosDependencia = proyectoRepositoryPort.findAll().stream()
                .filter(p -> sameText(p.getDependencia(), dependencia))
                .filter(p -> !esPendienteCompletar(p))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new FuragPdfGenerator().build(proyecto, proyectosDependencia, LocalDate.now(ZoneId.systemDefault()), detailMode,
                proyectosCompletosFurag(proyectosDependencia));
    }

    private Set<String> proyectosCompletosFurag(List<Proyecto> proyectos) {
        Set<String> ids = proyectos.stream().map(Proyecto::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Set.of();
        }
        Map<String, Set<String>> clavesPorProyecto = furagRespuestaRepositoryPort.findByProyectoIdIn(ids).stream()
                .filter(r -> r.getRespuesta() != null && FURAG_CLAVES.contains(r.getCodigoPregunta()))
                .collect(Collectors.groupingBy(
                        r -> r.getProyecto().getId(),
                        Collectors.mapping(r -> r.getCodigoPregunta(), Collectors.toSet())));
        return clavesPorProyecto.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(FURAG_CLAVES))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteRiesgosPdf(String detailMode) {
        List<RiesgoVerificacionReporteDTO> reportes = selfProxy().obtenerVerificacionRiesgos();
        return new RiesgosVerificacionPdfGenerator().build(reportes, LocalDate.now(ZoneId.systemDefault()), detailMode);
    }

    @Override
    public byte[] generarReportePortafolioExcel() {
        return construirExcelPortafolio(
                proyectoRepositoryPort.findAll().stream()
                        .filter(p -> !esPendienteCompletar(p))
                        .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .toList(),
                LocalDate.now(ZoneId.systemDefault())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePortafolioExcel(Authentication authentication, String query, String dependency, String status, String peti) {
        List<Proyecto> proyectos = obtenerProyectosPortafolio(authentication);
        List<Proyecto> filtrados = aplicarFiltrosPortafolio(proyectos, query, dependency, status, peti);
        return construirExcelPortafolio(filtrados, LocalDate.now(ZoneId.systemDefault()));
    }

    @Override
    @Transactional
    public byte[] generarReporteActualProyectoExcel(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        LocalDate corte = LocalDate.now(ZoneId.systemDefault());
        ProyectoAvanceResponseDTO avance = progressMetricsService.construir(proyecto, corte);

        try (
                InputStream templateStream = new ClassPathResource("report-assets/Reporte actual proyecto PETI.xlsx").getInputStream();
                Workbook workbook = new XSSFWorkbook(templateStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            poblarFichaProyecto(workbook.getSheetAt(0), proyecto);
            Map<String, Map<String, Integer>> context = poblarSeguimientoProyecto(workbook.getSheetAt(1), proyecto, avance, corte);
            poblarAvancesProyecto(workbook.getSheetAt(2), proyecto, context);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el reporte actual Excel del proyecto.", ex);
        }
    }

    private void poblarFichaProyecto(Sheet sheet, Proyecto proyecto) {
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 1), 2, safe(proyecto.getId()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 2), 2, safe(proyecto.getNombre()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 3), 2, safe(proyecto.getAlcanceDetallado()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 4), 2, proyecto.getPatrocinador() == null ? "" : safe(proyecto.getPatrocinador().getNombre()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 5), 2, "");
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 6), 2, safe(proyecto.getDependencia()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 7), 2, safe(resolveDirectorAsignado(proyecto)));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 8), 2, safe(proyecto.getCorreoDirector()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 9), 2, safe(proyecto.getObjetivoGeneral()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 10), 2, objetivosEspecificos(proyecto));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 11), 2, formatYear(proyecto.getFechaInicio()));
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 12), 2, formatYear(fechaFinProyecto(proyecto)));
        ExcelSheetSupport.setCellNumber(ExcelSheetSupport.ensureRow(sheet, 13), 2, proyecto.getPresupuestoEstimado());
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 14), 2, Boolean.TRUE.equals(proyecto.getPeti()) ? "SI" : "NO");
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 15), 2, estrategiaPeti(proyecto));
        boolean esTransformacion = proyecto.getEstrategiaPetiConfig() != null
                && "TRANSFORMACION_DIGITAL".equalsIgnoreCase(proyecto.getEstrategiaPetiConfig().getCodigo());
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 16), 2, esTransformacion ? "SI" : "NO");
    }

    private Map<String, Map<String, Integer>> poblarSeguimientoProyecto(Sheet sheet, Proyecto proyecto, ProyectoAvanceResponseDTO avance, LocalDate corte) {
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 1), 1, safe(proyecto.getId()) + " " + safe(proyecto.getNombre()));
        List<DetalleSeguimiento> detalles = detallesSeguimiento(proyecto, avance, corte);
        prepararFilasDetalle(sheet, detalles.size());
        MapasSeguimiento mapas = indexarFilasDetalle(detalles);

        int rowIndex = 4;
        for (int i = 0; i < detalles.size(); i++) {
            int excelRow = 5 + i;
            DetalleSeguimiento detalle = detalles.get(i);
            Row row = ExcelSheetSupport.ensureRow(sheet, rowIndex++);

            boolean isFirstInFase = (excelRow == mapas.faseFirstRow().get(detalle.fase()));
            boolean isFirstInHito = (excelRow == mapas.hitoFirstRow().get(detalle.hito()));
            boolean isFirstInProject = (i == 0);

            ExcelSheetSupport.escribirCabeceraAgrupada(row, 1, 2, isFirstInFase, detalle.fase(), detalle.ponderacionFase());
            ExcelSheetSupport.escribirCabeceraAgrupada(row, 3, 4, isFirstInHito, detalle.hito(), detalle.ponderacionHito());

            ExcelSheetSupport.setCellText(row, 5, detalle.entregable());
            ExcelSheetSupport.setCellPercent(row, 6, detalle.ponderacionEntregable());
            ExcelSheetSupport.setCellText(row, 7, detalle.descripcion());
            ExcelSheetSupport.setCellDate(row, 8, toDate(detalle.fechaLimite()));
            ExcelSheetSupport.setCellDate(row, 9, toDate(detalle.fechaEntrega()));
            ExcelSheetSupport.setCellFormula(row, 10, "I" + excelRow + "-J" + excelRow);
            ExcelSheetSupport.setCellFormula(row, 11, "IF(ISNUMBER(J" + excelRow + "),1,0)");
            ExcelSheetSupport.setCellHyperlink(row, 12, detalle.evidencia(), detalle.evidenciaPublica());
            ExcelSheetSupport.setCellText(row, 13, detalle.observacion());

            ExcelSheetSupport.escribirFormulaSuma(row, 14, isFirstInHito, mapas.hitoEntregableRows().get(detalle.hito()), "G", "L");
            ExcelSheetSupport.escribirFormulaSuma(row, 15, isFirstInFase, mapas.faseHitoRows().get(detalle.fase()), "O", "E");
            ExcelSheetSupport.escribirFormulaSuma(row, 16, isFirstInProject, mapas.projectFaseFirstRows(), "P", "C");

            ExcelSheetSupport.setCellFormula(row, 17, "TODAY()");
            ExcelSheetSupport.setCellFormula(row, 18, "I" + excelRow + "-R" + excelRow);
            ExcelSheetSupport.setCellFormula(row, 19, "IF(S" + excelRow + ">0,0,1)");

            ExcelSheetSupport.escribirFormulaSuma(row, 20, isFirstInHito, mapas.hitoEntregableRows().get(detalle.hito()), "G", "T");
            ExcelSheetSupport.escribirFormulaSuma(row, 21, isFirstInFase, mapas.faseHitoRows().get(detalle.fase()), "E", "U");
            ExcelSheetSupport.escribirFormulaSuma(row, 22, isFirstInProject, mapas.projectFaseFirstRows(), "V", "C");

            ExcelSheetSupport.setCellFormula(row, 23, "IF(R" + excelRow + "-I" + excelRow + ">=0,\"Si\",\"No\")");
            ExcelSheetSupport.setCellFormula(row, 24, "IF(AND(L" + excelRow + "=1,X" + excelRow + "=\"Si\"),\"Si\",\"\")");
            ExcelSheetSupport.setCellFormula(row, 25, "IF(AND(K" + excelRow + ">=0,X" + excelRow + "=\"Si\"),\"Si\",\"\")");
        }

        escribirIndicadores(sheet, detalles.size());

        Map<String, Map<String, Integer>> context = new HashMap<>();
        context.put("faseFirstRows", mapas.faseFirstRow());
        context.put("hitoFirstRows", mapas.hitoFirstRow());
        return context;
    }

    private record MapasSeguimiento(
            Map<String, Integer> faseFirstRow,
            Map<String, Integer> hitoFirstRow,
            Map<String, List<Integer>> faseHitoRows,
            Map<String, List<Integer>> hitoEntregableRows,
            List<Integer> projectFaseFirstRows) { }

    private MapasSeguimiento indexarFilasDetalle(List<DetalleSeguimiento> detalles) {
        Map<String, Integer> faseFirstRowMap = new LinkedHashMap<>();
        Map<String, Integer> hitoFirstRowMap = new LinkedHashMap<>();
        Map<String, List<Integer>> faseHitoRowsMap = new LinkedHashMap<>();
        Map<String, List<Integer>> hitoEntregableRowsMap = new LinkedHashMap<>();
        List<Integer> projectFaseFirstRows = new ArrayList<>();

        for (int i = 0; i < detalles.size(); i++) {
            int excelRow = 5 + i;
            DetalleSeguimiento det = detalles.get(i);

            faseFirstRowMap.putIfAbsent(det.fase(), excelRow);
            if (!projectFaseFirstRows.contains(faseFirstRowMap.get(det.fase()))) {
                projectFaseFirstRows.add(faseFirstRowMap.get(det.fase()));
            }

            hitoFirstRowMap.putIfAbsent(det.hito(), excelRow);
            faseHitoRowsMap.computeIfAbsent(det.fase(), k -> new ArrayList<>());
            if (!faseHitoRowsMap.get(det.fase()).contains(hitoFirstRowMap.get(det.hito()))) {
                faseHitoRowsMap.get(det.fase()).add(hitoFirstRowMap.get(det.hito()));
            }

            hitoEntregableRowsMap.computeIfAbsent(det.hito(), k -> new ArrayList<>()).add(excelRow);
        }
        return new MapasSeguimiento(faseFirstRowMap, hitoFirstRowMap, faseHitoRowsMap, hitoEntregableRowsMap, projectFaseFirstRows);
    }

    private void escribirIndicadores(Sheet sheet, int totalDetalles) {
        int totalFilas = Math.max(totalDetalles, 1);
        int lastDataRow = 4 + totalFilas;
        int filaResumen = lastDataRow + 1;
        int indicadorFila = filaResumen + 2;
        int filaCorte = indicadorFila + 1;

        Row rowResumen = ExcelSheetSupport.ensureRow(sheet, filaResumen - 1);
        ExcelSheetSupport.setCellFormula(rowResumen, 24, "COUNTIFS($Y$5:$Y$" + lastDataRow + ",\"Si\",$X$5:$X$" + lastDataRow + ",\"Si\")/COUNTIF($X$5:$X$" + lastDataRow + ",\"Si\")");
        ExcelSheetSupport.setCellFormula(rowResumen, 25, "COUNTIFS($Z$5:$Z$" + lastDataRow + ",\"Si\",$Y$5:$Y$" + lastDataRow + ",\"Si\")/COUNTIF($Y$5:$Y$" + lastDataRow + ",\"Si\")");

        Row titleRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila - 1);
        ExcelSheetSupport.setCellText(titleRow, 23, "INDICADORES AL CORTE");

        Row dateRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila);
        ExcelSheetSupport.setCellText(dateRow, 23, "Fecha de corte");
        ExcelSheetSupport.setCellFormula(dateRow, 24, "R5");
        ExcelSheetSupport.setCellText(dateRow, 25, "Base del cálculo");

        Row progRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 1);
        ExcelSheetSupport.setCellText(progRow, 23, "Programados al corte");
        ExcelSheetSupport.setCellFormula(progRow, 24, "COUNTIFS(I:I,\"<=\"&Y" + filaCorte + ",I:I,\"<>\")");
        ExcelSheetSupport.setCellText(progRow, 25, "Entregables con fecha límite menor o igual a la fecha de corte");

        Row entRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 2);
        ExcelSheetSupport.setCellText(entRow, 23, "Entregados al corte");
        ExcelSheetSupport.setCellFormula(entRow, 24, "COUNTIFS(J:J,\"<=\"&Y" + filaCorte + ",J:J,\"<>\",L:L,1)");
        ExcelSheetSupport.setCellText(entRow, 25, "Programados al corte con OK = 1");

        Row tiempoRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 3);
        ExcelSheetSupport.setCellText(tiempoRow, 23, "Entregados a tiempo");
        ExcelSheetSupport.setCellFormula(tiempoRow, 24, "SUMPRODUCT(--(J5:J" + lastDataRow + "<=Y" + filaCorte + "),--(I5:I" + lastDataRow + "<>\"\"),--(L5:L" + lastDataRow + "=1),--(J5:J" + lastDataRow + "<>\"\"),--(J5:J" + lastDataRow + "<=I5:I" + lastDataRow + "))");
        ExcelSheetSupport.setCellText(tiempoRow, 25, "Entregados al corte con fecha de entrega menor o igual a la fecha límite");

        Row efcRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 4);
        ExcelSheetSupport.setCellText(efcRow, 23, "Eficacia");
        ExcelSheetSupport.setCellFormula(efcRow, 24, "MIN(1,IFERROR(Y" + (indicadorFila + 3) + "/Y" + (indicadorFila + 2) + ",0))");
        ExcelSheetSupport.setCellText(efcRow, 25, "Entregados al corte / Programados al corte");

        Row efiRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 5);
        ExcelSheetSupport.setCellText(efiRow, 23, "Eficiencia");
        ExcelSheetSupport.setCellFormula(efiRow, 24, "MIN(1,IFERROR(Y" + (indicadorFila + 4) + "/Y" + (indicadorFila + 2) + ",0))");
        ExcelSheetSupport.setCellText(efiRow, 25, "Entregados a tiempo / Programados al corte");

        Row totRow = ExcelSheetSupport.ensureRow(sheet, indicadorFila + 6);
        ExcelSheetSupport.setCellText(totRow, 23, "total entregables");
        ExcelSheetSupport.setCellFormula(totRow, 24, "COUNT(I5:I" + lastDataRow + ")");
    }

    private void poblarAvancesProyecto(Sheet sheet, Proyecto proyecto, Map<String, Map<String, Integer>> context) {
        ExcelSheetSupport.setCellText(ExcelSheetSupport.ensureRow(sheet, 1), 1, "AVANCES DEL PROYECTO " + safe(proyecto.getId()));

        Map<String, Integer> faseFirstRowMap = context != null ? context.get("faseFirstRows") : Map.of();
        Map<String, Integer> hitoFirstRowMap = context != null ? context.get("hitoFirstRows") : Map.of();

        Row projRow = ExcelSheetSupport.ensureRow(sheet, 5);
        ExcelSheetSupport.setCellText(projRow, 1, "PROYECTO");
        ExcelSheetSupport.setCellFormula(projRow, 2, "AVANCE!W5");
        ExcelSheetSupport.setCellFormula(projRow, 3, "AVANCE!Q5");
        ExcelSheetSupport.setCellFormula(projRow, 4, "C6-D6");
        ExcelSheetSupport.setCellFormula(projRow, 5, "IF(E6>0,\"ATRASO\",\"EN TIEMPO\")");

        int currentPoiRow = 7;
        Row templateFaseRow = ExcelSheetSupport.plantillaDe(sheet, 7, projRow);

        List<Fase> fases = fasesOrdenadas(proyecto);

        for (Fase fase : fases) {
            currentPoiRow = escribirFilaAvanceFase(sheet, fase, currentPoiRow, templateFaseRow, faseFirstRowMap);
        }

        currentPoiRow++;
        Row templateHitoRow = ExcelSheetSupport.plantillaDe(sheet, 10, templateFaseRow);

        for (Fase fase : fases) {
            for (Hito hito : hitosOrdenados(fase)) {
                currentPoiRow = escribirFilaAvanceHito(sheet, hito, currentPoiRow, templateHitoRow, hitoFirstRowMap);
            }
        }
    }

    private int escribirFilaAvanceFase(Sheet sheet, Fase fase, int poiRow, Row templateRow, Map<String, Integer> primeraFilaFase) {
        int excelRow = poiRow + 1;
        Row row = ExcelSheetSupport.ensureRow(sheet, poiRow);
        if (row != templateRow) ExcelSheetSupport.copiarEstilosFila(templateRow, row, 1, 5);
        ExcelSheetSupport.setCellText(row, 1, safe(fase.getNombre()));

        Integer avanceFila = primeraFilaFase.get(safe(fase.getNombre()));
        if (avanceFila != null) {
            ExcelSheetSupport.setCellFormula(row, 2, "AVANCE!V" + avanceFila);
            ExcelSheetSupport.setCellFormula(row, 3, "AVANCE!P" + avanceFila);
        } else {
            ExcelSheetSupport.setCellPercent(row, 2, BigDecimal.ZERO);
            ExcelSheetSupport.setCellPercent(row, 3, BigDecimal.ZERO);
        }
        ExcelSheetSupport.setCellFormula(row, 4, "C" + excelRow + "-D" + excelRow);
        ExcelSheetSupport.setCellFormula(row, 5, "IF(E" + excelRow + ">0,\"ATRASO\",\"EN TIEMPO\")");
        return poiRow + 1;
    }

    private int escribirFilaAvanceHito(Sheet sheet, Hito hito, int poiRow, Row templateRow, Map<String, Integer> primeraFilaHito) {
        int excelRow = poiRow + 1;
        Row row = ExcelSheetSupport.ensureRow(sheet, poiRow);
        if (row != templateRow) ExcelSheetSupport.copiarEstilosFila(templateRow, row, 1, 5);
        ExcelSheetSupport.setCellText(row, 1, safe(hito.getNombre()));

        Integer avanceHito = primeraFilaHito.get(safe(hito.getNombre()));
        if (avanceHito != null) {
            ExcelSheetSupport.setCellFormula(row, 2, "AVANCE!U" + avanceHito);
            ExcelSheetSupport.setCellFormula(row, 3, "AVANCE!O" + avanceHito);
        } else {
            ExcelSheetSupport.setCellPercent(row, 2, BigDecimal.ZERO);
            ExcelSheetSupport.setCellPercent(row, 3, BigDecimal.ZERO);
        }
        ExcelSheetSupport.setCellFormula(row, 4, "C" + excelRow + "-D" + excelRow);
        ExcelSheetSupport.setCellFormula(row, 5, "IF(E" + excelRow + ">0,\"ATRASO\",\"EN TIEMPO\")");
        return poiRow + 1;
    }


    private void prepararFilasDetalle(Sheet sheet, int totalFilas) {
        int firstDataRow = 4;
        int templateRows = 8;
        if (totalFilas > templateRows) {
            sheet.shiftRows(firstDataRow + templateRows, sheet.getLastRowNum(), totalFilas - templateRows, true, false);
        }
        Row plantilla = sheet.getRow(firstDataRow);
        for (int rowIndex = firstDataRow; rowIndex < firstDataRow + Math.max(templateRows, totalFilas); rowIndex++) {
            Row row = ExcelSheetSupport.ensureRow(sheet, rowIndex);
            if (row == null) continue;
            if (row != plantilla) ExcelSheetSupport.copiarEstilosFila(plantilla, row, 1, 25);
            ExcelSheetSupport.clearTemplateRow(row, 1, 25);
        }
    }

    private List<DetalleSeguimiento> detallesSeguimiento(Proyecto proyecto, ProyectoAvanceResponseDTO avance, LocalDate corte) {
        Map<Integer, FaseAvanceDTO> fases = safeList(avance.fases()).stream()
                .collect(Collectors.toMap(FaseAvanceDTO::id, fase -> fase, (left, right) -> left));
        List<DetalleSeguimiento> detalles = new ArrayList<>();
        for (Fase fase : fasesOrdenadas(proyecto)) {
            FaseAvanceDTO avanceFase = fases.get(fase.getId());
            Map<Integer, HitoAvanceDTO> hitos = hitosAvanceFase(avanceFase);
            for (Hito hito : hitosOrdenados(fase)) {
                HitoAvanceDTO avanceHito = hitos.get(hito.getId());
                for (Entregable entregable : entregablesOrdenados(hito)) {
                    detalles.add(construirDetalleSeguimiento(fase, hito, entregable, avanceFase, avanceHito, avance, corte));
                }
            }
        }
        return detalles;
    }

    private List<Fase> fasesOrdenadas(Proyecto proyecto) {
        return safeList(proyecto.getFases()).stream()
                .sorted(Comparator.comparing(Fase::getId, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    private List<Hito> hitosOrdenados(Fase fase) {
        return safeList(fase.getHitos()).stream()
                .sorted(Comparator.comparing(Hito::getId, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    private List<Entregable> entregablesOrdenados(Hito hito) {
        return safeList(hito.getEntregables()).stream()
                .sorted(Comparator.comparing(Entregable::getId, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    private Map<Integer, HitoAvanceDTO> hitosAvanceFase(FaseAvanceDTO avanceFase) {
        if (avanceFase == null) {
            return Map.of();
        }
        return safeList(avanceFase.hitos()).stream()
                .collect(Collectors.toMap(HitoAvanceDTO::id, hito -> hito, (left, right) -> left));
    }

    private Long calcularDiasAtraso(LocalDate limite, LocalDate entrega, LocalDate corte) {
        if (limite == null) {
            return null;
        }
        if (entrega != null) {
            return entrega.isAfter(limite) ? 0L : ChronoUnit.DAYS.between(entrega, limite);
        }
        if (limite.isBefore(corte)) {
            return ChronoUnit.DAYS.between(corte, limite);
        }
        return 0L;
    }

    private BigDecimal ratioEjecutadoHito(HitoAvanceDTO avanceHito) {
        return avanceHito == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceHito.progresoEjecutado());
    }

    private BigDecimal ratioProgramadoHito(HitoAvanceDTO avanceHito) {
        return avanceHito == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceHito.progresoProgramado());
    }

    private BigDecimal ratioEjecutadoFase(FaseAvanceDTO avanceFase) {
        return avanceFase == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceFase.progresoEjecutado());
    }

    private BigDecimal ratioProgramadoFase(FaseAvanceDTO avanceFase) {
        return avanceFase == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceFase.progresoProgramado());
    }

    private DetalleSeguimiento construirDetalleSeguimiento(Fase fase, Hito hito, Entregable entregable,
                                                           FaseAvanceDTO avanceFase, HitoAvanceDTO avanceHito,
                                                           ProyectoAvanceResponseDTO avance, LocalDate corte) {
        LocalDate limite = entregable.getFechaLimite();
        LocalDate entrega = entregable.getFechaEntregaEfectiva();
        boolean conforme = entregable.esConforme();
        boolean programado = limite != null && !limite.isAfter(corte);
        boolean eficaz = programado && conforme && (entrega == null || !entrega.isAfter(corte));
        boolean eficiente = eficaz && entrega != null && !entrega.isAfter(limite);
        Long diasAtraso = calcularDiasAtraso(limite, entrega, corte);
        String evidenciaPublica = crearUrlEvidenciaPublica(entregable);
        String evidencia = etiquetaEvidencia(entregable);
        return new DetalleSeguimiento(
                safe(fase.getNombre()), ratioDesdePonderacion(fase.getPonderacion()), safe(hito.getNombre()), ratioDesdePonderacion(hito.getPonderacion()),
                safe(entregable.getNombre()), ratioDesdePonderacion(entregable.getPonderacion()), nombreEntregable(entregable), limite, entrega,
                diasAtraso, conforme, evidencia, evidenciaPublica, safe(entregable.getObservacionRevision()),
                ratioEjecutadoHito(avanceHito),
                ratioEjecutadoFase(avanceFase),
                ratioDesdePorcentaje(avance.progresoEjecutado()), diasParaLimite(limite, corte),
                ratioProgramadoHito(avanceHito),
                ratioProgramadoFase(avanceFase),
                ratioDesdePorcentaje(avance.progresoProgramado()), programado, eficaz, eficiente
        );
    }

    private String etiquetaEvidencia(Entregable entregable) {
        return entregable.getArchivoPdf() != null && !entregable.getArchivoPdf().isBlank() ? "Ver evidencia" : "";
    }

    private Long diasParaLimite(LocalDate limite, LocalDate corte) {
        return limite == null ? null : ChronoUnit.DAYS.between(corte, limite);
    }

    private String objetivosEspecificos(Proyecto proyecto) {
        return safeList(proyecto.getObjetivosEspecificos()).stream()
                .sorted(Comparator.comparing(ObjetivoEspecifico::getOrden, Comparator.nullsLast(Short::compareTo)))
                .map(ObjetivoEspecifico::getDescripcion)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private LocalDate fechaFinProyecto(Proyecto proyecto) {
        return safeList(proyecto.getFases()).stream().flatMap(fase -> safeList(fase.getHitos()).stream())
                .flatMap(hito -> safeList(hito.getEntregables()).stream()).map(Entregable::getFechaLimite)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
    }

    private String estrategiaPeti(Proyecto proyecto) {
        if (proyecto.getEstrategiaPetiConfig() != null) return safe(proyecto.getEstrategiaPetiConfig().getNombre());
        return "";
    }

    private String codigoEstrategiaPeti(Proyecto proyecto) {
        if (proyecto.getEstrategiaPetiConfig() != null) return proyecto.getEstrategiaPetiConfig().getCodigo();
        return null;
    }

    private String nombreEntregable(Entregable entregable) {
        String descripcion = safe(entregable.getDescripcion());
        return descripcion.isBlank() ? safe(entregable.getNombre()) : descripcion;
    }

    private String crearUrlEvidenciaPublica(Entregable entregable) {
        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) return "";
        String token = publicEvidenceAccessService.getOrCreateToken(entregable, "reporte-excel");
        String base = safe(publicUrlProperties.getBase());
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v1/public/evidencia/" + token;
    }

    private String formatYear(LocalDate date) { return date == null ? "" : String.valueOf(date.getYear()); }
    private LocalDateTime toDate(LocalDate date) { return date == null ? null : date.atStartOfDay(); }
    private BigDecimal safeDecimal(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal ratioDesdePorcentaje(BigDecimal value) { return safeDecimal(value).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP); }
    private BigDecimal ratioDesdePonderacion(BigDecimal value) { return value != null && value.compareTo(BigDecimal.ONE) > 0 ? ratioDesdePorcentaje(value) : safeDecimal(value); }
    private <T> List<T> safeList(List<T> values) { return values == null ? List.of() : values; }

    private record DetalleSeguimiento(
            String fase, BigDecimal ponderacionFase, String hito, BigDecimal ponderacionHito, String entregable,
            BigDecimal ponderacionEntregable, String descripcion, LocalDate fechaLimite, LocalDate fechaEntrega,
            Long diasAtraso, boolean conforme, String evidencia, String evidenciaPublica, String observacion, BigDecimal ejecutadoHito,
            BigDecimal ejecutadoFase, BigDecimal ejecutadoProyecto, Long diasParaLimite, BigDecimal programadoHito,
            BigDecimal programadoFase, BigDecimal programadoProyecto, boolean programadoAlCorte, boolean eficaz, boolean eficiente) { }

    private List<Proyecto> obtenerProyectosPortafolio(Authentication authentication) {
        if (authentication != null) {
            try {
                if (proyectoSecurity.canAccessGlobal("PROYECTO:VER", authentication)) {
                    return proyectoRepositoryPort.findAll().stream()
                            .filter(p -> !esPendienteCompletar(p))
                            .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                            .toList();
                }
            } catch (RuntimeException ex) {
                // CWE-390: degradar a scope menor es el camino seguro, pero el fallo
                // del chequeo de autorizacion jamas debe quedar sin registro.
                log.warn("No se pudo evaluar acceso global para portafolio; "
                        + "se restringe al alcance de proyectos asignados. causa={}", ex.toString());
            }
        }

        String username = identityExtractor.resolveUsername(authentication);
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<String> proyectoIds = seguridadUsuarioProyectoRepositoryPort.findProyectoIdsByUsername(username).stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (proyectoIds.isEmpty()) {
            return List.of();
        }

        return proyectoRepositoryPort.findAllById(proyectoIds).stream()
                .filter(p -> !esPendienteCompletar(p))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<Proyecto> aplicarFiltrosPortafolio(List<Proyecto> proyectos, String query, String dependency, String status, String peti) {
        String normalizedQuery = normalizeText(query);
        String normalizedDependency = normalizeText(dependency);
        String normalizedPeti = normalizeText(peti);

        final Set<String> allowedStatuses;
        if (status != null && !status.isBlank()) {
            allowedStatuses = Arrays.stream(status.split(","))
                    .map(s -> normalizeText(s.trim()))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
        } else {
            allowedStatuses = null;
        }

        return proyectos.stream()
                .filter(proyecto -> {
                    String projectStatus = proyecto.getEstado() != null ? proyecto.getEstado().name() : proyecto.getEstadoCodigo();
                    String searchHaystack = String.join(" ",
                            safe(proyecto.getId()),
                            safe(proyecto.getNombre()),
                            safe(proyecto.getObjetivoGeneral()),
                            safe(proyecto.getDependencia()),
                            safe(resolveDirectorAsignado(proyecto)),
                            safe(projectStatus),
                            Boolean.TRUE.equals(proyecto.getPeti()) ? "PETI" : "NO PETI");

                    boolean matchesQuery = normalizedQuery.isBlank() || normalizeText(searchHaystack).contains(normalizedQuery);
                    boolean matchesDependency = normalizedDependency.isBlank()
                            || normalizeText(proyecto.getDependencia()).equals(normalizedDependency);
                    boolean matchesStatus = allowedStatuses == null || allowedStatuses.isEmpty()
                            || allowedStatuses.contains(normalizeText(projectStatus))
                            || allowedStatuses.contains(normalizeText(proyecto.getEstadoCodigo()));
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
        Row titleRow = ExcelSheetSupport.ensureRow(sheet, 1);
        ExcelSheetSupport.setCellText(titleRow, 2, "SEGUIMIENTO PROYECTOS PETI 2024 - 2028");
        ExcelSheetSupport.setCellText(titleRow, 3, "FECHA DE CORTE");

        Row dateRow = ExcelSheetSupport.ensureRow(sheet, 2);
        ExcelSheetSupport.setCellBlank(dateRow, 2);
        ExcelSheetSupport.setCellDate(dateRow, 3, corte.atStartOfDay());

        Row headerRow = ExcelSheetSupport.ensureRow(sheet, 3);
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
            ExcelSheetSupport.setCellText(headerRow, i + 1, headers[i]);
        }

        int dataStartRow = 4;
        int templateRows = 16;
        int visibleRows = Math.min(templateRows, snapshots.size());
        for (int index = 0; index < templateRows; index++) {
            Row row = ExcelSheetSupport.ensureRow(sheet, dataStartRow + index);
            if (index < visibleRows) {
                RowSnapshot snapshot = snapshots.get(index);
                ExcelSheetSupport.setCellText(row, 1, snapshot.codigo());
                ExcelSheetSupport.setCellText(row, 2, snapshot.nombre());
                ExcelSheetSupport.setCellText(row, 3, snapshot.meta());
                ExcelSheetSupport.setCellPercent(row, 4, snapshot.programado());
                ExcelSheetSupport.setCellPercent(row, 5, snapshot.avance());
                ExcelSheetSupport.setCellPercent(row, 6, snapshot.diferencia());
                ExcelSheetSupport.setCellText(row, 7, snapshot.estado());
                ExcelSheetSupport.setCellNumber(row, 8, snapshot.totalEntregables());
                ExcelSheetSupport.setCellNumber(row, 9, snapshot.entregablesProgramadosAlCorte());
                ExcelSheetSupport.setCellNumber(row, 10, snapshot.entregablesEntregadosAlCorte());
                ExcelSheetSupport.setCellPercent(row, 11, snapshot.eficacia());
                ExcelSheetSupport.setCellPercent(row, 12, snapshot.eficiencia());
                ExcelSheetSupport.setCellText(row, 13, snapshot.dependencia());
                ExcelSheetSupport.setCellText(row, 14, snapshot.responsable());
            } else {
                ExcelSheetSupport.clearTemplateRow(row, 1, 14);
            }
        }

        Row summaryRow = ExcelSheetSupport.ensureRow(sheet, 20);
        ExcelSheetSupport.setCellText(summaryRow, 1, "Promedios");
        ExcelSheetSupport.setCellPercent(summaryRow, 2, promedio(snapshots.stream().map(RowSnapshot::programado).toList()));
        ExcelSheetSupport.setCellPercent(summaryRow, 3, promedio(snapshots.stream().map(RowSnapshot::avance).toList()));
        ExcelSheetSupport.setCellPercent(summaryRow, 4, promedio(snapshots.stream().map(RowSnapshot::diferencia).toList()));
        ExcelSheetSupport.setCellNumber(summaryRow, 5, sumaEntera(snapshots.stream().map(RowSnapshot::totalEntregables).toList()));
        ExcelSheetSupport.setCellNumber(summaryRow, 6, sumaEntera(snapshots.stream().map(RowSnapshot::entregablesProgramadosAlCorte).toList()));
        ExcelSheetSupport.setCellNumber(summaryRow, 7, sumaEntera(snapshots.stream().map(RowSnapshot::entregablesEntregadosAlCorte).toList()));
        ExcelSheetSupport.setCellPercent(summaryRow, 8, promedio(snapshots.stream().map(RowSnapshot::eficacia).toList()));
        ExcelSheetSupport.setCellPercent(summaryRow, 9, promedio(snapshots.stream().map(RowSnapshot::eficiencia).toList()));
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
                safe(resolveDirectorAsignado(proyecto))
        );
    }




    private BigDecimal promedio(List<BigDecimal> values) {
        List<BigDecimal> safeValues = values.stream().filter(java.util.Objects::nonNull).toList();
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

    private boolean sameText(String left, String right) {
        return normalizeText(left).equals(normalizeText(right));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeOrDefault(String value) {
        return value == null || value.isBlank() ? "No asignado" : value.trim();
    }

    private String resolveDirectorAsignado(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return null;
        }
        SeguridadUsuarioProyecto assignment = seguridadUsuarioProyectoRepositoryPort
                .findActiveDirectorAssignmentsByProyectoId(proyecto.getId())
                .stream()
                .filter(item -> item.getUsuario() != null)
                .findFirst()
                .orElse(null);

        if (assignment == null || assignment.getUsuario() == null) {
            return null;
        }

        return firstNonBlank(
                assignment.getUsuario().getNombre(),
                assignment.getUsuario().getUsername()
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean esTratamientoDiligenciado(com.proyecta.api_gestion.domain.model.Riesgo riesgo) {
        if (riesgo == null) {
            return false;
        }
        boolean tieneTratamiento = riesgo.getTratamiento() != null && !riesgo.getTratamiento().trim().isEmpty();
        return tieneTratamiento && EstadoRiesgo.TRATADO.equals(riesgo.getEstado());
    }

    private boolean esPendienteCompletar(Proyecto proyecto) {
        if (proyecto.getEstadoConfig() != null) {
            return "PENDIENTE_COMPLETAR".equals(proyecto.getEstadoConfig().getCodigo());
        }
        return EstadoProyecto.PENDIENTE_COMPLETAR.equals(proyecto.getEstado());
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
        return proyectoRepositoryPort.findById(proyectoId)
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


    private List<Entregable> obtenerEntregablesProyecto(String proyectoId) {
        return entregableRepositoryPort.findByProyectoId(proyectoId).stream()
                .sorted(this::compararEntregables)
                .toList();
    }

    private int compararEntregables(Entregable a, Entregable b) {
        int cmp = compararFechasLimite(a.getFechaLimite(), b.getFechaLimite());
        if (cmp != 0) {
            return cmp;
        }
        return compararNombres(a.getNombre(), b.getNombre());
    }

    private int compararFechasLimite(LocalDate izquierda, LocalDate derecha) {
        if (izquierda == null && derecha == null) return 0;
        if (izquierda == null) return 1;
        if (derecha == null) return -1;
        return izquierda.compareTo(derecha);
    }

    private int compararNombres(String izquierda, String derecha) {
        if (izquierda == null && derecha == null) return 0;
        if (izquierda == null) return 1;
        if (derecha == null) return -1;
        return izquierda.compareToIgnoreCase(derecha);
    }

    private boolean esEntregablePendienteVencido(Entregable entregable) {
        if (entregable == null || entregable.getFechaLimite() == null) {
            return false;
        }
        if (esEntregableConforme(entregable)) {
            return false;
        }
        return entregable.getFechaLimite().isBefore(LocalDate.now(ZoneId.systemDefault()));
    }

    private boolean esEntregableConforme(Entregable entregable) {
        if (entregable == null) {
            return false;
        }
        return EstadoEntregable.COMPLETADO.equals(entregable.getEstado())
                || EstadoEntregable.APROBADO.equals(entregable.getEstado())
                || Boolean.TRUE.equals(entregable.getConforme());
    }






}

