package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
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
import com.proyecta.api_gestion.service.report.SimplePdfReportBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.common.usermodel.HyperlinkType;
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
import java.time.temporal.ChronoUnit;
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
    private final ProjectProgressMetricsService progressMetricsService;
    private final SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ProyectoSecurity proyectoSecurity;
    private final PublicEvidenceAccessService publicEvidenceAccessService;
    private final PublicUrlProperties publicUrlProperties;

    public ReporteServiceImpl(ProyectoRepository proyectoRepository,
                              EntregableRepository entregableRepository,
                              RiesgoRepository riesgoRepository,
                              ReporteConfigRepository reporteConfigRepository,
                              ProjectProgressMetricsService progressMetricsService,
                              SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository,
                              KeycloakIdentityExtractor identityExtractor,
                              ProyectoSecurity proyectoSecurity,
                              PublicEvidenceAccessService publicEvidenceAccessService,
                              PublicUrlProperties publicUrlProperties) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.reporteConfigRepository = reporteConfigRepository;
        this.progressMetricsService = progressMetricsService;
        this.seguridadUsuarioProyectoRepository = seguridadUsuarioProyectoRepository;
        this.identityExtractor = identityExtractor;
        this.proyectoSecurity = proyectoSecurity;
        this.publicEvidenceAccessService = publicEvidenceAccessService;
        this.publicUrlProperties = publicUrlProperties;
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
        return proyectoRepository.getProyectosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos() {
        return proyectoRepository.getProyectosConAtrasosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuragReporteDTO> obtenerFurag(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p -> {
            return new FuragReporteDTO(
                    p.getId(),
                    p.getNombre(),
                    p.getPeti(),
                    p.getEstrategiaPetiConfig() != null
                            ? p.getEstrategiaPetiConfig().getCodigo()
                            : p.getEstrategiaPeti() != null ? p.getEstrategiaPeti().name() : null,
                    p.getVigenciaPeti(),
                    p.getObjetivoGeneral()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoVerificacionReporteDTO> obtenerVerificacionRiesgos() {
        List<Proyecto> proyectosCierre = proyectoRepository.findAll().stream()
                .filter(Proyecto::esEstadoTerminal)
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        Map<String, List<com.proyecta.api_gestion.model.Riesgo>> riesgosPorProyecto = riesgoRepository.findAll().stream()
                .filter(r -> r.getProyecto() != null && r.getProyecto().getId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getProyecto().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return proyectosCierre.stream()
                .map(proyecto -> {
                    List<com.proyecta.api_gestion.model.Riesgo> riesgosProyecto =
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
    public byte[] generarReportePortafolioPdf(String detailMode) {
        List<ProyectoReporteResumenDTO> proyectos = obtenerTodosLosProyectos();
        return new EstadoTodosProyectosPdfGenerator().build(proyectos, LocalDate.now(), detailMode);
    }

    @Override
    public byte[] generarReporteProyectosConRetrasosPdf(String detailMode) {
        List<ProyectoReporteResumenDTO> proyectos = obtenerProyectosConRetrasos();
        List<EntregablePendienteDTO> entregables = proyectos.isEmpty()
                ? List.of()
                : entregableRepository.findPendientesVencidosByProyecto(proyectos.get(0).id(), LocalDate.now()).stream()
                .limit(3)
                .toList();
        return new ProyectosConRetrasosPdfGenerator().build(proyectos, entregables, LocalDate.now(), detailMode);
    }

    @Override
    public byte[] generarReportePlanComunicacionesPdf(String detailMode) {
        List<Proyecto> proyectos = proyectoRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getPeti()))
                .filter(p -> !esPendienteCompletar(p))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new PlanComunicacionesPdfGenerator().build(proyectos, LocalDate.now(), detailMode);
    }

    @Override
    public byte[] generarReporteFuragPdf(String proyectoId, String detailMode) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        String dependencia = safe(proyecto.getDependencia());

        List<Proyecto> proyectosDependencia = proyectoRepository.findAll().stream()
                .filter(p -> sameText(p.getDependencia(), dependencia))
                .filter(p -> !esPendienteCompletar(p))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new FuragPdfGenerator().build(proyecto, proyectosDependencia, LocalDate.now(), detailMode);
    }

    @Override
    public byte[] generarReporteRiesgosPdf(String detailMode) {
        List<RiesgoVerificacionReporteDTO> reportes = obtenerVerificacionRiesgos();
        return new RiesgosVerificacionPdfGenerator().build(reportes, LocalDate.now(), detailMode);
    }

    @Override
    public byte[] generarReportePortafolioExcel() {
        return construirExcelPortafolio(
                proyectoRepository.findAll().stream()
                        .filter(p -> !esPendienteCompletar(p))
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

    @Override
    @Transactional
    public byte[] generarReporteActualProyectoExcel(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        LocalDate corte = LocalDate.now();
        ProyectoAvanceResponseDTO avance = progressMetricsService.construir(proyecto, corte);

        try (
                InputStream templateStream = new ClassPathResource("report-assets/Reporte actual proyecto PETI.xlsx").getInputStream();
                Workbook workbook = new XSSFWorkbook(templateStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            poblarFichaProyecto(workbook.getSheetAt(0), proyecto);
            poblarSeguimientoProyecto(workbook.getSheetAt(1), proyecto, avance, corte);
            poblarAvancesProyecto(workbook.getSheetAt(2), proyecto, avance);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el reporte actual Excel del proyecto.", ex);
        }
    }

    private void poblarFichaProyecto(Sheet sheet, Proyecto proyecto) {
        setCellText(ensureRow(sheet, 1), 2, safe(proyecto.getId()));
        setCellText(ensureRow(sheet, 2), 2, safe(proyecto.getNombre()));
        setCellText(ensureRow(sheet, 3), 2, safe(proyecto.getAlcanceDetallado()));
        setCellText(ensureRow(sheet, 4), 2, proyecto.getPatrocinador() == null ? "" : safe(proyecto.getPatrocinador().getNombre()));
        setCellText(ensureRow(sheet, 5), 2, "");
        setCellText(ensureRow(sheet, 6), 2, safe(proyecto.getDependencia()));
        setCellText(ensureRow(sheet, 7), 2, safe(resolveDirectorAsignado(proyecto)));
        setCellText(ensureRow(sheet, 8), 2, safe(proyecto.getCorreoDirector()));
        setCellText(ensureRow(sheet, 9), 2, safe(proyecto.getObjetivoGeneral()));
        setCellText(ensureRow(sheet, 10), 2, objetivosEspecificos(proyecto));
        setCellText(ensureRow(sheet, 11), 2, formatYear(proyecto.getFechaInicio()));
        setCellText(ensureRow(sheet, 12), 2, formatYear(fechaFinProyecto(proyecto)));
        setCellNumber(ensureRow(sheet, 13), 2, proyecto.getPresupuestoEstimado());
        setCellText(ensureRow(sheet, 14), 2, Boolean.TRUE.equals(proyecto.getPeti()) ? "SI" : "NO");
        setCellText(ensureRow(sheet, 15), 2, estrategiaPeti(proyecto));
        setCellText(ensureRow(sheet, 16), 2, "No registrado");
    }

    private void poblarSeguimientoProyecto(Sheet sheet, Proyecto proyecto, ProyectoAvanceResponseDTO avance, LocalDate corte) {
        setCellText(ensureRow(sheet, 1), 1, safe(proyecto.getId()) + " " + safe(proyecto.getNombre()));
        List<DetalleSeguimiento> detalles = detallesSeguimiento(proyecto, avance, corte);
        prepararFilasDetalle(sheet, detalles.size());

        int rowIndex = 4;
        for (DetalleSeguimiento detalle : detalles) {
            Row row = ensureRow(sheet, rowIndex++);
            setCellText(row, 1, detalle.fase());
            setCellPercent(row, 2, detalle.ponderacionFase());
            setCellText(row, 3, detalle.hito());
            setCellPercent(row, 4, detalle.ponderacionHito());
            setCellText(row, 5, detalle.entregable());
            setCellPercent(row, 6, detalle.ponderacionEntregable());
            setCellText(row, 7, detalle.descripcion());
            setCellDate(row, 8, toDate(detalle.fechaLimite()));
            setCellDate(row, 9, toDate(detalle.fechaEntrega()));
            setCellFormula(row, 10, formulaAtraso(rowIndex));
            setCellFormula(row, 11, "IF(J" + rowIndex + "=\"\",0,1)");
            setCellHyperlink(row, 12, detalle.evidencia(), detalle.evidenciaPublica());
            setCellText(row, 13, detalle.observacion());
            setCellFormula(row, 14, formulaPromedioPorHito(rowIndex, detalles.size(), "L"));
            setCellFormula(row, 15, formulaPromedioPorFase(rowIndex, detalles.size(), "O"));
            setCellFormula(row, 16, "AVERAGE($P$5:$P$" + (4 + Math.max(detalles.size(), 1)) + ")");
            setCellDate(row, 17, toDate(corte));
            setCellFormula(row, 18, "IF(I" + rowIndex + "=\"\",\"\",I" + rowIndex + "-R" + rowIndex + ")");
            setCellFormula(row, 19, "IF(AND(J" + rowIndex + "<>\"\",J" + rowIndex + "<=I" + rowIndex + "),1,0)");
            setCellFormula(row, 20, formulaPromedioPorHito(rowIndex, detalles.size(), "T"));
            setCellFormula(row, 21, formulaPromedioPorFase(rowIndex, detalles.size(), "U"));
            setCellFormula(row, 22, "AVERAGE($V$5:$V$" + (4 + Math.max(detalles.size(), 1)) + ")");
            setCellFormula(row, 23, "IF(I" + rowIndex + "<=R" + rowIndex + ",\"Si\",\"No\")");
            setCellFormula(row, 24, "IF(AND(I" + rowIndex + "<=R" + rowIndex + ",L" + rowIndex + "=1),\"Si\",\"\")");
            setCellFormula(row, 25, "IF(AND(Y" + rowIndex + "=\"Si\",J" + rowIndex + "<=I" + rowIndex + "),\"Si\",\"\")");
        }

        int indicadorFila = 4 + Math.max(20, detalles.size()) + 2;
        Row title = ensureRow(sheet, indicadorFila);
        setCellText(title, 23, "INDICADORES AL CORTE");
        setCellText(ensureRow(sheet, indicadorFila + 1), 23, "Fecha de corte");
        setCellDate(ensureRow(sheet, indicadorFila + 1), 24, toDate(corte));
        setCellText(ensureRow(sheet, indicadorFila + 2), 23, "Programados al corte");
        setCellFormula(ensureRow(sheet, indicadorFila + 2), 24, "COUNTIF($X$5:$X$" + (4 + Math.max(detalles.size(), 1)) + ",\"Si\")");
        setCellText(ensureRow(sheet, indicadorFila + 3), 23, "Entregados al corte");
        setCellFormula(ensureRow(sheet, indicadorFila + 3), 24, "COUNTIF($Y$5:$Y$" + (4 + Math.max(detalles.size(), 1)) + ",\"Si\")");
        setCellText(ensureRow(sheet, indicadorFila + 4), 23, "Entregados a tiempo");
        setCellFormula(ensureRow(sheet, indicadorFila + 4), 24, "COUNTIF($Z$5:$Z$" + (4 + Math.max(detalles.size(), 1)) + ",\"Si\")");
        setCellText(ensureRow(sheet, indicadorFila + 5), 23, "Eficacia");
        setCellFormula(ensureRow(sheet, indicadorFila + 5), 24, "IFERROR(Y" + (indicadorFila + 4) + "/Y" + (indicadorFila + 3) + ",0)");
        setCellText(ensureRow(sheet, indicadorFila + 6), 23, "Eficiencia");
        setCellFormula(ensureRow(sheet, indicadorFila + 6), 24, "IFERROR(Y" + (indicadorFila + 5) + "/Y" + (indicadorFila + 4) + ",0)");
        setCellText(ensureRow(sheet, indicadorFila + 7), 23, "total entregables");
        setCellFormula(ensureRow(sheet, indicadorFila + 7), 24, "COUNTA($F$5:$F$" + (4 + Math.max(detalles.size(), 1)) + ")");
    }

    private void poblarAvancesProyecto(Sheet sheet, Proyecto proyecto, ProyectoAvanceResponseDTO avance) {
        setCellText(ensureRow(sheet, 3), 2, "AVANCES DEL PROYECTO " + safe(proyecto.getId()));
        int rowIndex = 7;
        limpiarFilas(sheet, rowIndex, sheet.getLastRowNum(), 2, 6);
        rowIndex = escribirAvance(sheet, rowIndex, "PROYECTO", avance.progresoProgramado(), avance.progresoEjecutado(), avance.estado());
        for (FaseAvanceDTO fase : safeList(avance.fases())) {
            rowIndex = escribirAvance(sheet, rowIndex + 1, safe(fase.nombre()), fase.progresoProgramado(), fase.progresoEjecutado(), fase.estado());
            for (HitoAvanceDTO hito : safeList(fase.hitos())) {
                rowIndex = escribirAvance(sheet, rowIndex, safe(hito.nombre()), hito.progresoProgramado(), hito.progresoEjecutado(), hito.estado());
            }
        }
    }

    private int escribirAvance(Sheet sheet, int rowIndex, String nombre, BigDecimal programado, BigDecimal ejecutado, String estado) {
        Row row = ensureRow(sheet, rowIndex);
        setCellText(row, 2, nombre);
        setCellPercent(row, 3, ratioDesdePorcentaje(programado));
        setCellPercent(row, 4, ratioDesdePorcentaje(ejecutado));
        setCellPercent(row, 5, ratioDesdePorcentaje(safeDecimal(programado).subtract(safeDecimal(ejecutado))));
        setCellText(row, 6, safe(estado).replace('_', ' '));
        return rowIndex + 1;
    }

    private void prepararFilasDetalle(Sheet sheet, int totalFilas) {
        int firstDataRow = 4;
        int templateRows = 20;
        if (totalFilas > templateRows) {
            sheet.shiftRows(firstDataRow + templateRows, sheet.getLastRowNum(), totalFilas - templateRows, true, false);
        }
        Row plantilla = sheet.getRow(firstDataRow);
        for (int rowIndex = firstDataRow; rowIndex < firstDataRow + Math.max(templateRows, totalFilas); rowIndex++) {
            Row row = ensureRow(sheet, rowIndex);
            if (row != plantilla) copiarEstilosFila(plantilla, row, 1, 25);
            clearTemplateRow(row, 1, 25);
        }
    }

    private void copiarEstilosFila(Row source, Row target, int fromColumnInclusive, int toColumnInclusive) {
        if (source == null) return;
        target.setHeight(source.getHeight());
        for (int column = fromColumnInclusive; column <= toColumnInclusive; column++) {
            Cell sourceCell = source.getCell(column);
            Cell targetCell = target.getCell(column);
            if (targetCell == null) targetCell = target.createCell(column);
            if (sourceCell != null) targetCell.setCellStyle(sourceCell.getCellStyle());
        }
    }

    private String formulaAtraso(int row) {
        return "IF(I" + row + "=\"\",\"\",IF(J" + row + "=\"\",R" + row + "-I" + row + ",I" + row + "-J" + row + "))";
    }

    private String formulaPromedioPorHito(int row, int totalFilas, String column) {
        int lastRow = 4 + Math.max(totalFilas, 1);
        return "IFERROR(AVERAGEIFS($" + column + "$5:$" + column + "$" + lastRow + ",$D$5:$D$" + lastRow + ",D" + row + "),0)";
    }

    private String formulaPromedioPorFase(int row, int totalFilas, String column) {
        int lastRow = 4 + Math.max(totalFilas, 1);
        return "IFERROR(AVERAGEIFS($" + column + "$5:$" + column + "$" + lastRow + ",$B$5:$B$" + lastRow + ",B" + row + "),0)";
    }

    private void limpiarFilas(Sheet sheet, int fromRow, int toRow, int fromColumn, int toColumn) {
        for (int row = fromRow; row <= toRow; row++) clearTemplateRow(ensureRow(sheet, row), fromColumn, toColumn);
    }

    private List<DetalleSeguimiento> detallesSeguimiento(Proyecto proyecto, ProyectoAvanceResponseDTO avance, LocalDate corte) {
        Map<Integer, FaseAvanceDTO> fases = safeList(avance.fases()).stream()
                .collect(Collectors.toMap(FaseAvanceDTO::id, fase -> fase, (left, right) -> left));
        List<DetalleSeguimiento> detalles = new ArrayList<>();
        for (Fase fase : safeList(proyecto.getFases()).stream().sorted(Comparator.comparing(Fase::getId, Comparator.nullsLast(Integer::compareTo))).toList()) {
            FaseAvanceDTO avanceFase = fases.get(fase.getId());
            Map<Integer, HitoAvanceDTO> hitos = avanceFase == null ? Map.of() : safeList(avanceFase.hitos()).stream()
                    .collect(Collectors.toMap(HitoAvanceDTO::id, hito -> hito, (left, right) -> left));
            for (Hito hito : safeList(fase.getHitos()).stream().sorted(Comparator.comparing(Hito::getId, Comparator.nullsLast(Integer::compareTo))).toList()) {
                HitoAvanceDTO avanceHito = hitos.get(hito.getId());
                for (Entregable entregable : safeList(hito.getEntregables()).stream().sorted(Comparator.comparing(Entregable::getId, Comparator.nullsLast(Integer::compareTo))).toList()) {
                    LocalDate limite = entregable.getFechaLimite();
                    LocalDate entrega = entregable.getFechaEntregaReal();
                    boolean conforme = entregable.esConforme();
                    boolean programado = limite != null && !limite.isAfter(corte);
                    boolean eficaz = programado && conforme && (entrega == null || !entrega.isAfter(corte));
                    boolean eficiente = eficaz && entrega != null && !entrega.isAfter(limite);
                    Long diasAtraso = limite == null ? null : ChronoUnit.DAYS.between(limite, entrega == null ? corte : entrega);
                    String evidenciaPublica = crearUrlEvidenciaPublica(entregable);
                    detalles.add(new DetalleSeguimiento(
                            safe(fase.getNombre()), ratioDesdePonderacion(fase.getPonderacion()), safe(hito.getNombre()), ratioDesdePonderacion(hito.getPonderacion()),
                            safe(entregable.getNombre()), ratioDesdePonderacion(entregable.getPonderacion()), nombreEntregable(entregable), limite, entrega,
                            diasAtraso, conforme, safe(entregable.getArchivoPdf()), evidenciaPublica, safe(entregable.getObservacionRevision()),
                            avanceHito == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceHito.progresoEjecutado()),
                            avanceFase == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceFase.progresoEjecutado()),
                            ratioDesdePorcentaje(avance.progresoEjecutado()), limite == null ? null : ChronoUnit.DAYS.between(corte, limite),
                            avanceHito == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceHito.progresoProgramado()),
                            avanceFase == null ? BigDecimal.ZERO : ratioDesdePorcentaje(avanceFase.progresoProgramado()),
                            ratioDesdePorcentaje(avance.progresoProgramado()), programado, eficaz, eficiente
                    ));
                }
            }
        }
        return detalles;
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
        return proyecto.getEstrategiaPeti() == null ? "" : proyecto.getEstrategiaPeti().name().replace('_', ' ');
    }

    private String nombreEntregable(Entregable entregable) {
        String descripcion = safe(entregable.getDescripcion());
        return descripcion.isBlank() ? safe(entregable.getNombre()) : descripcion;
    }

    private String crearUrlEvidenciaPublica(Entregable entregable) {
        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) return "";
        String token = publicEvidenceAccessService.getOrCreateToken(entregable, "reporte-excel");
        String base = safe(publicUrlProperties.getBase()).replaceAll("/+$", "");
        return base + "/api/v1/public/evidencia/" + token;
    }

    private String formatYear(LocalDate date) { return date == null ? "" : String.valueOf(date.getYear()); }
    private Date toDate(LocalDate date) { return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()); }
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
                    return proyectoRepository.findAll().stream()
                            .filter(p -> !esPendienteCompletar(p))
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
                .filter(p -> !esPendienteCompletar(p))
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
                            safe(resolveDirectorAsignado(proyecto)),
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

    private void setCellFormula(Row row, int columnIndex, String formula) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) cell = row.createCell(columnIndex);
        cell.setCellFormula(formula);
    }

    private void setCellHyperlink(Row row, int columnIndex, String label, String url) {
        setCellText(row, columnIndex, label);
        if (url == null || url.isBlank()) return;
        Cell cell = row.getCell(columnIndex);
        Hyperlink hyperlink = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(url);
        cell.setHyperlink(hyperlink);
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
        SeguridadUsuarioProyecto assignment = seguridadUsuarioProyectoRepository
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

    private boolean esTratamientoDiligenciado(com.proyecta.api_gestion.model.Riesgo riesgo) {
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
                || EstadoEntregable.APROBADO.equals(entregable.getEstado())
                || Boolean.TRUE.equals(entregable.getConforme());
    }

    private String estadoEntregable(Entregable entregable) {
        if (entregable == null) {
            return "No disponible";
        }
        if (EstadoEntregable.APROBADO.equals(entregable.getEstado()) || Boolean.TRUE.equals(entregable.getConforme())) {
            return "Aprobado";
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
