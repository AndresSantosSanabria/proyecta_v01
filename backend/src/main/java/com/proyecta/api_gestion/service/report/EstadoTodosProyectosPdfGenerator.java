package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.dto.report.ProyectoReporteResumenDTO;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EstadoTodosProyectosPdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    private static final float LEFT = 40f;
    private static final float RIGHT = 40f;
    private static final float TOP = 40f;
    private static final float BOTTOM = 42f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float HEADER_Y = PAGE_HEIGHT - TOP;
    private static final float TITLE_Y = 730f;
    private static final float BODY_START_Y = 640f;

    private static final Color COLOR_TEXT = new Color(22, 24, 28);
    private static final Color COLOR_MUTED = new Color(94, 104, 117);
    private static final Color COLOR_BORDER = new Color(197, 207, 219);
    public byte[] build(List<ProyectoReporteResumenDTO> proyectos, LocalDate corte) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = loadFonts(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            canvas.startPage();
            canvas.renderHeader();
            canvas.renderTitle();
            canvas.renderMeta(corte);
            canvas.renderTable(proyectos);
            canvas.finishPage();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de portafolio.", ex);
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
                // fallback below
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
        private final PDImageXObject headerLogo;
        private final PDImageXObject footerLogo;
        private PDPageContentStream content;
        private float cursorY;

        private PdfCanvas(PDDocument document, FontPack fonts) throws IOException {
            this.document = document;
            this.fonts = fonts;
            this.headerLogo = loadImage(document, "/report-assets/logo gob cun.png");
            this.footerLogo = loadImage(document, "/report-assets/STD.png");
        }

        void startPage() throws IOException {
            closeContent();
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            cursorY = HEADER_Y;
        }

        void finishPage() throws IOException {
            drawFooter();
            closeContent();
        }

        void renderHeader() throws IOException {
            if (headerLogo != null) {
                content.drawImage(headerLogo, LEFT - 4f, HEADER_Y - 34f, 160f, 38f);
            }
        }

        void renderTitle() throws IOException {
            drawCenteredText("REPORTE: ESTADO DE TODOS LOS PROYECTOS.", fonts.bold(), 17.5f, 0f, TITLE_Y, PAGE_WIDTH, COLOR_TEXT);
        }

        void renderMeta(LocalDate corte) throws IOException {
            cursorY = 690f;
            drawLabelValue("FILTRO:", "[Proyectos PETI]", cursorY, true);
            cursorY -= 24f;
            drawLabelValue("Fecha del reporte:", formatDate(corte), cursorY, false);
            cursorY -= 24f;
            drawLabelValue("Vigencia:", vigenciaGlobal(), cursorY, false);
            cursorY -= 30f;
        }

        void renderTable(List<ProyectoReporteResumenDTO> proyectos) throws IOException {
            float[] widths = normalize(new float[]{18f, 36f, 22f, 24f});
            List<List<String>> rows = proyectos == null ? List.of() : proyectos.stream()
                    .sorted(Comparator.comparing(ProyectoReporteResumenDTO::id, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .map(p -> List.of(
                            safe(p.id()),
                            safe(p.nombre()),
                            formatPercent(p.avance()),
                            estadoTexto(p.estado())
                    ))
                    .toList();

            float headerTop = cursorY;
            float headerHeight = drawTableHeader(widths, headerTop);
            cursorY = headerTop - headerHeight;

            if (rows.isEmpty()) {
                return;
            }

            for (List<String> row : rows) {
                float rowHeight = computeRowHeight(row, widths, 8.7f, 12.2f);
                if (cursorY - rowHeight < BOTTOM + 40f) {
                    finishPage();
                    startPage();
                    renderHeader();
                    renderTitle();
                    renderMeta(LocalDate.now());
                    headerTop = cursorY;
                    headerHeight = drawTableHeader(widths, headerTop);
                    cursorY = headerTop - headerHeight;
                }
                drawTableRow(row, widths, rowHeight, 8.7f);
                cursorY -= rowHeight;
            }
        }

        private float drawTableHeader(float[] widths, float topY) throws IOException {
            String[] headers = {
                    "Código del proyecto",
                    "Nombre del proyecto",
                    "Avance total del proyecto (%)",
                    "Estado del proyecto (En desarrollo - cerrado)"
            };
            float[] headerSizes = {8.6f, 8.6f, 8.3f, 8.0f};
            float maxHeight = 0f;
            float x = LEFT;
            for (int i = 0; i < headers.length; i++) {
                float colWidth = CONTENT_WIDTH * widths[i];
                List<String> wrapped = wrap(headers[i], fonts.bold(), headerSizes[i], colWidth - 8f);
                float textHeight = wrapped.size() * 10.5f;
                maxHeight = Math.max(maxHeight, Math.max(24f, textHeight + 8f));
                drawFilledRect(x, topY - maxHeight, colWidth, maxHeight, Color.WHITE);
                drawRect(x, topY - maxHeight, colWidth, maxHeight, COLOR_BORDER, 0.6f);
                drawWrapped(wrapped, fonts.bold(), headerSizes[i], x + 4f, topY - 13f, 10.5f, COLOR_MUTED);
                x += colWidth;
            }
            return maxHeight;
        }

        private void drawTableRow(List<String> row, float[] widths, float rowHeight, float fontSize) throws IOException {
            float x = LEFT;
            float y = cursorY;
            drawFilledRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, Color.WHITE);
            drawRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_BORDER, 0.6f);
            for (int i = 0; i < widths.length; i++) {
                float colWidth = CONTENT_WIDTH * widths[i];
                if (i > 0) {
                    drawVerticalLine(x, y, y - rowHeight, COLOR_BORDER, 0.6f);
                }
                List<String> wrapped = wrap(i < row.size() ? row.get(i) : "", fonts.regular(), fontSize, colWidth - 8f);
                drawWrapped(wrapped, fonts.regular(), fontSize, x + 4f, y - 11f, 11.2f, COLOR_TEXT);
                x += colWidth;
            }
        }

        private float computeRowHeight(List<String> row, float[] widths, float fontSize, float leading) throws IOException {
            float maxLines = 1f;
            for (int i = 0; i < widths.length; i++) {
                String value = i < row.size() ? row.get(i) : "";
                int lines = wrap(value, fonts.regular(), fontSize, CONTENT_WIDTH * widths[i] - 8f).size();
                maxLines = Math.max(maxLines, lines);
            }
            return Math.max(22f, maxLines * leading + 4f);
        }

        private void drawLabelValue(String label, String value, float y, boolean large) throws IOException {
            float labelSize = large ? 11.6f : 10.2f;
            float valueSize = large ? 11.6f : 10.2f;
            drawText(label, fonts.bold(), labelSize, LEFT, y, COLOR_TEXT);
            float labelWidth = stringWidth(fonts.bold(), labelSize, label + " ");
            drawText(value, fonts.regular(), valueSize, LEFT + labelWidth, y, COLOR_TEXT);
        }

        private void drawFooter() throws IOException {
            if (footerLogo != null) {
                content.drawImage(footerLogo, 40f, 0f, 470f, 60f);
            }
        }

        private void closeContent() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private void drawFilledRect(float x, float y, float width, float height, Color fill) throws IOException {
            content.setNonStrokingColor(fill);
            content.addRect(x, y, width, height);
            content.fill();
            content.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawRect(float x, float y, float width, float height, Color stroke, float lineWidth) throws IOException {
            content.setStrokingColor(stroke);
            content.setLineWidth(lineWidth);
            content.addRect(x, y, width, height);
            content.stroke();
            content.setStrokingColor(COLOR_TEXT);
        }

        private void drawVerticalLine(float x, float yTop, float yBottom, Color stroke, float lineWidth) throws IOException {
            content.setStrokingColor(stroke);
            content.setLineWidth(lineWidth);
            content.moveTo(x, yTop);
            content.lineTo(x, yBottom);
            content.stroke();
            content.setStrokingColor(COLOR_TEXT);
        }

        private void drawText(String text, PDFont font, float size, float x, float y, Color color) throws IOException {
            content.beginText();
            content.setNonStrokingColor(color);
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(normalize(text));
            content.endText();
            content.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawCenteredText(String text, PDFont font, float size, float x, float y, float width, Color color) throws IOException {
            float textWidth = stringWidth(font, size, text);
            float startX = x + Math.max(0f, (width - textWidth) / 2f);
            drawText(text, font, size, startX, y, color);
        }

        private void drawWrapped(List<String> lines, PDFont font, float size, float x, float topY, float leading, Color color) throws IOException {
            float currentY = topY;
            for (String line : lines) {
                drawText(line, font, size, x, currentY, color);
                currentY -= leading;
            }
        }

        private List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
            String value = text == null || text.isBlank() ? "No disponible" : text;
            String[] words = normalize(value).split("\\s+");
            List<String> result = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (stringWidth(font, size, candidate) <= width) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (!line.isEmpty()) {
                        result.add(line.toString());
                        line.setLength(0);
                    }
                    if (stringWidth(font, size, word) <= width) {
                        line.append(word);
                    } else {
                        result.add(word);
                    }
                }
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            if (result.isEmpty()) {
                result.add("No disponible");
            }
            return result;
        }

        private float stringWidth(PDFont font, float size, String text) throws IOException {
            return font.getStringWidth(normalize(text)) / 1000f * size;
        }

        private String normalize(String text) {
            if (text == null) {
                return "";
            }
            if (fonts.unicode()) {
                return text;
            }
            return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        }

        private String safe(String value) {
            return value == null || value.isBlank() ? "No disponible" : value.trim();
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

        private String estadoTexto(String estado) {
            return safe(estado);
        }

        private String formatDate(LocalDate date) {
            return date == null ? "[DD/MM/AAAA]" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        private String vigenciaGlobal() {
            int year = LocalDate.now().getYear();
            return year + "-" + (year + 3);
        }

        private PDImageXObject loadImage(PDDocument document, String classpath) throws IOException {
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

        private float[] normalize(float[] widths) {
            float total = 0f;
            for (float width : widths) {
                total += width;
            }
            float[] normalized = new float[widths.length];
            for (int i = 0; i < widths.length; i++) {
                normalized[i] = widths[i] / total;
            }
            return normalized;
        }
    }
}
