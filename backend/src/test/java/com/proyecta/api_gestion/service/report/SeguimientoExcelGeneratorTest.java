package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica que el reporte de "Seguimiento Excel" escriba formulas validas para Excel
 * (nombres de funcion en ingles, separados por coma) y que los indicadores de avance y
 * programa se calculen sobre los grupos reales (hito/fase/proyecto), igual que el Excel
 * de referencia.
 */
class SeguimientoExcelGeneratorTest {

    private static final String PROYECTO_ID = "P03";

    private SeguimientoExcelGenerator generator;

    @BeforeEach
    void setUp() {
        ProyectoRepositoryPort proyectos = mock(ProyectoRepositoryPort.class);
        when(proyectos.findById(PROYECTO_ID)).thenReturn(Optional.of(proyecto()));

        PublicEvidenceAccessService evidencia = mock(PublicEvidenceAccessService.class);
        PublicUrlProperties urlProperties = mock(PublicUrlProperties.class);
        when(urlProperties.getBase()).thenReturn("http://localhost:8082");

        generator = new SeguimientoExcelGenerator(proyectos, evidencia, urlProperties);
    }

    @Test
    void escribeFormulasEnInglesSinPuntoYComa() throws Exception {
        try (Workbook workbook = abrir()) {
            int formulas = 0;
            for (Row row : workbook.getSheetAt(0)) {
                for (Cell cell : row) {
                    if (cell.getCellType() != CellType.FORMULA) continue;
                    formulas++;
                    String formula = cell.getCellFormula();
                    assertTrue(!formula.contains(";"),
                            "separador ';' no valido en " + cell.getAddress() + ": " + formula);
                    assertTrue(!formula.matches("(?s).*(SI\\.ERROR|PROMEDIO|CONTAR\\.SI|SUMAPRODUCTO|HOY\\().*"),
                            "funcion en español en " + cell.getAddress() + ": " + formula);
                }
            }
            assertTrue(formulas > 40, "se esperaban formulas en toda la hoja");
        }
    }

    @Test
    void calculaAvanceYGraficaSobreLosGruposReales() throws Exception {
        try (Workbook workbook = abrir()) {
            Sheet avance = workbook.getSheetAt(0);

            // Ejecutado hito (O) y programa hito (U): solo en la primera fila de cada hito.
            assertEquals("(G5*L5)+(G6*L6)", formula(avance, 4, 14));
            assertEquals("(G5*T5)+(G6*T6)", formula(avance, 4, 20));
            assertEquals("(G7*L7)+(G8*L8)+(G9*L9)", formula(avance, 6, 14));
            assertEquals("(G7*T7)+(G8*T8)+(G9*T9)", formula(avance, 6, 20));
            assertNull(formulaOrNull(avance, 5, 14), "la segunda fila del hito no debe repetir O");
            assertNull(formulaOrNull(avance, 7, 20), "las filas siguientes al inicio no deben repetir U");

            // Ejecutado fase (P) y programa fase (V): ponderados por la ponderacion del hito.
            assertEquals("(O5*E5)", formula(avance, 4, 15));
            assertEquals("(U5*E5)", formula(avance, 4, 21));
            assertEquals("(O7*E7)", formula(avance, 6, 15));
            assertEquals("(U7*E7)", formula(avance, 6, 21));
            assertNull(formulaOrNull(avance, 5, 15), "P solo se escribe en la primera fila de la fase");
            assertNull(formulaOrNull(avance, 8, 21), "V solo se escribe en la primera fila de la fase");

            // Ejecutado pyto (Q) y programa pyto (W): ponderados por la ponderacion de la fase.
            assertEquals("(P5*C5)+(P7*C7)", formula(avance, 4, 16));
            assertEquals("(V5*C5)+(V7*C7)", formula(avance, 4, 22));
            assertNull(formulaOrNull(avance, 6, 16), "Q solo se escribe en la primera fila de la tabla");
            assertNull(formulaOrNull(avance, 8, 22), "W solo se escribe en la primera fila de la tabla");

            // Las columnas de agrupacion quedan combinadas para que el valor se muestre una sola vez.
            assertTrue(contieneMerges(avance, "O5:O6"), "falta la combinacion de O para el hito 1");
            assertTrue(contieneMerges(avance, "O7:O9"), "falta la combinacion de O para el hito 2");
            assertTrue(contieneMerges(avance, "Q5:Q9"), "falta la combinacion de Q para el proyecto");
            assertTrue(contieneMerges(avance, "B5:B6"), "falta la combinacion de la fase 1");
            assertTrue(contieneMerges(avance, "B7:B9"), "falta la combinacion de la fase 2");
        }
    }

    @Test
    void usaLosMismosIndicadoresQueElExcelDeReferencia() throws Exception {
        try (Workbook workbook = abrir()) {
            Sheet avance = workbook.getSheetAt(0);

            // Resumen bajo la tabla (fila 10) y bloque de indicadores desde la fila 12.
            assertTrue(formula(avance, 9, 24).startsWith("COUNTIFS($Y$5:$Y$9"));
            assertEquals("INDICADORES AL CORTE", texto(avance, 11, 23));
            assertEquals("Fecha de corte", texto(avance, 12, 23));
            assertEquals("COUNTIFS(I5:I9,\"<=\"&Y13,I5:I9,\"<>\")", formula(avance, 13, 24));
            assertEquals("COUNTIFS(J5:J9,\"<=\"&Y13,J5:J9,\"<>\",L5:L9,1)", formula(avance, 14, 24));
            assertTrue(formula(avance, 15, 24).startsWith("SUMPRODUCT(--(J5:J9<=Y13)"));

            // Eficacia = entregados al corte / programados al corte.
            assertEquals("MIN(1,IFERROR(Y15/Y14,0))", formula(avance, 16, 24));

            // Eficiencia = entregados a tiempo / entregados al corte (sin tope MIN(1,...)).
            assertEquals("IFERROR(Y16/Y15,0)", formula(avance, 17, 24));
            assertEquals("Entregados a tiempo / Entregados al corte", texto(avance, 17, 25));
        }
    }

    private Workbook abrir() throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(generator.generate(PROYECTO_ID, "gestor")));
    }

    private String formula(Sheet sheet, int rowIndex, int columnIndex) {
        Cell cell = sheet.getRow(rowIndex).getCell(columnIndex);
        assertNotNull(cell, "falta la celda " + rowIndex + "/" + columnIndex);
        assertEquals(CellType.FORMULA, cell.getCellType(),
                "se esperaba formula en la celda " + cell.getAddress());
        return cell.getCellFormula();
    }

    private String formulaOrNull(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return null;
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() != CellType.FORMULA) return null;
        return cell.getCellFormula();
    }

    private String texto(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getCell(columnIndex) == null) return null;
        return row.getCell(columnIndex).getStringCellValue();
    }

    private boolean contieneMerges(Sheet sheet, String rango) {
        return sheet.getMergedRegions().stream().anyMatch(r -> rango.equals(r.formatAsString()));
    }

    private Proyecto proyecto() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(PROYECTO_ID);
        proyecto.setNombre("FORTALECIMIENTO DEL PROCESO DE GESTION DOCUMENTAL");
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
}
