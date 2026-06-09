package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.dto.report.RiesgoVerificacionReporteDTO;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RiesgosVerificacionPdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    private static final float LEFT = 85f;
    private static final float RIGHT = 85f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float HEADER_LOGO_X = 14f;
    private static final float HEADER_LOGO_Y = 781f;
    private static final float HEADER_LOGO_W = 156f;
    private static final float HEADER_LOGO_H = 44f;
    private static final float FOOTER_X = 40f;
    private static final float FOOTER_Y = 0f;
    private static final float FOOTER_W = 470f;
    private static final float FOOTER_H = 60f;
    private static final float FOOTER_TOP_LIMIT = 92f;

    private static final Color COLOR_TEXT = new Color(18, 18, 18);
    private static final Color COLOR_MUTED = new Color(58, 58, 58);
    private static final Color COLOR_BORDER = new Color(152, 152, 152);

    public byte[] build(List<RiesgoVerificacionReporteDTO> reportes, LocalDate corte) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = loadFonts(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            canvas.render(reportes, corte);
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de riesgos.", ex);
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
            }
        }

        return new FontPack(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                false
        );
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "[DD/MM/AAAA]" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "SI" : "NO";
    }

    private record FontPack(PDFont regular, PDFont bold, boolean unicode) {
    }

    private record RiskRow(String codigo, String nombre, String director, String dependencia, boolean diligencio) {
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

        void render(List<RiesgoVerificacionReporteDTO> reportes, LocalDate corte) throws IOException {
            List<RiskRow> rows = buildRows(reportes);
            int totalYes = (int) rows.stream().filter(RiskRow::diligencio).count();
            int totalNo = rows.size() - totalYes;

            startPage();
            renderHeader();
            renderTitle();
            renderMeta(corte);
            renderSummary(totalYes, totalNo);
            renderTableHeading();

            if (rows.isEmpty()) {
                drawEmptyState("No hay proyectos con cierre registrados.");
            } else {
                for (RiskRow row : rows) {
                    float rowHeight = rowHeight(row);
                    if (cursorY - rowHeight < FOOTER_TOP_LIMIT) {
                        finishPage();
                        startPage();
                        renderHeader();
                        renderTitle();
                        renderMeta(corte);
                        renderSummary(totalYes, totalNo);
                        renderTableHeading();
                    }
                    drawRow(row, rowHeight);
                    cursorY -= rowHeight;
                }
            }

            finishPage();
        }

        private List<RiskRow> buildRows(List<RiesgoVerificacionReporteDTO> reportes) {
            if (reportes == null || reportes.isEmpty()) {
                return List.of();
            }
            return reportes.stream()
                    .sorted(Comparator.comparing(RiesgoVerificacionReporteDTO::proyectoId, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .map(reporte -> new RiskRow(
                            safe(reporte.proyectoId()),
                            safe(reporte.nombreProyecto()),
                            safe(reporte.directorProyecto()),
                            safe(reporte.dependencia()),
                            reporte.diligencioTratamiento()
                    ))
                    .toList();
        }

        private void startPage() throws IOException {
            closeContent();
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            cursorY = PAGE_HEIGHT - 40f;
        }

        private void finishPage() throws IOException {
            drawFooter();
            closeContent();
        }

        private void renderHeader() throws IOException {
            if (headerLogo != null) {
                content.drawImage(headerLogo, HEADER_LOGO_X, HEADER_LOGO_Y, HEADER_LOGO_W, HEADER_LOGO_H);
            }
        }

        private void renderTitle() throws IOException {
            drawText("REPORTE: VERIFICACIÓN DE TRATAMIENTO A RIESGOS.", fonts.bold(), 17.2f, LEFT, 730f, COLOR_TEXT);
        }

        private void renderMeta(LocalDate corte) throws IOException {
            drawLabelValue("FILTRO:", "[Proyectos con cierre]", 690f, true);
            drawLabelValue("Fecha del reporte:", formatDate(corte), 650f, false);
            drawLabelValue("Vigencia:", "[2024-2027]", 625f, false);
            cursorY = 560f;
        }

        private void renderSummary(int totalYes, int totalNo) throws IOException {
            drawLabelValue("Conteo de proyectos que diligenciaron tratamiento de riesgos:", String.valueOf(totalYes), 560f, false);
            drawLabelValue("Conteo de proyectos que NO diligenciaron tratamiento de riesgos:", String.valueOf(totalNo), 538f, false);
            cursorY = 500f;
        }

        private void renderTableHeading() throws IOException {
            drawTableHeader(cursorY);
            cursorY -= headerHeight();
        }

        private void drawEmptyState(String message) throws IOException {
            drawText(message, fonts.regular(), 10.4f, LEFT, cursorY - 6f, COLOR_MUTED);
            cursorY -= 30f;
        }

        private void drawRow(RiskRow row, float rowHeight) throws IOException {
            float x = LEFT;
            float y = cursorY;
            float[] widths = widths();
            String[] values = {
                    row.codigo(),
                    row.nombre(),
                    row.director(),
                    row.dependencia(),
                    yesNo(row.diligencio())
            };

            drawFilledRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, Color.WHITE);
            drawRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_BORDER, 0.6f);

            for (int i = 0; i < widths.length; i++) {
                float colWidth = CONTENT_WIDTH * widths[i];
                if (i > 0) {
                    drawVerticalLine(x, y, y - rowHeight, COLOR_BORDER, 0.6f);
                }
                float fontSize = i == 4 ? 10.2f : 9.2f;
                List<String> wrapped = wrap(values[i], fonts.regular(), fontSize, colWidth - 8f);
                drawWrapped(wrapped, fonts.regular(), fontSize, x + 4f, y - 11f, 10.8f, COLOR_TEXT);
                x += colWidth;
            }
        }

        private void drawTableHeader(float topY) throws IOException {
            String[] headers = {
                    "Codigo del\nproyecto",
                    "Nombre del\nproyecto",
                    "Director del\nproyecto",
                    "Dependencia",
                    "Diligencio\ntratamiento de\nriesgos"
            };
            float[] sizes = {8.8f, 8.8f, 8.6f, 8.8f, 8.1f};
            float[] widths = widths();
            float maxHeight = 0f;
            List<List<String>> wrappedHeaders = new ArrayList<>();

            for (int i = 0; i < headers.length; i++) {
                float colWidth = CONTENT_WIDTH * widths[i];
                List<String> wrapped = wrap(headers[i], fonts.bold(), sizes[i], colWidth - 8f);
                wrappedHeaders.add(wrapped);
                maxHeight = Math.max(maxHeight, Math.max(30f, wrapped.size() * 10.5f + 8f));
            }

            float x = LEFT;
            for (int i = 0; i < headers.length; i++) {
                float colWidth = CONTENT_WIDTH * widths[i];
                drawFilledRect(x, topY - maxHeight, colWidth, maxHeight, Color.WHITE);
                drawRect(x, topY - maxHeight, colWidth, maxHeight, COLOR_BORDER, 0.6f);
                drawCenteredWrapped(wrappedHeaders.get(i), fonts.bold(), sizes[i], x, topY - 10f, colWidth, 10.5f, COLOR_MUTED);
                x += colWidth;
            }
        }

        private float headerHeight() {
            return 52f;
        }

        private float rowHeight(RiskRow row) throws IOException {
            float maxLines = 1f;
            float[] widths = widths();
            String[] values = {
                    row.codigo(),
                    row.nombre(),
                    row.director(),
                    row.dependencia(),
                    yesNo(row.diligencio())
            };
            for (int i = 0; i < widths.length; i++) {
                float fontSize = i == 4 ? 10.2f : 9.2f;
                List<String> wrapped = wrap(values[i], fonts.regular(), fontSize, CONTENT_WIDTH * widths[i] - 8f);
                maxLines = Math.max(maxLines, wrapped.size());
            }
            return Math.max(22f, maxLines * 11f + 6f);
        }

        private float[] widths() {
            return new float[]{0.20f, 0.24f, 0.20f, 0.21f, 0.15f};
        }

        private void drawLabelValue(String label, String value, float y, boolean boldValue) throws IOException {
            drawText(label, fonts.bold(), 11.2f, LEFT, y, COLOR_TEXT);
            float labelWidth = stringWidth(fonts.bold(), 11.2f, label + " ");
            drawText(value, boldValue ? fonts.bold() : fonts.regular(), 11.2f, LEFT + labelWidth, y, COLOR_TEXT);
        }

        private void drawFooter() throws IOException {
            if (footerLogo != null) {
                content.drawImage(footerLogo, FOOTER_X, FOOTER_Y, FOOTER_W, FOOTER_H);
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

        private void drawVerticalLine(float x, float topY, float bottomY, Color stroke, float lineWidth) throws IOException {
            content.setStrokingColor(stroke);
            content.setLineWidth(lineWidth);
            content.moveTo(x, bottomY);
            content.lineTo(x, topY);
            content.stroke();
            content.setStrokingColor(COLOR_TEXT);
        }

        private void drawText(String text, PDFont font, float size, float x, float y, Color color) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, y);
            content.showText(encode(text));
            content.endText();
        }

        private void drawWrapped(List<String> lines, PDFont font, float size, float x, float topY, float leading, Color color) throws IOException {
            float y = topY;
            for (String line : lines) {
                drawText(line, font, size, x, y, color);
                y -= leading;
            }
        }

        private void drawCenteredWrapped(List<String> lines, PDFont font, float size, float x, float topY, float width, float leading, Color color) throws IOException {
            float totalHeight = (lines.size() - 1) * leading;
            float startY = topY - (totalHeight / 2f);
            float y = startY;
            for (String line : lines) {
                float textWidth = stringWidth(font, size, line);
                float drawX = x + (width - textWidth) / 2f;
                drawText(line, font, size, drawX, y, color);
                y -= leading;
            }
        }

        private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            String source = text == null ? "" : text;
            String[] rawLines = source.split("\\R", -1);
            List<String> result = new ArrayList<>();
            for (String rawLine : rawLines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    result.add("");
                    continue;
                }
                StringBuilder current = new StringBuilder();
                for (String word : line.split("\\s+")) {
                    String trial = current.length() == 0 ? word : current + " " + word;
                    if (stringWidth(font, size, trial) <= maxWidth) {
                        current.setLength(0);
                        current.append(trial);
                    } else {
                        if (current.length() > 0) {
                            result.add(current.toString());
                        }
                        current.setLength(0);
                        current.append(word);
                    }
                }
                if (current.length() > 0) {
                    result.add(current.toString());
                }
            }
            return result.isEmpty() ? List.of("") : result;
        }

        private float stringWidth(PDFont font, float size, String text) throws IOException {
            String encoded = encode(text);
            return font.getStringWidth(encoded) / 1000f * size;
        }

        private String encode(String text) {
            if (fonts.unicode()) {
                return Objects.toString(text, "");
            }
            return ascii(text);
        }

        private String ascii(String text) {
            if (text == null) {
                return "";
            }
            String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "");
            StringBuilder ascii = new StringBuilder();
            for (char c : normalized.toCharArray()) {
                if (c >= 32 && c <= 126) {
                    ascii.append(c);
                } else if (Character.isWhitespace(c)) {
                    ascii.append(' ');
                }
            }
            return ascii.toString();
        }
    }

    private static PDImageXObject loadImage(PDDocument document, String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return null;
        }
        try (var inputStream = resource.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return null;
            }
            return LosslessFactory.createFromImage(document, image);
        }
    }
}
