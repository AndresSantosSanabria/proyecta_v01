package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.dto.avance.FaseAvanceDTO;
import com.proyecta.api_gestion.dto.avance.HitoAvanceDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica que el "Descargar reporte actual" reproduzca el formato y las formulas
 * del Excel de referencia (FINAL FORMULACION.xlsx).
 *
 * El proyecto de prueba tiene 5 entregables: la fase 2 inicia en la fila 7, que era
 * exactamente la fila que quedaba oculta por la fusion fija B5:B8 de la plantilla, y
 * 5 filas de datos dejaban sin escribir las filas 20 a 22 del bloque de indicadores.
 */
class ReporteActualProyectoExcelTest {

    private static final String PROYECTO_ID = "P03";
    private static final int TOTAL_ENTREGABLES = 5;

    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        ProyectoRepositoryPort proyectos = mock(ProyectoRepositoryPort.class);
        when(proyectos.findById(PROYECTO_ID)).thenReturn(Optional.of(proyecto()));
        when(proyectos.findAll()).thenReturn(List.of(proyecto()));

        ProjectProgressMetricsService metricas = mock(ProjectProgressMetricsService.class);
        when(metricas.construir(any(Proyecto.class), any(LocalDate.class))).thenReturn(avance());

        SeguridadUsuarioProyectoRepositoryPort seguridad = mock(SeguridadUsuarioProyectoRepositoryPort.class);
        when(seguridad.findActiveDirectorAssignmentsByProyectoId(PROYECTO_ID)).thenReturn(List.of());

        PublicEvidenceAccessService evidencia = mock(PublicEvidenceAccessService.class);
        when(evidencia.getOrCreateToken(any(Entregable.class), anyString())).thenReturn("token-evidencia");

        PublicUrlProperties urlProperties = mock(PublicUrlProperties.class);
        when(urlProperties.getBase()).thenReturn("http://localhost:8080");

        service = new ReporteServiceImpl(
                proyectos,
                mock(com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort.class),
                mock(com.proyecta.api_gestion.application.port.out.persistence.RiesgoRepositoryPort.class),
                mock(com.proyecta.api_gestion.application.port.out.persistence.ReporteConfigRepositoryPort.class),
                metricas,
                seguridad,
                mock(KeycloakIdentityExtractor.class),
                mock(ProyectoSecurity.class),
                evidencia,
                urlProperties,
                mock(com.proyecta.api_gestion.application.port.out.persistence.FuragRespuestaRepositoryPort.class),
                null
        );
    }

    @Test
    void generaUnSoloBloqueDeIndicadoresSinCeldasCombinadasObsoletas() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                service.generarReporteActualProyectoExcel(PROYECTO_ID)))) {
            Sheet avance = workbook.getSheet("AVANCE");

            // La unica celda combinada permitida es el titulo del bloque de indicadores (X12:Z12).
            assertEquals(1, avance.getNumMergedRegions(),
                    "solo debe existir la combinacion del titulo de indicadores");
            assertEquals("X12:Z12", avance.getMergedRegion(0).formatAsString());

            // La fase 2 arranca en la fila 7: no puede quedar oculta dentro de una fusion.
            assertEquals("FASE 2", texto(avance, 6, 1));

            // El bloque completo debe escribirse una unica vez y sin restos de la plantilla.
            assertEquals("INDICADORES AL CORTE", texto(avance, 11, 23));
            assertEquals("Fecha de corte", texto(avance, 12, 23));
            assertEquals("Programados al corte", texto(avance, 13, 23));
            assertEquals("Entregados al corte", texto(avance, 14, 23));
            assertEquals("Entregados a tiempo", texto(avance, 15, 23));
            assertEquals("Eficacia", texto(avance, 16, 23));
            assertEquals("Eficiencia", texto(avance, 17, 23));
            assertEquals("total entregables", texto(avance, 18, 23));

            assertEquals(1, contar(avance, "INDICADORES AL CORTE"), "el titulo no debe repetirse");
            assertEquals(1, contar(avance, "Fecha de corte"), "el bloque de indicadores no debe duplicarse");

            // La plantilla trae indicadores hasta la fila 22; con 5 entregables deben quedar vacios.
            for (int rowIndex = 19; rowIndex <= 21; rowIndex++) {
                assertVacia(avance, rowIndex, 23);
                assertVacia(avance, rowIndex, 24);
            }
        }
    }

    @Test
    void aplicaLasFormulasDeLaReferencia() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                service.generarReporteActualProyectoExcel(PROYECTO_ID)))) {
            Sheet avance = workbook.getSheet("AVANCE");

            // Atraso con guardia cuando no hay fecha de entrega (referencia: =IF(I5-J5>1000,"NA",I5-J5))
            assertEquals("IF(I5-J5>1000,\"NA\",I5-J5)", avance.getRow(4).getCell(10).getCellFormula());

            // Ejecutado PYTO pondera las fases reales (filas 5 y 7), no la fila 10 de la plantilla.
            assertEquals("(P5*C5)+(P7*C7)", avance.getRow(4).getCell(16).getCellFormula());

            // Mismo orden de factores que la referencia: U=(T*G), V=(U*E).
            assertEquals("(T5*G5)+(T6*G6)", avance.getRow(4).getCell(20).getCellFormula());
            assertEquals("(U5*E5)", avance.getRow(4).getCell(21).getCellFormula());

            // Indicadores al corte
            assertEquals("COUNTIFS(I:I,\"<=\"&Y13,I:I,\"<>\")", avance.getRow(13).getCell(24).getCellFormula());
            assertEquals("MIN(1,IFERROR(Y15/Y14,0))", avance.getRow(16).getCell(24).getCellFormula());
            // Eficiencia = entregados a tiempo / entregados al corte (sin tope MIN(1,...))
            assertEquals("IFERROR(Y16/Y15,0)", avance.getRow(17).getCell(24).getCellFormula());
            assertEquals("Entregados a tiempo / Entregados al corte", texto(avance, 17, 25));
            assertEquals("COUNT(I5:I9)", avance.getRow(18).getCell(24).getCellFormula());

            Sheet informe = workbook.getSheet("INFORME 1");
            assertEquals("ITEM", texto(informe, 3, 1));
            assertEquals("AVANCE!W5", informe.getRow(5).getCell(2).getCellFormula());
            assertEquals("AVANCE!V5", informe.getRow(7).getCell(2).getCellFormula());
            assertEquals("AVANCE!U5", informe.getRow(10).getCell(2).getCellFormula());
            assertEquals("AVANCE!U7", informe.getRow(11).getCell(2).getCellFormula());
        }
    }

    @Test
    void escribeLasFechasCompletasDeLaBaseDeDatos() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                service.generarReporteActualProyectoExcel(PROYECTO_ID)))) {
            Sheet datos = workbook.getSheet("DATOS");

            // Los datos de la base se conservan tal cual (incluidas META y PRESUPUESTO)...
            assertEquals("META", texto(datos, 3, 1));
            assertEquals("Alcance del proyecto", texto(datos, 3, 2));

            // ...pero las fechas deben coincidir con las del sistema, no solo el año.
            assertEquals("18/10/2024", texto(datos, 11, 2), "fecha de inicio");
            assertEquals("30/05/2026", texto(datos, 12, 2), "fecha fin = mayor fecha limite de entregables");
        }
    }

    @Test
    void sobrescribeLosTextosDeEjemploDeLaPlantilla() throws Exception {
        // La plantilla guarda los textos como inlineStr; POI no los limpia al sobrescribir
        // y el archivo generado conservaba los datos de ejemplo (P02 CUNDILEX, etc.).
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                service.generarReporteActualProyectoExcel(PROYECTO_ID)))) {
            Sheet datos = workbook.getSheet("DATOS");
            assertEquals("P03", texto(datos, 1, 2));
            assertEquals("FORTALECIMIENTO DEL PROCESO DE GESTION DOCUMENTAL", texto(datos, 2, 2));
            assertEquals("Alcance del proyecto", texto(datos, 3, 2));
            assertEquals("Migrar al SGDEA", texto(datos, 9, 2));

            Sheet avance = workbook.getSheet("AVANCE");
            assertEquals("P03 FORTALECIMIENTO DEL PROCESO DE GESTION DOCUMENTAL", texto(avance, 1, 1));
            assertEquals("E01", texto(avance, 4, 7));
            assertEquals(0, contar(avance, "P02 CUNDILEX"), "no deben quedar datos de ejemplo");

            Sheet informe = workbook.getSheet("INFORME 1");
            assertEquals("AVANCES DEL PROYECTO P03", texto(informe, 1, 1));
        }
    }

    private String texto(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getCell(columnIndex) == null) return null;
        return row.getCell(columnIndex).getStringCellValue();
    }

    private void assertVacia(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        org.apache.poi.ss.usermodel.Cell cell = row == null ? null : row.getCell(columnIndex);
        boolean vacia = cell == null
                || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK
                || (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                    && cell.getStringCellValue().isEmpty());
        assertTrue(vacia, "X/Y" + (rowIndex + 1) + " debe quedar vacia");
    }

    private int contar(Sheet sheet, String valor) {
        int total = 0;
        for (Row row : sheet) {
            for (var cell : row) {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && valor.equals(cell.getStringCellValue())) {
                    total++;
                }
            }
        }
        return total;
    }

    private Proyecto proyecto() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(PROYECTO_ID);
        proyecto.setNombre("FORTALECIMIENTO DEL PROCESO DE GESTION DOCUMENTAL");
        proyecto.setAlcanceDetallado("Alcance del proyecto");
        proyecto.setDependencia("SECRETARIA GENERAL");
        proyecto.setCorreoDirector("director@cundinamarca.gov.co");
        proyecto.setObjetivoGeneral("Migrar al SGDEA");
        proyecto.setFechaInicio(LocalDate.of(2024, 10, 18));
        proyecto.setPresupuestoEstimado(BigDecimal.ZERO);
        proyecto.setPeti(true);
        proyecto.setFases(List.of(
                fase(1, "FASE 1", 50, List.of(hito(1, "HITO 1", 100, List.of(
                        entregable(1, "E01", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 10)),
                        entregable(2, "E02", LocalDate.of(2026, 2, 20), null))))),
                fase(2, "FASE 2", 50, List.of(hito(2, "HITO 2", 100, List.of(
                        entregable(3, "E03", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)),
                        entregable(4, "E04", LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 1)),
                        entregable(5, "E05", LocalDate.of(2026, 5, 30), null)))))
        ));
        return proyecto;
    }

    private Fase fase(int id, String nombre, int ponderacion, List<Hito> hitos) {
        Fase fase = new Fase();
        fase.setId(id);
        fase.setNombre(nombre);
        fase.setPonderacion(BigDecimal.valueOf(ponderacion));
        fase.setHitos(new ArrayList<>(hitos));
        return fase;
    }

    private Hito hito(int id, String nombre, int ponderacion, List<Entregable> entregables) {
        Hito hito = new Hito();
        hito.setId(id);
        hito.setNombre(nombre);
        hito.setPonderacion(BigDecimal.valueOf(ponderacion));
        hito.setEntregables(new ArrayList<>(entregables));
        return hito;
    }

    private Entregable entregable(int id, String nombre, LocalDate fechaLimite, LocalDate fechaEntrega) {
        Entregable entregable = new Entregable();
        entregable.setId(id);
        entregable.setNombre(nombre);
        entregable.setDescripcion(nombre);
        entregable.setPonderacion(BigDecimal.valueOf(100));
        entregable.setFechaLimite(fechaLimite);
        entregable.setFechaEntregaReal(fechaEntrega);
        return entregable;
    }

    private ProyectoAvanceResponseDTO avance() {
        return new ProyectoAvanceResponseDTO(
                PROYECTO_ID, PROYECTO_ID, "FORTALECIMIENTO",
                BigDecimal.valueOf(60), BigDecimal.valueOf(55), BigDecimal.valueOf(-5),
                BigDecimal.valueOf(91), BigDecimal.valueOf(91), "ACTIVO",
                BigDecimal.valueOf(55), 3L, 5L, 1L, 1L, 4L, 3L, 3L,
                LocalDate.now(),
                List.of(
                        new FaseAvanceDTO(1, "FASE 1", "", BigDecimal.valueOf(50),
                                BigDecimal.valueOf(60), BigDecimal.valueOf(55), BigDecimal.ZERO,
                                BigDecimal.valueOf(91), "EN_PROCESO", BigDecimal.valueOf(55),
                                List.of(new HitoAvanceDTO(1, "HITO 1", "", BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(60), BigDecimal.valueOf(55), BigDecimal.ZERO,
                                        BigDecimal.valueOf(91), "EN_PROCESO", BigDecimal.valueOf(55),
                                        List.of()))),
                        new FaseAvanceDTO(2, "FASE 2", "", BigDecimal.valueOf(50),
                                BigDecimal.valueOf(60), BigDecimal.valueOf(55), BigDecimal.ZERO,
                                BigDecimal.valueOf(91), "EN_PROCESO", BigDecimal.valueOf(55),
                                List.of(new HitoAvanceDTO(2, "HITO 2", "", BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(60), BigDecimal.valueOf(55), BigDecimal.ZERO,
                                        BigDecimal.valueOf(91), "EN_PROCESO", BigDecimal.valueOf(55),
                                        List.of())))
                )
        );
    }

    @Test
    void laPlantillaDeReporteConservaLosEncabezadosDeLaReferencia() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(getClass().getClassLoader()
                .getResourceAsStream("report-assets/Reporte actual proyecto PETI.xlsx"))) {
            Sheet avance = workbook.getSheet("AVANCE");
            assertEquals(" PROG", texto(avance, 2, 18));
            assertEquals(" PROG", texto(avance, 2, 19));
            assertEquals("SIGNO", texto(avance, 3, 18));
            assertEquals(0, avance.getNumMergedRegions(), "la plantilla no debe traer fusiones fijas");
            assertTrue(texto(workbook.getSheet("INFORME 1"), 3, 1).equals("ITEM"));
        }
    }
}
