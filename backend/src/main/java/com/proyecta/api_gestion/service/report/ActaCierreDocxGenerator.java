package com.proyecta.api_gestion.service.report;

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

    public byte[] build(ActaCierrePdfGenerator.ActaCierrePdfData data) {
        try (InputStream inputStream = openTemplate();
             XWPFDocument doc = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            fillHeader(doc, data);
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

    private void fillHeader(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable header = doc.getHeaderList().isEmpty() ? null : doc.getHeaderList().get(0).getTables().get(0);
        if (header == null) {
            throw new IllegalStateException("La plantilla no contiene el encabezado institucional esperado.");
        }
        // Solo actualizamos la fecha de aprobación preservando el estilo existente en la celda
        setCellTextPreservingStyle(header.getRow(2).getCell(2), "FECHA APROBACION: " + safe(data.fechaCierre()));
    }

    private void fillGeneralInformation(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 0);
        setCellTextPreservingStyle(table.getRow(1).getCell(0), safe(data.codigoProyecto()));
        setCellTextPreservingStyle(table.getRow(2).getCell(0), safe(data.nombreProyecto()));
        // Patrocinador: Row 3 = sub-header (untouched), Row 4 = Nombre, Row 5 = Cargo, Row 6 = Entidad
        setCellTextPreservingStyle(table.getRow(4).getCell(0), safe(data.patrocinadorNombre()));
        setCellTextPreservingStyle(table.getRow(5).getCell(0), safe(data.patrocinadorCargo()));
        setCellTextPreservingStyle(table.getRow(6).getCell(0), safe(data.patrocinadorEntidad()));
        // Director: Row 7 = sub-header (untouched), Row 8 = Nombre, Row 9 = Cargo, Row 10 = Entidad
        setCellTextPreservingStyle(table.getRow(8).getCell(0), safe(data.directorNombre()));
        setCellTextPreservingStyle(table.getRow(9).getCell(0), safe(data.directorCargo()));
        setCellTextPreservingStyle(table.getRow(10).getCell(0), safe(data.directorEntidad()));
        setCellTextPreservingStyle(table.getRow(11).getCell(0), safe(data.fechaInicio()));
        setCellTextPreservingStyle(table.getRow(12).getCell(0), safe(data.fechaCierre()));
        setCellTextPreservingStyle(table.getRow(13).getCell(0), safe(data.duracionTotalMeses()) + " meses");
    }

    private void fillObjectiveSections(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        // Table 1: Row 0 = blue header "OBJETIVO GENERAL:", Row 1 = white content row
        XWPFTable objective = bodyTable(doc, 1);
        setCellTextPreservingStyle(objective.getRow(1).getCell(0), safe(data.objetivoGeneral()));

        // Table 2: Row 0 = blue header "OBJETIVOS ESPECIFICOS:", Row 1 = white content row
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
        setCellTextPreservingStyle(objectives.getRow(1).getCell(0), sb.length() == 0 ? "No registrado" : sb.toString());

        // Table 3: Row 0 = blue header "RESUMEN EJECUTIVO:", Row 1 = white content row
        XWPFTable summary = bodyTable(doc, 3);
        setCellTextPreservingStyle(summary.getRow(1).getCell(0), safe(data.resumenEjecutivo()));
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
                setCellTextPreservingStyle(row.getCell(0), String.valueOf(i + 1));
                setCellTextPreservingStyle(row.getCell(1), safe(item.nombre()));
                setCellTextPreservingStyle(row.getCell(2), safe(item.fechaEntrega()));
                setCellTextPreservingStyle(row.getCell(3), safe(item.descripcion()));
                setCellTextPreservingStyle(row.getCell(4), safe(item.evidencia()));
            } else {
                for (int c = 0; c < 5; c++) {
                    setCellTextPreservingStyle(row.getCell(c), "");
                }
            }
        }
    }

    private void fillLessons(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 5);
        setBodyCellValuePreservingStyle(table.getRow(1).getCell(0), "ASPECTOS POSITIVOS (qué funcionó bien):", safe(data.leccionesPositivas()));
        setBodyCellValuePreservingStyle(table.getRow(2).getCell(0), "ASPECTOS A MEJORAR (qué no funcionó):", safe(data.leccionesMejorar()));
        setBodyCellValuePreservingStyle(table.getRow(3).getCell(0), "RECOMENDACIONES PARA FUTUROS PROYECTOS:", safe(data.recomendaciones()));
    }

    private void fillAlignmentAndValue(XWPFDocument doc) {
        String alignment = "El proyecto se alinea con el plan de desarrollo al fortalecer la transformacion digital, "
                + "la modernizacion administrativa y la interoperabilidad institucional. La solucion aporta automatizacion "
                + "de procesos, gestion basada en datos, trazabilidad de decisiones y capacidad de control sobre los entregables y riesgos del proyecto.";
        String publicValue = "Genera valor publico al reducir tiempos de gestion, mejorar la transparencia del seguimiento, "
                + "concentrar la informacion en una sola plataforma, facilitar la consulta de evidencias y aumentar la calidad del servicio prestado a ciudadanos y equipos internos.";

        // Table 6: Row 0 = blue header "ALINEACION ESTRATEGICA", Row 1 = white content row
        XWPFTable alignmentTable = bodyTable(doc, 6);
        setCellTextPreservingStyle(alignmentTable.getRow(1).getCell(0), alignment);

        // Table 7: Row 0 = blue header "VALOR PUBLICO", Row 1 = white content row
        XWPFTable valueTable = bodyTable(doc, 7);
        setCellTextPreservingStyle(valueTable.getRow(1).getCell(0), publicValue);
    }

    private void fillTransfer(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 8);
        setCellTextPreservingStyle(table.getRow(1).getCell(0), safe(data.transferenciaActividad()));
        setCellTextPreservingStyle(table.getRow(1).getCell(1), safe(data.transferenciaFecha()));
        setCellTextPreservingStyle(table.getRow(1).getCell(2), safe(data.transferenciaUbicacionEvidencia()));

        setCellTextPreservingStyle(table.getRow(2).getCell(0), "");
        setCellTextPreservingStyle(table.getRow(2).getCell(1), "");
        setCellTextPreservingStyle(table.getRow(2).getCell(2), "");
    }

    private void fillApproval(XWPFDocument doc, ActaCierrePdfGenerator.ActaCierrePdfData data) {
        XWPFTable table = bodyTable(doc, 10);
        setBodyCellValuePreservingStyle(table.getRow(1).getCell(0), "Nombre del director del Proyecto:", safe(data.directorNombre()));
        setBodyCellValuePreservingStyle(table.getRow(1).getCell(1), "Nombre del patrocinador del proyecto:", safe(data.patrocinadorNombre()));

        setBodyCellValuePreservingStyle(table.getRow(2).getCell(0), "Firma del director del Proyecto:", "________________________________");
        setBodyCellValuePreservingStyle(table.getRow(2).getCell(1), "Firma del patrocinador del proyecto:", "________________________________");

        setBodyCellValuePreservingStyle(table.getRow(3).getCell(0), "Fecha:", safe(data.fechaCierre()));
        replaceText(doc, "[xxxx]", safe(data.nombreProyecto()));
    }

    private XWPFTable bodyTable(XWPFDocument doc, int index) {
        List<XWPFTable> tables = doc.getTables();
        if (index < 0 || index >= tables.size()) {
            throw new IllegalStateException("La plantilla DOCX no contiene la tabla esperada en la posicion " + index + ".");
        }
        return tables.get(index);
    }

    private void setBodyCellValuePreservingStyle(XWPFTableCell cell, String label, String value) {
        if (cell == null) return;
        
        // Asume que la celda tiene una estructura de label (título) y valor
        XWPFParagraph labelParagraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphArray(0);
        
        // Conservamos los estilos del label, típicamente en negrita
        StyleConfig labelStyle = extractStyle(labelParagraph);
        applyTextToParagraph(labelParagraph, label, labelStyle);
        
        // El valor estará en un párrafo separado
        XWPFParagraph valueParagraph;
        if (cell.getParagraphs().size() > 1) {
            valueParagraph = cell.getParagraphs().get(1);
        } else {
            valueParagraph = cell.addParagraph();
        }
        
        StyleConfig valueStyle = extractStyle(valueParagraph);
        if (valueStyle.fontFamily == null && labelStyle.fontFamily != null) {
            // Si el valor no tenia formato, copiamos el del label pero sin negrita
            valueStyle.fontFamily = labelStyle.fontFamily;
            valueStyle.fontSize = labelStyle.fontSize;
            valueStyle.bold = false;
        }
        
        applyTextToParagraph(valueParagraph, value, valueStyle);
        
        // Eliminamos párrafos sobrantes
        while (cell.getParagraphs().size() > 2) {
            cell.removeParagraph(cell.getParagraphs().size() - 1);
        }
    }

    private void setCellTextPreservingStyle(XWPFTableCell cell, String text) {
        if (cell == null) return;

        XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphArray(0);
        StyleConfig style = extractStyle(paragraph);

        // Limpiar párrafos adicionales que existieran en la plantilla de prueba
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }

        applyTextToParagraph(paragraph, text, style);
    }

    private void applyTextToParagraph(XWPFParagraph paragraph, String text, StyleConfig style) {
        // Limpiar contenido existente del párrafo
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        String safeText = text == null ? "" : text;
        String[] lines = safeText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            XWPFRun run = paragraph.createRun();
            if (style.fontFamily != null) run.setFontFamily(style.fontFamily);
            if (style.fontSize != null) run.setFontSize(style.fontSize);
            if (style.bold != null) run.setBold(style.bold);
            if (style.color != null) run.setColor(style.color);
            if (style.italic != null) run.setItalic(style.italic);
            
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
        
        if (style.alignment != null) {
            paragraph.setAlignment(style.alignment);
        }
    }

    private static class StyleConfig {
        String fontFamily;
        Integer fontSize;
        Boolean bold;
        Boolean italic;
        String color;
        ParagraphAlignment alignment;
    }

    private StyleConfig extractStyle(XWPFParagraph paragraph) {
        StyleConfig config = new StyleConfig();
        config.alignment = paragraph.getAlignment();
        
        if (!paragraph.getRuns().isEmpty()) {
            XWPFRun firstRun = paragraph.getRuns().get(0);
            config.fontFamily = firstRun.getFontFamily();
            if (firstRun.getFontSizeAsDouble() != null) {
                config.fontSize = firstRun.getFontSizeAsDouble().intValue();
            }
            config.bold = firstRun.isBold();
            config.italic = firstRun.isItalic();
            config.color = firstRun.getColor();
        }
        return config;
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

        StyleConfig style = extractStyle(paragraph);
        
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        
        XWPFRun run = paragraph.createRun();
        if (style.fontFamily != null) run.setFontFamily(style.fontFamily);
        if (style.fontSize != null) run.setFontSize(style.fontSize);
        if (style.bold != null) run.setBold(style.bold);
        if (style.color != null) run.setColor(style.color);
        if (style.italic != null) run.setItalic(style.italic);
        
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
