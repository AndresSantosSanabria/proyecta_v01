package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.domain.model.enums.DetailMode;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class PdfReportCanvas {

    static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    static final float LEFT = 85f;
    static final float RIGHT = 85f;
    static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    static final float HEADER_LOGO_X = 14f;
    static final float HEADER_LOGO_Y = 781f;
    static final float HEADER_LOGO_W = 156f;
    static final float HEADER_LOGO_H = 44f;
    static final float FOOTER_X = 40f;
    static final float FOOTER_Y = 0f;
    static final float FOOTER_W = 470f;
    static final float FOOTER_H = 60f;
    static final float FOOTER_TOP_LIMIT = 92f;
    static final float TABLE_HEADER_HEIGHT = 52f;

    static final Color COLOR_TEXT = new Color(18, 18, 18);
    static final Color COLOR_MUTED = new Color(58, 58, 58);
    static final Color COLOR_BORDER = new Color(152, 152, 152);

    static final String FECHA_PLACEHOLDER = "[DD/MM/AAAA]";

    private final PDDocument document;
    protected final FontPack fonts;
    private final PDImageXObject headerLogo;
    private final PDImageXObject footerLogo;
    private PDPageContentStream content;
    protected float cursorY;

    @FunctionalInterface
    interface PageFlow {
        void apply() throws IOException;
    }

    @FunctionalInterface
    interface RowHeight<T> {
        float heightOf(T row) throws IOException;
    }

    @FunctionalInterface
    interface RowPainter<T> {
        void paint(T row, float height) throws IOException;
    }

    protected PdfReportCanvas(PDDocument document, FontPack fonts) throws IOException {
        this.document = document;
        this.fonts = fonts;
        this.headerLogo = loadImage(document, "/report-assets/logo gob cun.png");
        this.footerLogo = loadImage(document, "/report-assets/STD.png");
    }

    protected <T> void renderSimpleReport(List<T> rows, String emptyMessage, PageFlow head,
                                          RowHeight<T> rowHeight, RowPainter<T> painter) throws IOException {
        startPage();
        renderHeader();
        head.apply();
        if (rows.isEmpty()) {
            drawEmptyState(emptyMessage);
        } else {
            for (T row : rows) {
                float height = rowHeight.heightOf(row);
                if (cursorY - height < FOOTER_TOP_LIMIT) {
                    finishPage();
                    startPage();
                    renderHeader();
                    head.apply();
                }
                painter.paint(row, height);
                cursorY -= height;
            }
        }
        finishPage();
    }

    protected void startPage() throws IOException {
        closeContent();
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);
        content = new PDPageContentStream(document, page);
        cursorY = PAGE_HEIGHT - 40f;
    }

    protected void finishPage() throws IOException {
        drawFooter();
        closeContent();
    }

    protected void renderHeader() throws IOException {
        if (headerLogo != null) {
            content.drawImage(headerLogo, HEADER_LOGO_X, HEADER_LOGO_Y, HEADER_LOGO_W, HEADER_LOGO_H);
        }
    }

    protected void drawEmptyState(String message) throws IOException {
        drawText(message, fonts.regular(), 10.4f, LEFT, cursorY - 6f, COLOR_MUTED);
        cursorY -= 30f;
    }

    protected float drawTableHeader(float topY, String[] headers, float[] widths, float[] sizes, float minHeight) throws IOException {
        float maxHeight = 0f;
        List<List<String>> wrappedHeaders = new ArrayList<>();

        for (int i = 0; i < headers.length; i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            List<String> wrapped = wrap(headers[i], fonts.bold(), sizes[i], colWidth - 8f);
            wrappedHeaders.add(wrapped);
            maxHeight = Math.max(maxHeight, Math.max(minHeight, wrapped.size() * 10.5f + 8f));
        }

        float x = LEFT;
        for (int i = 0; i < headers.length; i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            drawFilledRect(x, topY - maxHeight, colWidth, maxHeight, Color.WHITE);
            drawRect(x, topY - maxHeight, colWidth, maxHeight, COLOR_BORDER, 0.6f);
            drawCenteredWrapped(wrappedHeaders.get(i), fonts.bold(), sizes[i], x, topY - 10f, colWidth, 10.5f, COLOR_MUTED);
            x += colWidth;
        }
        return maxHeight;
    }

    protected void drawWrappedRow(String[] values, float[] widths, float[] cellSizes, float rowHeight) throws IOException {
        float x = LEFT;
        float y = cursorY;

        drawFilledRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, Color.WHITE);
        drawRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_BORDER, 0.6f);

        for (int i = 0; i < widths.length; i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            if (i > 0) {
                drawVerticalLine(x, y, y - rowHeight, COLOR_BORDER, 0.6f);
            }
            List<String> wrapped = wrap(values[i], fonts.regular(), cellSizes[i], colWidth - 8f);
            drawWrapped(wrapped, fonts.regular(), cellSizes[i], x + 4f, y - 11f, 10.8f, COLOR_TEXT);
            x += colWidth;
        }
    }

    protected float wrappedRowHeight(String[] values, float[] widths, float[] cellSizes) throws IOException {
        float maxLines = 1f;
        for (int i = 0; i < widths.length; i++) {
            List<String> wrapped = wrap(values[i], fonts.regular(), cellSizes[i], CONTENT_WIDTH * widths[i] - 8f);
            maxLines = Math.max(maxLines, wrapped.size());
        }
        return Math.max(22f, maxLines * 11f + 6f);
    }

    protected void drawPlainRow(String[] values, float[] widths, float[] cellSizes, float rowHeight) throws IOException {
        float x = LEFT;
        float y = cursorY;

        drawFilledRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, Color.WHITE);
        drawRect(x, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_BORDER, 0.6f);

        for (int i = 0; i < widths.length; i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            if (i > 0) {
                drawVerticalLine(x, y, y - rowHeight, COLOR_BORDER, 0.6f);
            }
            drawText(values[i], fonts.regular(), cellSizes[i], x + 4f, y - 11f, COLOR_TEXT);
            x += colWidth;
        }
    }

    protected void drawLabelValue(String label, String value, float y, boolean boldValue) throws IOException {
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
        PdfReportShared.drawFilledRect(content, x, y, width, height, fill, COLOR_TEXT);
    }

    private void drawRect(float x, float y, float width, float height, Color stroke, float lineWidth) throws IOException {
        PdfReportShared.drawRect(content, x, y, width, height, stroke, lineWidth, COLOR_TEXT);
    }

    private void drawVerticalLine(float x, float topY, float bottomY, Color stroke, float lineWidth) throws IOException {
        content.setStrokingColor(stroke);
        content.setLineWidth(lineWidth);
        content.moveTo(x, bottomY);
        content.lineTo(x, topY);
        content.stroke();
        content.setStrokingColor(COLOR_TEXT);
    }

    protected void drawText(String text, PDFont font, float size, float x, float y, Color color) throws IOException {
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

    @SuppressWarnings("java:S107")
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
        return PdfTextWrap.wrapLines(text, maxWidth, s -> stringWidth(font, size, s));
    }

    private float stringWidth(PDFont font, float size, String text) throws IOException {
        String encoded = encode(text);
        return font.getStringWidth(encoded) / 1000f * size;
    }

    String encode(String text) {
        if (fonts.unicode()) {
            return Objects.toString(text, "");
        }
        return ascii(text);
    }

    private static String ascii(String text) {
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

    static String formatDate(LocalDate date) {
        return date == null ? FECHA_PLACEHOLDER : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }

    static String yesNo(boolean value) {
        return value ? "SI" : "NO";
    }

    record FilaProyecto(String codigo, String nombre, String director, String dependencia, boolean aplica) {
    }

    static String[] headersCon(String ultimaColumna) {
        return new String[]{
                "Codigo del\nproyecto",
                "Nombre del\nproyecto",
                "Director del\nproyecto",
                "Dependencia",
                ultimaColumna
        };
    }

    static float[] headerSizesCon(float ultimoTamano) {
        return new float[]{8.8f, 8.8f, 8.6f, 8.8f, ultimoTamano};
    }

    static float[] cellSizesCinco() {
        return new float[]{9.2f, 9.2f, 9.2f, 9.2f, 10.2f};
    }

    static float[] widthsCinco() {
        return new float[]{0.20f, 0.24f, 0.20f, 0.21f, 0.15f};
    }

    protected void renderModoDetalle(DetailMode mode, String detalleDetallado) throws IOException {
        if (mode.isDetailed()) {
            drawLabelValue("NIVEL DE DETALLE:", detalleDetallado, 570f, true);
        } else {
            drawLabelValue("NIVEL DE DETALLE:", "[Resumido]", 570f, true);
        }
    }

    protected void renderResumenFilas(List<FilaProyecto> rows, String etiquetaA, String etiquetaB) throws IOException {
        int responde = contarSi(rows);
        int noResponde = rows.size() - responde;
        drawLabelValue(etiquetaA, String.valueOf(responde), 560f, false);
        drawLabelValue(etiquetaB, String.valueOf(noResponde), 538f, false);
        cursorY = 500f;
    }

    private static int contarSi(List<FilaProyecto> rows) {
        return (int) rows.stream().filter(FilaProyecto::aplica).count();
    }

    protected void renderEncabezadoTablaCinco(String ultimaColumna, float tamanoUltimaColumna, float altura) throws IOException {
        drawTableHeader(cursorY, headersCon(ultimaColumna), widthsCinco(), headerSizesCon(tamanoUltimaColumna), altura);
        cursorY -= TABLE_HEADER_HEIGHT;
    }

    protected void renderReporteCinco(List<FilaProyecto> rows, String mensajeVacio,
                                      PageFlow titulo, PageFlow meta, PageFlow modoDetalle,
                                      PageFlow resumen, PageFlow encabezado) throws IOException {
        PageFlow head = () -> {
            titulo.apply();
            meta.apply();
            modoDetalle.apply();
            resumen.apply();
            encabezado.apply();
        };
        renderSimpleReport(rows, mensajeVacio, head, this::alturaFilaProyecto, this::drawFilaProyecto);
    }

    protected void drawFilaProyecto(FilaProyecto row, float rowHeight) throws IOException {
        drawWrappedRow(filaProyectoValores(row), widthsCinco(), cellSizesCinco(), rowHeight);
    }

    protected float alturaFilaProyecto(FilaProyecto row) throws IOException {
        return wrappedRowHeight(filaProyectoValores(row), widthsCinco(), cellSizesCinco());
    }

    static String[] filaProyectoValores(FilaProyecto row) {
        return new String[]{
                row.codigo(),
                row.nombre(),
                row.director(),
                row.dependencia(),
                yesNo(row.aplica())
        };
    }

    static PDImageXObject loadImage(PDDocument document, String resourcePath) throws IOException {
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
