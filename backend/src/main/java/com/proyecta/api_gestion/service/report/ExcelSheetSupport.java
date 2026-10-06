package com.proyecta.api_gestion.service.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ExcelSheetSupport {

    private ExcelSheetSupport() {
    }

    public static void escribirCabeceraAgrupada(Row row, int columnaTexto, int columnaPonderacion, boolean primeraAparicion, String texto, BigDecimal ponderacion) {
        if (primeraAparicion) {
            setCellText(row, columnaTexto, texto);
            setCellPercent(row, columnaPonderacion, ponderacion);
        } else {
            setCellText(row, columnaTexto, "");
            setCellBlank(row, columnaPonderacion);
        }
    }

    public static void escribirFormulaSuma(Row row, int columna, boolean aplicar, List<Integer> filas, String columnaIzquierda, String columnaDerecha) {
        if (aplicar) {
            setCellFormula(row, columna, construirFormulaSuma(filas, columnaIzquierda, columnaDerecha));
        } else {
            setCellBlank(row, columna);
        }
    }

    public static String construirFormulaSuma(List<Integer> filas, String columnaIzquierda, String columnaDerecha) {
        StringBuilder sb = new StringBuilder();
        for (int r : filas) {
            if (!sb.isEmpty()) sb.append("+");
            sb.append("(").append(columnaIzquierda).append(r).append("*").append(columnaDerecha).append(r).append(")");
        }
        return sb.toString();
    }

    public static Row plantillaDe(Sheet sheet, int rowIndex, Row porDefecto) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : porDefecto;
    }

    public static void copiarEstilosFila(Row source, Row target, int fromColumnInclusive, int toColumnInclusive) {
        if (source == null) return;
        target.setHeight(source.getHeight());
        for (int column = fromColumnInclusive; column <= toColumnInclusive; column++) {
            Cell sourceCell = source.getCell(column);
            Cell targetCell = target.getCell(column);
            if (targetCell == null) targetCell = target.createCell(column);
            if (sourceCell != null) targetCell.setCellStyle(sourceCell.getCellStyle());
        }
    }

    public static Row ensureRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    public static void clearTemplateRow(Row row, int fromColumnInclusive, int toColumnInclusive) {
        for (int col = fromColumnInclusive; col <= toColumnInclusive; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
            }
            cell.setBlank();
        }
    }

    public static void setCellText(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value != null ? value : "");
    }

    public static void setCellNumber(Row row, int columnIndex, Number value) {
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

    public static void setCellPercent(Row row, int columnIndex, BigDecimal value) {
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

    public static void setCellDate(Row row, int columnIndex, LocalDateTime value) {
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

    public static void setCellBlank(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setBlank();
    }

    public static void setCellFormula(Row row, int columnIndex, String formula) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) cell = row.createCell(columnIndex);
        cell.setCellFormula(formula);
    }

    public static void setCellHyperlink(Row row, int columnIndex, String label, String url) {
        setCellText(row, columnIndex, label);
        Cell cell = row.getCell(columnIndex);
        if (cell != null) {
            CellStyle style = row.getSheet().getWorkbook().createCellStyle();
            CellStyle existing = cell.getCellStyle();
            if (existing != null) style.cloneStyleFrom(existing);
            style.setShrinkToFit(true);
            style.setWrapText(false);
            cell.setCellStyle(style);
        }
        if (url == null || url.isBlank()) return;
        if (cell == null) return;
        Hyperlink hyperlink = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(url);
        cell.setHyperlink(hyperlink);
    }
}
