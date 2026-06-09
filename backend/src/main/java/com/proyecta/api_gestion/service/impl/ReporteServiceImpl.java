package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.report.*;
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
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
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
    private final ProjectProgressMetricsService progressMetricsService;
    private final SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ProyectoSecurity proyectoSecurity;

    public ReporteServiceImpl(ProyectoRepository proyectoRepository,
                              EntregableRepository entregableRepository,
                              RiesgoRepository riesgoRepository,
                              ReporteConfigRepository reporteConfigRepository,
                              ProjectProgressMetricsService progressMetricsService,
                              SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository,
                              KeycloakIdentityExtractor identityExtractor,
                              ProyectoSecurity proyectoSecurity) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.reporteConfigRepository = reporteConfigRepository;
        this.progressMetricsService = progressMetricsService;
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
    public Optional<FuragReporteDTO> obtenerFurag(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p -> {
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
                            safeOrDefault(proyecto.getDirector()),
                            safeOrDefault(proyecto.getDependencia()),
                            diligencioTratamiento
                    );
                })
                .toList();
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
        return new EstadoProyectoEspecificoPdfGenerator().build(
                proyecto,
                fases,
                objetivos,
                pendientesVencidos,
                conformes
        );
    }

    @Override
    public byte[] generarReportePortafolioPdf() {
        List<ProyectoReporteResumenDTO> proyectos = obtenerTodosLosProyectos();
        return new EstadoTodosProyectosPdfGenerator().build(proyectos, LocalDate.now());
    }

    @Override
    public byte[] generarReporteProyectosConRetrasosPdf() {
        List<ProyectoReporteResumenDTO> proyectos = obtenerProyectosConRetrasos();
        List<EntregablePendienteDTO> entregables = proyectos.isEmpty()
                ? List.of()
                : entregableRepository.findPendientesVencidosByProyecto(proyectos.get(0).id(), LocalDate.now()).stream()
                .limit(3)
                .toList();
        return new ProyectosConRetrasosPdfGenerator().build(proyectos, entregables, LocalDate.now());
    }

    @Override
    public byte[] generarReportePlanComunicacionesPdf() {
        List<Proyecto> proyectos = proyectoRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getPeti()))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new PlanComunicacionesPdfGenerator().build(proyectos, LocalDate.now());
    }

    @Override
    public byte[] generarReporteFuragPdf(String proyectoId) {
        Proyecto proyecto = cargarProyecto(proyectoId);
        String dependencia = safe(proyecto.getDependencia());

        List<Proyecto> proyectosDependencia = proyectoRepository.findAll().stream()
                .filter(p -> sameText(p.getDependencia(), dependencia))
                .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new FuragPdfGenerator().build(proyecto, proyectosDependencia, LocalDate.now());
    }

    @Override
    public byte[] generarReporteRiesgosPdf() {
        List<RiesgoVerificacionReporteDTO> reportes = obtenerVerificacionRiesgos();
        return new RiesgosVerificacionPdfGenerator().build(reportes, LocalDate.now());
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

    private boolean sameText(String left, String right) {
        return normalizeText(left).equals(normalizeText(right));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeOrDefault(String value) {
        return value == null || value.isBlank() ? "No asignado" : value.trim();
    }

    private boolean esTratamientoDiligenciado(com.proyecta.api_gestion.model.Riesgo riesgo) {
        if (riesgo == null) {
            return false;
        }
        boolean tieneTratamiento = riesgo.getTratamiento() != null && !riesgo.getTratamiento().trim().isEmpty();
        return tieneTratamiento && EstadoRiesgo.TRATADO.equals(riesgo.getEstado());
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
