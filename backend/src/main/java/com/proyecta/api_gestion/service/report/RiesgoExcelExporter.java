package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.model.Riesgo;
import com.proyecta.api_gestion.model.RiesgoSolucionAdjunto;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RiesgoExcelExporter {

    private final String publicUrlBase;

    public RiesgoExcelExporter(PublicUrlProperties publicUrlProperties) {
        this.publicUrlBase = normalizeBase(publicUrlProperties.getBase());
    }

    private String normalizeBase(String base) {
        if (base == null || base.isBlank()) {
            return "http://localhost:8082";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public byte[] buildProjectRiskMatrix(String projectId, List<Riesgo> riesgos) {
        try (
                InputStream templateStream = new ClassPathResource("report-assets/Matriz de Riesgos Plantilla.xlsx").getInputStream();
                Workbook workbook = new XSSFWorkbook(templateStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            List<RiskRow> rows = buildRows(projectId, riesgos);
            int dataStartRow = 6;
            int templateRows = 5;
            Row styleRow = sheet.getRow(dataStartRow);
            if (styleRow == null) {
                throw new IllegalStateException("La plantilla de matriz de riesgos no contiene la fila de datos esperada.");
            }

            for (int i = 0; i < rows.size(); i++) {
                RiskRow data = rows.get(i);
                Row source = sheet.getRow(dataStartRow + Math.min(i, templateRows - 1));
                Row target = sheet.getRow(dataStartRow + i);
                if (target == null) {
                    target = sheet.createRow(dataStartRow + i);
                }
                copyRowStyle(source, target);
                target.setHeight(source != null ? source.getHeight() : styleRow.getHeight());
                fillRow(target, data, workbook);
            }

            for (int i = rows.size(); i < templateRows; i++) {
                Row row = sheet.getRow(dataStartRow + i);
                if (row != null) {
                    clearRow(row, 0, 10);
                    row.setHeight(styleRow.getHeight());
                }
            }

            int lastRow = Math.max(dataStartRow + rows.size() - 1, dataStartRow);
            sheet.setAutoFilter(new CellRangeAddress(4, lastRow, 0, 10));
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el archivo Excel de riesgos.", ex);
        }
    }

    private List<RiskRow> buildRows(String projectId, List<Riesgo> riesgos) {
        List<RiskRow> rows = new ArrayList<>();
        int numero = 1;
        for (Riesgo riesgo : riesgos) {
            List<RiesgoSolucionAdjunto> soluciones = riesgo.getSoluciones() == null ? List.of() : new ArrayList<>(riesgo.getSoluciones());
            if (soluciones.isEmpty()) {
                rows.add(mapRiskRow(numero, riesgo, "Sin evidencia", null));
                numero++;
                continue;
            }

            StringBuilder labels = new StringBuilder();
            StringBuilder urls = new StringBuilder();
            for (int i = 0; i < soluciones.size(); i++) {
                RiesgoSolucionAdjunto adjunto = soluciones.get(i);
                if (i > 0) {
                    labels.append('\n');
                    urls.append('\n');
                }
                labels.append(defaultLabel(adjunto.getNombreOriginal(), i + 1));
                urls.append(publicSolutionUrl(projectId, riesgo.getId(), adjunto.getId()));
            }
            rows.add(mapRiskRow(numero, riesgo, labels.toString(), urls.toString()));
            numero++;
        }
        return rows;
    }

    private RiskRow mapRiskRow(int numero, Riesgo riesgo, String evidenciaLabel, String evidenciaUrl) {
        return new RiskRow(
                String.valueOf(numero),
                normalize(riesgo.getDescripcion(), "Sin descripci�n"),
                riesgo.getProbabilidad() == null ? "" : riesgo.getProbabilidad().name(),
                riesgo.getImpacto() == null ? "" : riesgo.getImpacto().name(),
                String.valueOf(score(riesgo.getProbabilidad(), riesgo.getImpacto())),
                riesgo.getNivel() == null ? "" : riesgo.getNivel().name(),
                normalize(riesgo.getTratamiento() != null ? riesgo.getTratamiento() : riesgo.getAccionesMitigacion(), ""),
                normalize(riesgo.getEntidadResponsable() != null ? riesgo.getEntidadResponsable() : riesgo.getRolResponsable(), ""),
                normalize(riesgo.getAccionesMitigacion() != null ? riesgo.getAccionesMitigacion() : riesgo.getTratamiento(), ""),
                riesgo.getFechaAccion(),
                evidenciaLabel == null || evidenciaLabel.isBlank() ? "Click aqu�" : evidenciaLabel,
                evidenciaUrl
        );
    }

    private void fillRow(Row row, RiskRow data, Workbook workbook) {
        CellStyle levelStyle = row.getCell(5) != null ? row.getCell(5).getCellStyle() : null;
        setText(row, 0, data.nro());
        setText(row, 1, data.descripcion());
        setText(row, 2, data.probabilidad());
        setText(row, 3, data.impacto());
        setText(row, 4, data.calificacion());
        setLevel(row, 5, data.nivel(), levelStyle, workbook);
        setText(row, 6, data.mitigar());
        setText(row, 7, data.responsable());
        setText(row, 8, data.acciones());
        setDate(row, 9, data.fechaAccion());
        setEvidence(row, 10, data.evidenciaLabel(), data.evidenciaUrl(), workbook);
    }

    private void setText(Row row, int col, String value) {
        cell(row, col).setCellValue(value == null ? "" : value);
    }

    private void setDate(Row row, int col, LocalDate value) {
        Cell cell = cell(row, col);
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(java.util.Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private void setLevel(Row row, int col, String value, CellStyle baseStyle, Workbook workbook) {
        Cell cell = cell(row, col);
        cell.setCellValue(value == null ? "" : value);
        CellStyle style = workbook.createCellStyle();
        if (baseStyle != null) {
            style.cloneStyleFrom(baseStyle);
        }
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("EXTREMO".equals(normalized)) {
            style.setFillForegroundColor(IndexedColors.RED.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else if ("ALTO".equals(normalized)) {
            style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        cell.setCellStyle(style);
    }

    private void setEvidence(Row row, int col, String label, String urls, Workbook workbook) {
        Cell cell = cell(row, col);
        cell.setCellValue(label == null || label.isBlank() ? "Click aqu�" : label);
        cell.getCellStyle().setWrapText(true);
        if (urls != null && !urls.isBlank()) {
            String firstUrl = urls.split("\\R")[0].trim();
            if (!firstUrl.isBlank()) {
                CreationHelper helper = workbook.getCreationHelper();
                Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(firstUrl);
                cell.setHyperlink(hyperlink);
            }
        }
    }

    private Cell cell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            cell = row.createCell(col);
        }
        return cell;
    }

    private void copyRowStyle(Row source, Row target) {
        if (source == null || target == null) return;
        for (int col = 0; col <= 10; col++) {
            Cell sourceCell = source.getCell(col);
            if (sourceCell == null) continue;
            cell(target, col).setCellStyle(sourceCell.getCellStyle());
        }
    }

    private void clearRow(Row row, int fromCol, int toCol) {
        if (row == null) return;
        for (int col = fromCol; col <= toCol; col++) {
            cell(row, col).setBlank();
        }
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private String defaultLabel(String nombreOriginal, int index) {
        return "Entregable - " + String.format(Locale.ROOT, "%02d", index);
    }

    private String publicSolutionUrl(String projectId, Integer riesgoId, Long solucionId) {
        return publicUrlBase + "/api/v1/public/riesgos/" + projectId + "/" + riesgoId + "/soluciones/" + solucionId + "?inline=true";
    }

    private int score(Object probabilidad, Object impacto) {
        int p = switch (String.valueOf(probabilidad)) {
            case "UNO" -> 1;
            case "DOS" -> 2;
            case "TRES" -> 3;
            case "CUATRO" -> 4;
            case "CINCO" -> 5;
            default -> 0;
        };
        int i = switch (String.valueOf(impacto)) {
            case "UNO" -> 1;
            case "DOS" -> 2;
            case "TRES" -> 3;
            case "CUATRO" -> 4;
            case "CINCO" -> 5;
            default -> 0;
        };
        return p + i;
    }

    private record RiskRow(
            String nro,
            String descripcion,
            String probabilidad,
            String impacto,
            String calificacion,
            String nivel,
            String mitigar,
            String responsable,
            String acciones,
            LocalDate fechaAccion,
            String evidenciaLabel,
            String evidenciaUrl
    ) {}
}
