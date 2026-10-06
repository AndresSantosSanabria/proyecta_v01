package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.domain.model.Patrocinador;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.domain.model.enums.EstadoProyecto;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Genera el PDF "Estado de proyecto específico" con el mismo layout
 * institucional del formato de referencia.
 */
public final class EstadoProyectoEspecificoPdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();

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

    private static final String NO_APLICA = "No aplica";
    private static final String PORCENTAJE_CERO = "0.00%";
    private static final String PENDIENTE = "Pendiente";
    private static final String NO_DISPONIBLE = "No disponible";

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
            FontPack fonts = FontPack.load(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);

            canvas.renderPageOne(proyecto, objetivos, mode.isDetailed());
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
                           boolean detailed) throws IOException {
            startPage();
            drawHeaderLogo();

            float y = 730f;
            drawText("REPORTE: ESTADO DE PROYECTO EN ESPECÍFICO.", fonts.bold(), 17.2f, LEFT, y, COLOR_TEXT);

            y -= 36f;
            drawLabelValueLine("Fecha del reporte:", formatDate(LocalDate.now(ZoneId.systemDefault())), y);
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
                drawText("Versión resumida: para ver fases, hitos y entregables use el modo detallado.",
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

            drawSubsection("4.2 Entregables aprobados (Ok)", y);
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
                return List.of(List.of("Sin fases registradas", NO_APLICA, PORCENTAJE_CERO, PENDIENTE));
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
                rows.add(List.of("Sin hitos registrados", NO_APLICA, PENDIENTE, PORCENTAJE_CERO));
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
                rows.add(List.of("Sin entregables vencidos", NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));
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
                rows.add(List.of("Sin entregables aprobados", NO_APLICA, NO_APLICA, NO_APLICA, NO_APLICA));
            }
            return rows;
        }

        private String projectoApprover(Proyecto proyecto) {
            if (proyecto == null) {
                return NO_DISPONIBLE;
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
                    EstadoProyecto.CERRADO.equals(estadoProyecto) || EstadoProyecto.CERRADO_FORZOSO.equals(estadoProyecto)
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
            HeaderCache headerCache = buildHeaderCache(table, widths, headerFontSize, rowLeading);
            float headerTop = topY;
            float cursorY = drawHeaderRow(headerCache, headerTop, widths, headerFontSize, rowLeading);

            for (List<String> row : table.rows) {
                float rowHeight = computeRowHeight(row, widths, cellFontSize, rowLeading);
                if (cursorY - rowHeight < FOOTER_TOP_LIMIT) {
                    finishPage();
                    startPage();
                    drawHeaderLogo();
                    headerCache = buildHeaderCache(table, widths, headerFontSize, rowLeading);
                    headerTop = 680f;
                    cursorY = drawHeaderRow(headerCache, headerTop, widths, headerFontSize, rowLeading);
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

        private record HeaderCache(List<List<String>> lines, float height) {
        }

        private HeaderCache buildHeaderCache(TableBlock table, float[] widths, float headerFontSize, float rowLeading) throws IOException {
            List<List<String>> cache = new ArrayList<>();
            float height = 0f;
            for (int i = 0; i < table.headers.length; i++) {
                float columnWidth = CONTENT_WIDTH * widths[i];
                List<String> headerLines = wrap(table.headers[i], fonts.bold(), headerFontSize, columnWidth - 4f);
                cache.add(headerLines);
                height = Math.max(height, Math.max(16f, headerLines.size() * rowLeading));
            }
            return new HeaderCache(cache, height);
        }

        private float drawHeaderRow(HeaderCache cache, float topY, float[] widths, float headerFontSize, float rowLeading) throws IOException {
            float x = LEFT;
            for (int i = 0; i < cache.lines().size(); i++) {
                float columnWidth = CONTENT_WIDTH * widths[i];
                drawWrapped(cache.lines().get(i), fonts.bold(), headerFontSize, x + 2f, topY, rowLeading, COLOR_TEXT);
                x += columnWidth;
            }
            return topY - cache.height() - 8f;
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

        private static final Pattern WHITESPACE = Pattern.compile("\\s+");
        private List<String> wrap(String text, PDFont font, float fontSize, float width) throws IOException {
            String value = text == null || text.isBlank() ? NO_DISPONIBLE : text;
            String normalized = normalizeForFont(value);
            String[] paragraphs = normalized.split("\\R");
            List<String> result = new ArrayList<>();
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    result.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (String word : WHITESPACE.split(paragraph)) {
                    appendWord(word, line, result, font, fontSize, width);
                }
                if (!line.isEmpty()) {
                    result.add(line.toString());
                }
            }
            if (result.isEmpty()) {
                result.add(NO_DISPONIBLE);
            }
            return result;
        }

        private void appendWord(String word, StringBuilder line, List<String> result, PDFont font, float fontSize, float width) throws IOException {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (stringWidth(font, fontSize, candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
                return;
            }
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
            return value == null || value.isBlank() ? NO_DISPONIBLE : value.trim();
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
            if (!nombre.equals(NO_DISPONIBLE) && !cargo.equals(NO_DISPONIBLE)) {
                return nombre + " - " + cargo;
            }
            if (!nombre.equals(NO_DISPONIBLE)) {
                return nombre;
            }
            return "No asignado";
        }

        private String associatedLabel(Entregable entregable) {
            if (entregable == null || entregable.getHito() == null) {
                return NO_APLICA;
            }
            Hito hito = entregable.getHito();
            if (hito.getFase() == null) {
                return safe(hito.getNombre());
            }
            return safe(hito.getFase().getNombre()) + " / " + safe(hito.getNombre());
        }

        private String estadoEntregableReporte(Entregable entregable) {
            if (entregable == null) {
                return PENDIENTE;
            }
            if (EstadoEntregable.APROBADO.equals(entregable.getEstado()) || Boolean.TRUE.equals(entregable.getConforme())) {
                return "Aprobado";
            }
            if (EstadoEntregable.COMPLETADO.equals(entregable.getEstado())) {
                return "En revisión";
            }
            if (entregable.getFechaLimite() != null && entregable.getFechaLimite().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
                return PENDIENTE;
            }
            return "En revisión";
        }

        private String estadoFase(Fase fase) {
            if (fase == null || fase.getAvanceCalculado() == null) {
                return PENDIENTE;
            }
            if (fase.getAvanceCalculado().compareTo(new BigDecimal("100")) >= 0) {
                return "Cumplido";
            }
            if (fase.getAvanceCalculado().compareTo(BigDecimal.ZERO) > 0) {
                return "En progreso";
            }
            return PENDIENTE;
        }

        private String estadoHito(Hito hito) {
            if (hito == null || hito.getAvanceCalculado() == null) {
                return PENDIENTE;
            }
            if (hito.getAvanceCalculado().compareTo(new BigDecimal("100")) >= 0) {
                return "Cumplido";
            }
            if (hito.getAvanceCalculado().compareTo(BigDecimal.ZERO) > 0) {
                return "En progreso";
            }
            return PENDIENTE;
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
            return NO_DISPONIBLE;
        }

        private String vigenciaDelProyecto(Proyecto proyecto) {
            if (proyecto == null || proyecto.getVigenciaPeti() == null || proyecto.getVigenciaPeti().isBlank()) {
                int year = LocalDate.now(ZoneId.systemDefault()).getYear();
                return year + "-" + (year + 3);
            }
            return proyecto.getVigenciaPeti().trim();
        }

        private String formatPercent(BigDecimal value) {
            if (value == null) {
                return PORCENTAJE_CERO;
            }
            BigDecimal normalized = value;
            if (normalized.compareTo(BigDecimal.ONE) <= 0) {
                normalized = normalized.multiply(BigDecimal.valueOf(100));
            }
            return normalized.setScale(2, RoundingMode.HALF_UP) + "%";
        }
    }

    private record TableBlock(String[] headers, float[] widths, List<List<String>> rows) {

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof TableBlock(String[] otherHeaders, float[] otherWidths,
                    List<List<String>> otherRows))) return false;
            return Arrays.equals(headers, otherHeaders)
                    && Arrays.equals(widths, otherWidths)
                    && Objects.equals(rows, otherRows);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(headers), Arrays.hashCode(widths), rows);
        }

        @Override
        public String toString() {
            return "TableBlock[headers=" + Arrays.toString(headers) + ", widths=" + Arrays.toString(widths)
                    + ", rows=" + rows + "]";
        }

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