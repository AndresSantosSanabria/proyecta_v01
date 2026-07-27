package com.proyecta.api_gestion.service.report;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Component
public class ActaCierreDocxGenerator {

    private static final String TEMPLATE_RESOURCE = "templates/acta_cierre_template.docx";
    private static final String BODY_FONT = "Arial";
    private static final String HEADER_FONT = "Tahoma";

    public byte[] build(ActaCierrePdfGenerator.ActaCierrePdfData data) {
        try (InputStream inputStream = openTemplate();
             XWPFDocument doc = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            fillHeader(doc);
            fillGeneralInformation(doc, data);
            fillObjectiveSections(doc, data);
            fillDeliverables(doc, data);
            fillLessons(doc, data);
            fillAlignmentAndValue(doc);
            fillTransfer(doc, data);
            fillApproval(doc, data);

            doc.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el acta de cierre en formato DOCX.", ex);
        }
    }

    private InputStream openTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_RESOURCE);
        if (!resource.exists()) {
            throw new IOException("No se encontró la plantilla DOCX del acta de cierre: " + TEMPLATE_RESOURCE);
        }
        return resource.getInputStream();
    }

    private void fillHeader(XWPFDocument doc) {
        XWPFTable header = doc.getHeaderList().isEmpty() ? null : doc.getHeaderList().get(0).getTables().get(0);
        if (header == null) {
            throw new IllegalStateException("La plantilla no contiene el encabezado institucional esperado.");
        }

        setCellText(header.getRow(0).getCell(1), "PROCESO DE GESTION TECNOLOGICA", true, HEADER_FONT, 10, ParagraphAlignment.CENTER);
        setCellText(header.getRow(0).getCell(2), "CODIGO:  A-GT-FR-004", true, HEADER_FONT, 9, ParagraphAlignment.RIGHT);
        setCellText(header.getRow(1).getCell(1), "ACTA DE CIERRE DE PROYECTO", true, HEADER_FONT, 10, ParagraphAlignment.CENTER);
        setCellText(header.getRow(1).getCell(2), "VERSION: 5", true, HEADER_FONT, 9, ParagraphAlignment.RIGHT);
        setCellText(header.getRow(2).getCell(2), "FECHA APROBACION: 04/03/2025", true, HEADER_FONT, 9, ParagraphAlignment.RIGHT);
    }

    private void fillGeneralInformation(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 0);
        addValueParagraph(table.getRow(1).getCell(0), safe(data.codigoProyecto()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(2).getCell(0), safe(data.nombreProyecto()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(4).getCell(0), sponsorLine(data.patrocinadorNombre(), data.patrocinadorCargo()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(5).getCell(0), safe(data.patrocinadorEntidad()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(8).getCell(0), directorLine(data.directorNombre(), data.directorCargo()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(9).getCell(0), safe(data.directorEntidad()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(11).getCell(0), safe(data.fechaInicio()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(12).getCell(0), safe(data.fechaCierre()), ParagraphAlignment.LEFT, false, 10);
        addValueParagraph(table.getRow(13).getCell(0), safe(data.duracionTotalMeses()) + " meses", ParagraphAlignment.LEFT, false, 10);
    }

    private void fillObjectiveSections(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable objective = bodyTable(doc, 1);
        addValueParagraph(objective.getRow(0).getCell(0), safe(data.objetivoGeneral()), ParagraphAlignment.BOTH, false, 10);

        XWPFTable objectives = bodyTable(doc, 2);
        StringBuilder sb = new StringBuilder();
        List<String> objetivos = data.objetivosEspecificos() == null ? List.of() : data.objetivosEspecificos();
        for (int i = 0; i < objetivos.size(); i++) {
            String objetivo = objetivos.get(i);
            if (objetivo == null || objetivo.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(i + 1).append(". ").append(objetivo.trim());
        }
        addValueParagraph(objectives.getRow(0).getCell(0), sb.length() == 0 ? "No registrado" : sb.toString(), ParagraphAlignment.BOTH, false, 10);

        XWPFTable summary = bodyTable(doc, 3);
        addValueParagraph(summary.getRow(0).getCell(0), safe(data.resumenEjecutivo()), ParagraphAlignment.BOTH, false, 10);
    }

    private void fillDeliverables(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 4);
        List<ActaCierrePdfGenerator.EntregableActaItem> entregables = data.entregables() == null
                ? List.of()
                : data.entregables().stream().filter(Objects::nonNull).limit(3).toList();

        for (int i = 0; i < 3; i++) {
            XWPFTableRow row = table.getRow(i + 1);
            if (i < entregables.size()) {
                ActaCierrePdfGenerator.EntregableActaItem item = entregables.get(i);
                setCellText(row.getCell(0), String.valueOf(i + 1), false, BODY_FONT, 9, ParagraphAlignment.CENTER);
                setCellText(row.getCell(1), safe(item.nombre()), false, BODY_FONT, 9, ParagraphAlignment.LEFT);
                setCellText(row.getCell(2), safe(item.fechaEntrega()), false, BODY_FONT, 9, ParagraphAlignment.CENTER);
                setCellText(row.getCell(3), safe(item.descripcion()), false, BODY_FONT, 9, ParagraphAlignment.LEFT);
                setCellText(row.getCell(4), safe(item.evidencia()), false, BODY_FONT, 9, ParagraphAlignment.LEFT);
            } else {
                for (int c = 0; c < 5; c++) {
                    setCellText(row.getCell(c), "", false, BODY_FONT, 9, ParagraphAlignment.LEFT);
                }
            }
        }
    }

    private void fillLessons(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 5);
        setBodyCellValue(table.getRow(1).getCell(0), "ASPECTOS POSITIVOS (qué funcionó bien):", safe(data.leccionesPositivas()));
        setBodyCellValue(table.getRow(2).getCell(0), "ASPECTOS A MEJORAR (qué no funcionó):", safe(data.leccionesMejorar()));
        setBodyCellValue(table.getRow(3).getCell(0), "RECOMENDACIONES PARA FUTUROS PROYECTOS:", safe(data.recomendaciones()));
    }

    private void fillAlignmentAndValue(XWPFDocument doc) {
        String alignment = "El proyecto se alinea con el plan de desarrollo al fortalecer la transformacion digital, "
                + "la modernizacion administrativa y la interoperabilidad institucional. La solucion aporta automatizacion "
                + "de procesos, gestion basada en datos, trazabilidad de decisiones y capacidad de control sobre los entregables y riesgos del proyecto.";
        String publicValue = "Genera valor publico al reducir tiempos de gestion, mejorar la transparencia del seguimiento, "
                + "concentrar la informacion en una sola plataforma, facilitar la consulta de evidencias y aumentar la calidad del servicio prestado a ciudadanos y equipos internos.";

        XWPFTable alignmentTable = bodyTable(doc, 6);
        addValueParagraph(alignmentTable.getRow(0).getCell(0), alignment, ParagraphAlignment.BOTH, false, 10);

        XWPFTable valueTable = bodyTable(doc, 7);
        addValueParagraph(valueTable.getRow(0).getCell(0), publicValue, ParagraphAlignment.BOTH, false, 10);
    }

    private void fillTransfer(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 8);
        setCellText(table.getRow(1).getCell(0), safe(data.transferenciaActividad()), false, BODY_FONT, 9, ParagraphAlignment.LEFT);
        setCellText(table.getRow(1).getCell(1), safe(data.transferenciaFecha()), false, BODY_FONT, 9, ParagraphAlignment.CENTER);
        setCellText(table.getRow(1).getCell(2), safe(data.transferenciaUbicacionEvidencia()), false, BODY_FONT, 9, ParagraphAlignment.LEFT);

        setCellText(table.getRow(2).getCell(0), "", false, BODY_FONT, 9, ParagraphAlignment.LEFT);
        setCellText(table.getRow(2).getCell(1), "", false, BODY_FONT, 9, ParagraphAlignment.LEFT);
        setCellText(table.getRow(2).getCell(2), "", false, BODY_FONT, 9, ParagraphAlignment.LEFT);
    }

    private void fillApproval(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 10);
        setBodyCellValue(table.getRow(1).getCell(0), "Nombre del director del Proyecto:", safe(data.directorNombre()));
        setBodyCellValue(table.getRow(1).getCell(1), "Nombre del patrocinador del proyecto:", safe(data.patrocinadorNombre()));

        setBodyCellValue(table.getRow(2).getCell(0), "Firma del director del Proyecto:", "________________________________");
        setBodyCellValue(table.getRow(2).getCell(1), "Firma del patrocinador del proyecto:", "________________________________");

        setBodyCellValue(table.getRow(3).getCell(0), "Fecha:", safe(data.fechaCierre()));
        replaceText(doc, "[xxxx]", safe(data.nombreProyecto()));
    }

    private XWPFTable bodyTable(XWPFDocument doc, int index) {
        List<XWPFTable> tables = doc.getTables();
        if (index < 0 || index >= tables.size()) {
            throw new IllegalStateException("La plantilla DOCX no contiene la tabla esperada en la posicion " + index + ".");
        }
        return tables.get(index);
    }

    private void setBodyCellValue(XWPFTableCell cell, String label, String value) {
        XWPFParagraph labelParagraph = ensureParagraph(cell, 0);
        setParagraphText(labelParagraph, label, true, BODY_FONT, 9, ParagraphAlignment.LEFT);
        XWPFParagraph valueParagraph;
        if (cell.getParagraphs().size() > 1) {
            valueParagraph = cell.getParagraphs().get(1);
        } else {
            valueParagraph = cell.addParagraph();
        }
        setParagraphText(valueParagraph, value, false, BODY_FONT, 10, ParagraphAlignment.LEFT);
        while (cell.getParagraphs().size() > 2) {
            cell.removeParagraph(cell.getParagraphs().size() - 1);
        }
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, String fontFamily, int size, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = ensureParagraph(cell, 0);
        setParagraphText(paragraph, text, bold, fontFamily, size, alignment);
        removeExtraParagraphs(cell, 1);
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }

    private void addValueParagraph(XWPFTableCell cell, String text, ParagraphAlignment alignment, boolean bold, int size) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }
        XWPFParagraph paragraph = cell.addParagraph();
        setParagraphText(paragraph, text, bold, BODY_FONT, size, alignment);
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }

    private XWPFParagraph ensureParagraph(XWPFTableCell cell, int index) {
        while (cell.getParagraphs().size() <= index) {
            cell.addParagraph();
        }
        return cell.getParagraphs().get(index);
    }

    private void removeExtraParagraphs(XWPFTableCell cell, int keepCount) {
        while (cell.getParagraphs().size() > keepCount) {
            cell.removeParagraph(cell.getParagraphs().size() - 1);
        }
    }

    private void setParagraphText(XWPFParagraph paragraph,
                                  String text,
                                  boolean bold,
                                  String fontFamily,
                                  int size,
                                  ParagraphAlignment alignment) {
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setBold(bold);
        run.setFontFamily(fontFamily);
        run.setFontSize(size);

        paragraph.setAlignment(alignment);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBetween(1.0);
    }

    private String sponsorLine(String nombre, String cargo) {
        String n = safe(nombre);
        String c = safe(cargo);
        if ("No registrado".equals(n) && "No registrado".equals(c)) {
            return "No registrado";
        }
        return n + (c.equals("No registrado") ? "" : " - " + c);
    }

    private void replaceText(XWPFDocument doc, String placeholder, String replacement) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            replaceText(paragraph, placeholder, replacement);
        }
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceText(paragraph, placeholder, replacement);
                    }
                }
            }
        }
    }

    private void replaceText(XWPFParagraph paragraph, String placeholder, String replacement) {
        String fullText = paragraph.getText();
        if (fullText == null || !fullText.contains(placeholder)) {
            return;
        }

        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
        XWPFRun run = paragraph.createRun();
        run.setText(fullText.replace(placeholder, replacement));
    }

    private String directorLine(String nombre, String cargo) {
        String n = safe(nombre);
        String c = safe(cargo);
        if ("No registrado".equals(n) && "No registrado".equals(c)) {
            return "No registrado";
        }
        return n + (c.equals("No registrado") ? "" : " - " + c);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "No registrado" : value.trim();
    }
}
