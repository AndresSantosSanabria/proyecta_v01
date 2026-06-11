package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Patrocinador;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Genera el PDF "Estado de proyecto específico" con el mismo layout
 * institucional del formato de referencia.
 */
public final class EstadoProyectoEspecificoPdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();

    private static final float LEFT = 82f;
    private static final float RIGHT = 82f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float HEADER_LOGO_X = 14f;
    private static final float HEADER_LOGO_Y = 781f;
    private static final float HEADER_LOGO_W = 156f;
    private static final float HEADER_LOGO_H = 44f;
    private static final float FOOTER_X = 96f;
    private static final float FOOTER_Y = 0f;
    private static final float FOOTER_W = 404f;
    private static final float FOOTER_H = 60f;
    private static final float FOOTER_TOP_LIMIT = 92f;

    private static final Color COLOR_TEXT = new Color(18, 18, 18);
    private static final Color COLOR_MUTED = new Color(58, 58, 58);
    private static final Color COLOR_RULE = new Color(152, 152, 152);

    private enum DetailMode {
        RESUMIDO,
        DETALLADO;

        static DetailMode from(String value) {
            if (value != null && value.trim().equalsIgnoreCase("detallado")) {
                return DETALLADO;
            }
            return RESUMIDO;
        }

        boolean isDetailed() {
            return this == DETALLADO;
        }
    }

    public byte[] build(Proyecto proyecto,
                        List<Fase> fases,
                        List<ObjetivoEspecifico> objetivos,
                        List<Entregable> entregablesPendientes,
                        List<Entregable> entregablesConformes,
                        String detailMode) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = loadFonts(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);

            canvas.renderPageOne(proyecto, objetivos, fases, entregablesPendientes, entregablesConformes, mode.isDetailed());
            if (mode.isDetailed()) {
                canvas.renderPageTwo(fases);
                canvas.renderPageThree(proyecto, entregablesPendientes, entregablesConformes);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de proyecto específico.", ex);
        }
    }

    private static FontPack loadFonts(PDDocument document) {
        List<String[]> candidates = List.of(
                new String[]{"C:\\Windows\\Fonts\\calibri.ttf", "C:\\Windows\\Fonts\\calibrib.ttf"},
                new String[]{"C:\\Windows\\Fonts\\arial.ttf", "C:\\Windows\\Fonts\\arialbd.ttf"}
        );

        for (String[] candidate : candidates) {
            try {
                File regular = new File(candidate[0]);
                File bold = new File(candidate[1]);
                if (regular.isFile() && bold.isFile()) {
                    return new FontPack(
                            PDType0Font.load(document, regular),
                            PDType0Font.load(document, bold),
                            true
                    );
                }
            } catch (IOException ignored) {
                // Se intenta con el siguiente candidato.
            }
        }

        return new FontPack(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                false
        );
    }

    private record FontPack(PDFont regular, PDFont bold, boolean unicode) {
    }

    private static final class PdfCanvas {
        private final PDDocument document;
        private final FontPack fonts;
        private PDPageContentStream content;

        private PdfCanvas(PDDocument document, FontPack fonts) {
            this.document = document;
            this.fonts = fonts;
        }

        void renderPageOne(Proyecto proyecto,
                           List<ObjetivoEspecifico> objetivos,
                           List<Fase> fases,
                           List<Entregable> entregablesPendientes,
                           List<Entregable> entregablesConformes,
                           boolean detailed) throws IOException {
            startPage();
            drawHeaderLogo();

            float y = 730f;
            drawText("REPORTE: ESTADO DE PROYECTO EN ESPECÍFICO.", fonts.bold(), 17.2f, LEFT, y, COLOR_TEXT);

            y -= 36f;
            drawLabelValueLine("Fecha del reporte:", formatDate(LocalDate.now()), y);
            y -= 26f;
            drawLabelValueLine("Vigencia:", safe(vigenciaDelProyecto(proyecto)), y);

            y -= 56f;
            drawSectionTitle("1. INFORMACIÓN GENERAL DEL PROYECTO", y);
            y -= 34f;
            y = drawKeyValueRow("Código del proyecto", safe(proyecto.getId()), y);
            y = drawKeyValueRow("Nombre del proyecto", safe(proyecto.getNombre()), y);
            y = drawKeyValueRow("Dependencia", safe(proyecto.getDependencia()), y);
            y = drawKeyValueRow("Patrocinador del proyecto", sponsorValue(proyecto.getPatrocinador()), y);
            y = drawKeyValueRow("Director del proyecto", safe(proyecto.getDirector()), y);

            drawRule(y - 4f);
            y -= 34f;

            drawSectionTitle("2. OBJETIVOS", y);
            y -= 28f;
            drawSubsection("2.1 Objetivo general", y);
            y -= 22f;
            y = drawParagraph(valueOrPlaceholder(proyecto.getObjetivoGeneral(), "[]"), LEFT, y, CONTENT_WIDTH, 10.7f, 13.2f);

            y -= 6f;
            drawSubsection("2.2 Objetivos específicos", y);
            y -= 24f;
            List<String> objetivosTexto = objetivos == null || objetivos.isEmpty()
                    ? List.of("No hay objetivos específicos registrados.")
                    : objetivos.stream()
                    .map(ObjetivoEspecifico::getDescripcion)
                    .filter(v -> v != null && !v.isBlank())
                    .toList();
            for (String objetivo : objetivosTexto) {
                y = drawBullet(objetivo, y);
            }

            drawRule(y - 4f);
            y -= 28f;

            drawSectionTitle("3. AVANCE DEL PROYECTO", y);
            y -= 28f;
            drawSubsection("3.1 Avance total del proyecto", y);
            y -= 22f;
            drawText("Porcentaje de avance general: " + formatPercent(proyecto.getAvanceTotal()),
                    fonts.regular(), 10.7f, LEFT, y, COLOR_TEXT);
            y -= 26f;

            drawStateCheckboxes(proyecto.getEstado(), y);
            if (!detailed) {
                y -= 22f;
                drawText("VersiÃ³n resumida: para ver fases, hitos y entregables use el modo detallado.",
                        fonts.regular(), 9.3f, LEFT, y, COLOR_MUTED);
            }
            finishPage();
        }

        void renderPageTwo(List<Fase> fases) throws IOException {
            startPage();
            drawHeaderLogo();

            float y = 666f;
            drawSubsection("3.2 Avance por fases", y);
            y -= 28f;
            TableBlock phases = new TableBlock(
                    new String[]{"Fase", "Descripción", "% Avance", "Estado"},
                    new float[]{0.13f, 0.42f, 0.15f, 0.30f},
                    phasesRows(fases)
            );
            y = drawPlainTable(phases, y, 10.0f, 11.0f, 14.0f);

            drawRule(y - 8f);
            y -= 28f;

            drawSubsection("3.3 Avance por hitos", y);
            y -= 28f;
            TableBlock milestones = new TableBlock(
                    new String[]{"Hito", "Descripción", "Estado", "% Avance"},
                    new float[]{0.13f, 0.33f, 0.36f, 0.18f},
                    milestonesRows(fases)
            );
            y = drawPlainTable(milestones, y, 10.0f, 11.0f, 14.0f);

            drawRule(y - 8f);
            y -= 28f;
            drawSectionTitle("4. ENTREGABLES", y);
            finishPage();
        }

        void renderPageThree(Proyecto proyecto,
                             List<Entregable> entregablesPendientes,
                             List<Entregable> entregablesConformes) throws IOException {
            startPage();
            drawHeaderLogo();

            float y = 666f;
            drawSubsection("4.1 Entregables pendientes de acuerdo a la fecha del reporte", y);
            y -= 28f;
            TableBlock pendientes = new TableBlock(
                    new String[]{"Entregable", "Fase / Hito asociado", "Fecha planificada", "Responsable", "Estado actual", "Fecha de entrega estimada"},
                    new float[]{0.20f, 0.18f, 0.14f, 0.16f, 0.18f, 0.14f},
                    pendientesRows(proyecto, entregablesPendientes)
            );
            y = drawPlainTable(pendientes, y, 9.4f, 10.5f, 13.5f);

            drawRule(y - 8f);
            y -= 28f;

            drawSubsection("4.2 Entregables a conformidad (Ok)", y);
            y -= 28f;
            TableBlock conformes = new TableBlock(
                    new String[]{"Entregable", "Fase / Hito asociado", "Fecha de entrega", "Fecha de aprobación", "Aprobado por"},
                    new float[]{0.23f, 0.18f, 0.16f, 0.18f, 0.25f},
                    conformesRows(proyecto, entregablesConformes)
            );
            drawPlainTable(conformes, y, 9.8f, 10.5f, 13.5f);
            finishPage();
        }

        private List<List<String>> phasesRows(List<Fase> fases) {
            if (fases == null || fases.isEmpty()) {
                return List.of(List.of("Sin fases registradas", "No aplica", "0.00%", "Pendiente"));
            }
            return fases.stream()
                    .sorted(Comparator.comparing(Fase::getId, Comparator.nullsLast(Integer::compareTo)))
                    .map(fase -> List.of(
                            safe(fase.getNombre()),
                            safe(fase.getDescripcion()),
                            formatPercent(fase.getAvanceCalculado()),
                            estadoFase(fase)
                    ))
                    .toList();
        }

        private List<List<String>> milestonesRows(List<Fase> fases) {
            List<List<String>> rows = new ArrayList<>();
            if (fases != null) {
                fases.stream()
                        .sorted(Comparator.comparing(Fase::getId, Comparator.nullsLast(Integer::compareTo)))
                        .forEach(fase -> {
                            List<Hito> hitos = fase.getHitos() == null ? List.of() : fase.getHitos().stream()
                                    .sorted(Comparator.comparing(Hito::getId, Comparator.nullsLast(Integer::compareTo)))
                                    .toList();
                            for (Hito hito : hitos) {
                                rows.add(List.of(
                                        safe(hito.getNombre()),
                                        safe(hito.getDescripcion()),
                                        estadoHito(hito),
                                        formatPercent(hito.getAvanceCalculado())
                                ));
                            }
                        });
            }
            if (rows.isEmpty()) {
                rows.add(List.of("Sin hitos registrados", "No aplica", "Pendiente", "0.00%"));
            }
            return rows;
        }

        private List<List<String>> pendientesRows(Proyecto proyecto, List<Entregable> entregables) {
            List<List<String>> rows = new ArrayList<>();
            if (entregables != null) {
                for (Entregable entregable : entregables) {
                    rows.add(List.of(
                            safe(entregable.getNombre()),
                            associatedLabel(entregable),
                            formatDateOrFallback(entregable.getFechaLimite(), entregable.getFechaEntregaReal()),
                            safe(proyecto.getDirector()),
                            estadoEntregableReporte(entregable),
                            formatDateOrFallback(entregable.getFechaEntregaReal(), entregable.getFechaLimite())
                    ));
                }
            }
            if (rows.isEmpty()) {
                rows.add(List.of("Sin entregables vencidos", "No aplica", "No aplica", "No aplica", "No aplica", "No aplica"));
            }
            return rows;
        }

        private List<List<String>> conformesRows(Proyecto proyecto, List<Entregable> entregables) {
            List<List<String>> rows = new ArrayList<>();
            if (entregables != null) {
                for (Entregable entregable : entregables) {
                    rows.add(List.of(
                            safe(entregable.getNombre()),
                            associatedLabel(entregable),
                            formatDateOrFallback(entregable.getFechaEntregaReal(), entregable.getFechaLimite()),
                            formatDateOrFallback(entregable.getFechaEntregaReal(), entregable.getFechaLimite()),
                            safe(projectoApprover(proyecto))
                    ));
                }
            }
            if (rows.isEmpty()) {
                rows.add(List.of("Sin entregables a conformidad", "No aplica", "No aplica", "No aplica", "No aplica"));
            }
            return rows;
        }

        private String projectoApprover(Proyecto proyecto) {
            if (proyecto == null) {
                return "No disponible";
            }
            Patrocinador patrocinador = proyecto.getPatrocinador();
            if (patrocinador == null) {
                return safe(proyecto.getDirector());
            }
            String nombre = safe(patrocinador.getNombre());
            String cargo = safe(patrocinador.getCargo());
            if (!nombre.isBlank() && !cargo.isBlank()) {
                return nombre + " - " + cargo;
            }
            if (!nombre.isBlank()) {
                return nombre;
            }
            return safe(proyecto.getDirector());
        }

        private void drawStateCheckboxes(EstadoProyecto estadoProyecto, float y) throws IOException {
            String[] labels = {"Activo", "Con retrasos", "Finalizado"};
            boolean[] selected = {
                    EstadoProyecto.ACTIVO.equals(estadoProyecto),
                    EstadoProyecto.CON_RETRASOS.equals(estadoProyecto),
                    EstadoProyecto.CERRADO.equals(estadoProyecto)
            };

            drawText("Estado general:", fonts.bold(), 10.7f, LEFT, y, COLOR_TEXT);
            float x = LEFT + 86f;
            for (int i = 0; i < labels.length; i++) {
                drawCheckbox(x, y + 2f, selected[i]);
                x += 14f;
                drawText(labels[i], fonts.regular(), 10.7f, x, y, COLOR_TEXT);
                x += stringWidth(fonts.regular(), 10.7f, labels[i]) + 18f;
            }
        }

        private float drawKeyValueRow(String label, String value, float y) throws IOException {
            float labelWidth = 174f;
            float valueX = LEFT + labelWidth + 12f;
            float valueWidth = PAGE_WIDTH - RIGHT - valueX;
            List<String> valueLines = wrap(value, fonts.regular(), 10.6f, valueWidth);
            float rowHeight = Math.max(24f, valueLines.size() * 13f);

            drawText(label, fonts.bold(), 10.7f, LEFT, y, COLOR_TEXT);
            drawWrapped(valueLines, fonts.regular(), 10.6f, valueX, y, 13f, COLOR_TEXT);
            return y - rowHeight - 10f;
        }

        private void drawLabelValueLine(String label, String value, float y) throws IOException {
            drawText(label + " ", fonts.bold(), 10.7f, LEFT, y, COLOR_TEXT);
            float labelWidth = stringWidth(fonts.bold(), 10.7f, label + " ");
            drawText(value, fonts.regular(), 10.7f, LEFT + labelWidth, y, COLOR_TEXT);
        }

        private void drawSectionTitle(String text, float y) throws IOException {
            drawText(text, fonts.bold(), 12.2f, LEFT, y, COLOR_TEXT);
        }

        private void drawSubsection(String text, float y) throws IOException {
            drawText(text, fonts.bold(), 11.0f, LEFT, y, COLOR_TEXT);
        }

        private float drawParagraph(String text, float x, float y, float width, float fontSize, float leading) throws IOException {
            List<String> lines = wrap(text, fonts.regular(), fontSize, width);
            drawWrapped(lines, fonts.regular(), fontSize, x, y, leading, COLOR_TEXT);
            return y - (lines.size() * leading) - 4f;
        }

        private float drawBullet(String text, float y) throws IOException {
            List<String> lines = wrap(text, fonts.regular(), 10.6f, CONTENT_WIDTH - 26f);
            drawText("•", fonts.bold(), 12f, LEFT + 12f, y, COLOR_TEXT);
            drawWrapped(lines, fonts.regular(), 10.6f, LEFT + 28f, y, 13f, COLOR_TEXT);
            return y - (lines.size() * 13f) - 14f;
        }

        private void drawRule(float y) throws IOException {
            ensurePageActive();
            content.setStrokingColor(COLOR_RULE);
            content.setLineWidth(0.8f);
            content.moveTo(LEFT, y);
            content.lineTo(PAGE_WIDTH - RIGHT, y);
            content.stroke();
            content.setStrokingColor(COLOR_TEXT);
        }

        private float drawPlainTable(TableBlock table, float topY, float headerFontSize, float cellFontSize, float rowLeading) throws IOException {
            float[] widths = table.normalizedWidths();
            float x = LEFT;
            float headerTop = topY;
            float headerHeight = 0f;
            List<List<String>> headerLinesCache = new ArrayList<>();
            for (int i = 0; i < table.headers.length; i++) {
                float columnWidth = CONTENT_WIDTH * widths[i];
                List<String> headerLines = wrap(table.headers[i], fonts.bold(), headerFontSize, columnWidth - 4f);
                headerLinesCache.add(headerLines);
                headerHeight = Math.max(headerHeight, Math.max(16f, headerLines.size() * rowLeading));
            }

            for (int i = 0; i < table.headers.length; i++) {
                float columnWidth = CONTENT_WIDTH * widths[i];
                drawWrapped(headerLinesCache.get(i), fonts.bold(), headerFontSize, x + 2f, headerTop, rowLeading, COLOR_TEXT);
                x += columnWidth;
            }

            float cursorY = headerTop - headerHeight - 8f;
            for (List<String> row : table.rows) {
                float rowHeight = computeRowHeight(row, widths, cellFontSize, rowLeading);
                if (cursorY - rowHeight < FOOTER_TOP_LIMIT) {
                    finishPage();
                    startPage();
                    drawHeaderLogo();
                    cursorY = 680f;
                    headerTop = cursorY;
                    x = LEFT;
                    headerLinesCache = new ArrayList<>();
                    headerHeight = 0f;
                    for (int i = 0; i < table.headers.length; i++) {
                        float columnWidth = CONTENT_WIDTH * widths[i];
                        List<String> headerLines = wrap(table.headers[i], fonts.bold(), headerFontSize, columnWidth - 4f);
                        headerLinesCache.add(headerLines);
                        headerHeight = Math.max(headerHeight, Math.max(16f, headerLines.size() * rowLeading));
                    }
                    for (int i = 0; i < table.headers.length; i++) {
                        float columnWidth = CONTENT_WIDTH * widths[i];
                        drawWrapped(headerLinesCache.get(i), fonts.bold(), headerFontSize, x + 2f, headerTop, rowLeading, COLOR_TEXT);
                        x += columnWidth;
                    }
                    cursorY = headerTop - headerHeight - 8f;
                }

                float cellX = LEFT;
                for (int i = 0; i < widths.length; i++) {
                    float cellWidth = CONTENT_WIDTH * widths[i];
                    String cellText = i < row.size() ? row.get(i) : "";
                    List<String> cellLines = wrap(cellText, fonts.regular(), cellFontSize, cellWidth - 4f);
                    drawWrapped(cellLines, fonts.regular(), cellFontSize, cellX + 2f, cursorY, rowLeading, COLOR_TEXT);
                    cellX += cellWidth;
                }
                cursorY -= rowHeight + 10f;
            }
            return cursorY;
        }

        private float computeRowHeight(List<String> row, float[] widths, float fontSize, float leading) throws IOException {
            float maxLines = 1f;
            for (int i = 0; i < widths.length; i++) {
                String value = i < row.size() ? row.get(i) : "";
                float width = CONTENT_WIDTH * widths[i] - 4f;
                int lines = wrap(value, fonts.regular(), fontSize, width).size();
                maxLines = Math.max(maxLines, lines);
            }
            return Math.max(28f, maxLines * leading);
        }

        private void startPage() throws IOException {
            closeContent();
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
        }

        private void finishPage() throws IOException {
            drawFooter();
            closeContent();
        }

        private void closeContent() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private void drawHeaderLogo() throws IOException {
            PDImageXObject logo = loadImage("/report-assets/logo gob cun.png");
            if (logo != null) {
                content.drawImage(logo, HEADER_LOGO_X, HEADER_LOGO_Y, HEADER_LOGO_W, HEADER_LOGO_H);
            }
        }

        private void drawFooter() throws IOException {
            PDImageXObject footer = loadImage("/report-assets/STD.png");
            if (footer != null) {
                content.drawImage(footer, FOOTER_X, FOOTER_Y, FOOTER_W, FOOTER_H);
            }
        }

        private PDImageXObject loadImage(String classpath) throws IOException {
            ClassPathResource resource = new ClassPathResource(classpath);
            if (!resource.exists()) {
                return null;
            }
            try (var input = resource.getInputStream()) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) {
                    return null;
                }
                return LosslessFactory.createFromImage(document, image);
            }
        }

        private void drawCheckbox(float x, float y, boolean checked) throws IOException {
            content.setStrokingColor(COLOR_TEXT);
            content.setLineWidth(0.8f);
            content.addRect(x, y, 10f, 10f);
            content.stroke();
            if (checked) {
                drawText("X", fonts.bold(), 8.5f, x + 2.2f, y + 1.3f, COLOR_TEXT);
            }
            content.setStrokingColor(COLOR_TEXT);
        }

        private void ensurePageActive() {
            if (content == null) {
                throw new IllegalStateException("No hay una página activa para dibujar.");
            }
        }

        private void drawText(String text, PDFont font, float size, float x, float y, Color color) throws IOException {
            ensurePageActive();
            content.beginText();
            content.setNonStrokingColor(color);
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(normalizeForFont(text));
            content.endText();
            content.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawWrapped(List<String> lines, PDFont font, float fontSize, float x, float topY, float leading, Color color) throws IOException {
            float currentY = topY;
            for (String line : lines) {
                drawText(line, font, fontSize, x, currentY, color);
                currentY -= leading;
            }
        }

        private List<String> wrap(String text, PDFont font, float fontSize, float width) throws IOException {
            String value = text == null || text.isBlank() ? "No disponible" : text;
            String normalized = normalizeForFont(value);
            String[] paragraphs = normalized.split("\\R");
            List<String> result = new ArrayList<>();
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    result.add("");
                    continue;
                }
                String[] words = paragraph.split("\\s+");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (stringWidth(font, fontSize, candidate) <= width) {
                        line.setLength(0);
                        line.append(candidate);
                    } else {
                        if (!line.isEmpty()) {
                            result.add(line.toString());
                            line.setLength(0);
                        }
                        if (stringWidth(font, fontSize, word) <= width) {
                            line.append(word);
                        } else {
                            result.addAll(breakLongWord(word, font, fontSize, width));
                        }
                    }
                }
                if (!line.isEmpty()) {
                    result.add(line.toString());
                }
            }
            if (result.isEmpty()) {
                result.add("No disponible");
            }
            return result;
        }

        private List<String> breakLongWord(String word, PDFont font, float fontSize, float width) throws IOException {
            List<String> parts = new ArrayList<>();
            StringBuilder buffer = new StringBuilder();
            for (char ch : word.toCharArray()) {
                String candidate = buffer.toString() + ch;
                if (stringWidth(font, fontSize, candidate) <= width) {
                    buffer.append(ch);
                } else {
                    if (!buffer.isEmpty()) {
                        parts.add(buffer.toString());
                    }
                    buffer.setLength(0);
                    buffer.append(ch);
                }
            }
            if (!buffer.isEmpty()) {
                parts.add(buffer.toString());
            }
            return parts;
        }

        private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
            String safe = normalizeForFont(text);
            return font.getStringWidth(safe) / 1000f * fontSize;
        }

        private String normalizeForFont(String text) {
            String value = text == null ? "" : text;
            if (fonts.unicode()) {
                return value;
            }
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            return normalized.replace('\u00a0', ' ');
        }

        private String safe(String value) {
            return value == null || value.isBlank() ? "No disponible" : value.trim();
        }

        private String valueOrPlaceholder(String value, String placeholder) {
            if (value == null || value.isBlank()) {
                return placeholder;
            }
            return value.trim();
        }

        private String sponsorValue(Patrocinador patrocinador) {
            if (patrocinador == null) {
                return "No asignado";
            }
            String nombre = safe(patrocinador.getNombre());
            String cargo = safe(patrocinador.getCargo());
            if (!nombre.equals("No disponible") && !cargo.equals("No disponible")) {
                return nombre + " - " + cargo;
            }
            if (!nombre.equals("No disponible")) {
                return nombre;
            }
            return "No asignado";
        }

        private String associatedLabel(Entregable entregable) {
            if (entregable == null || entregable.getHito() == null) {
                return "No aplica";
            }
            Hito hito = entregable.getHito();
            if (hito.getFase() == null) {
                return safe(hito.getNombre());
            }
            return safe(hito.getFase().getNombre()) + " / " + safe(hito.getNombre());
        }

        private String estadoEntregableReporte(Entregable entregable) {
            if (entregable == null) {
                return "Pendiente";
            }
            if (EstadoEntregable.A_CONFORMIDAD.equals(entregable.getEstado()) || Boolean.TRUE.equals(entregable.getConforme())) {
                return "Cumplido";
            }
            if (EstadoEntregable.COMPLETADO.equals(entregable.getEstado())) {
                return "En revisión";
            }
            if (entregable.getFechaLimite() != null && entregable.getFechaLimite().isBefore(LocalDate.now())) {
                return "Pendiente";
            }
            return "En revisión";
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

        private String formatDate(LocalDate date) {
            if (date == null) {
                return "[DD/MM/AAAA]";
            }
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        private String formatDateOrFallback(LocalDate primary, LocalDate fallback) {
            if (primary != null) {
                return formatDate(primary);
            }
            if (fallback != null) {
                return formatDate(fallback);
            }
            return "No disponible";
        }

        private String vigenciaDelProyecto(Proyecto proyecto) {
            if (proyecto == null || proyecto.getVigenciaPeti() == null || proyecto.getVigenciaPeti().isBlank()) {
                int year = LocalDate.now().getYear();
                return year + "-" + (year + 3);
            }
            return proyecto.getVigenciaPeti().trim();
        }

        private String formatPercent(BigDecimal value) {
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

    private record TableBlock(String[] headers, float[] widths, List<List<String>> rows) {
        private float[] normalizedWidths() {
            float total = 0f;
            for (float width : widths) {
                total += width;
            }
            float[] normalized = new float[widths.length];
            if (total <= 0f) {
                float equal = 1f / widths.length;
                for (int i = 0; i < widths.length; i++) {
                    normalized[i] = equal;
                }
                return normalized;
            }
            for (int i = 0; i < widths.length; i++) {
                normalized[i] = widths[i] / total;
            }
            return normalized;
        }
    }
}
