package com.proyecta.api_gestion.service.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Generador PDF institucional con layout por bloques, tablas y tarjetas.
 * <p>
 * Mantiene una firma simple ({@code build(title, lines)}) pero interpreta
 * tokens de renderizado para producir un PDF con estructura visual real:
 * cabecera, secciones, tablas, tarjetas y notas.
 */
public final class SimplePdfReportBuilder {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    private static final float LEFT = 40f;
    private static final float RIGHT = 40f;
    private static final float TOP = 40f;
    private static final float BOTTOM = 42f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float HEADER_START_Y = PAGE_HEIGHT - TOP;
    private static final float FOOTER_Y = 24f;

    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final Color COLOR_TEXT = new Color(28, 35, 43);
    private static final Color COLOR_MUTED = new Color(105, 116, 130);
    private static final Color COLOR_LINE = new Color(223, 229, 237);
    private static final Color COLOR_TITLE = new Color(20, 65, 93);
    private static final Color COLOR_SECTION = new Color(25, 85, 61);
    private static final Color COLOR_WATERMARK = new Color(188, 196, 205);
    private static final Color COLOR_FOOTER = new Color(122, 133, 146);
    private static final Pattern WIDTH_PART_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    private SimplePdfReportBuilder() {
    }

    public static byte[] build(String title, List<String> lines) {
        String safeTitle = sanitize(title);
        List<ReportElement> elements = parse(lines);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRenderer renderer = new PdfRenderer(document, safeTitle);
            renderer.startPage();
            renderer.render(elements);
            renderer.finish();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte.", ex);
        }
    }

    private static List<ReportElement> parse(List<String> lines) {
        List<ReportElement> elements = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            elements.add(new ParagraphElement("Sin informacion disponible."));
            return elements;
        }

        int i = 0;
        while (i < lines.size()) {
            String raw = lines.get(i);
            if (raw != null) {
                String line = raw.trim();
                if (line.isEmpty()) {
                    elements.add(new SpacerElement(8f));
                } else if (line.equals("PAGEBREAK")) {
                    elements.add(new PageBreakElement());
                } else if (line.equals("HR")) {
                    elements.add(new HorizontalRuleElement());
                } else if (line.startsWith("TABLE|")) {
                    TableBlock table = parseTable(line, lines, i);
                    elements.add(table);
                    i = table.endIndex();
                } else {
                    elements.add(parseSimple(line));
                }
            }
            i++;
        }

        return elements;
    }

    private static ReportElement parseSimple(String line) {
        String[] parts = line.split("\\|", -1);
        String token = parts[0].trim().toUpperCase(Locale.ROOT);

        return switch (token) {
            case "HDR" -> new HeaderLineElement(valueAt(parts, 1));
            case "SEC" -> new SectionElement(valueAt(parts, 1));
            case "SUB" -> new SubsectionElement(valueAt(parts, 1));
            case "TXT" -> new ParagraphElement(valueAt(parts, 1));
            case "BUL" -> new BulletElement(valueAt(parts, 1));
            case "KV" -> new KeyValueElement(valueAt(parts, 1), valueAt(parts, 2));
            case "CARD" -> new CardElement(valueAt(parts, 1), valueAt(parts, 2), valueAt(parts, 3));
            case "CHK" -> new CheckElement(valueAt(parts, 1), parseBoolean(valueAt(parts, 2)));
            default -> new ParagraphElement(line);
        };
    }

    private static TableBlock parseTable(String firstLine, List<String> lines, int startIndex) {
        String[] parts = firstLine.split("\\|", -1);
        String widthsSpec = valueAt(parts, 1);
        int headerStart = looksLikeWidths(widthsSpec) ? 2 : 1;

        List<String> headers = new ArrayList<>();
        for (int i = headerStart; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                headers.add(parts[i].trim());
            }
        }

        float[] widths = looksLikeWidths(widthsSpec) ? parseWidths(widthsSpec, headers.size()) : defaultWidths(headers.size());
        List<List<String>> rows = new ArrayList<>();
        int endIndex = startIndex;
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String next = lines.get(i);
            if (next != null) {
                String trimmed = next.trim();
                if (!trimmed.startsWith("ROW|")) {
                    break;
                }
                String[] rowParts = trimmed.split("\\|", -1);
                List<String> row = new ArrayList<>();
                for (int j = 1; j < rowParts.length; j++) {
                    row.add(sanitize(rowParts[j]));
                }
                rows.add(row);
                endIndex = i;
            }
        }

        return new TableBlock(headers, widths, rows, endIndex);
    }

    private static boolean looksLikeWidths(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length < 2) {
            return false;
        }
        for (String part : parts) {
            if (!WIDTH_PART_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

    private static float[] parseWidths(String spec, int columns) {
        String[] parts = spec.split(",");
        float[] widths = new float[columns];
        float total = 0f;
        for (int i = 0; i < columns; i++) {
            float width = i < parts.length ? parseFloat(parts[i], 1f) : 1f;
            widths[i] = width;
            total += width;
        }
        if (total <= 0f) {
            return defaultWidths(columns);
        }
        for (int i = 0; i < widths.length; i++) {
            widths[i] = widths[i] / total;
        }
        return widths;
    }

    private static float[] defaultWidths(int columns) {
        float[] widths = new float[columns];
        if (columns <= 0) {
            return widths;
        }
        float part = 1f / columns;
        Arrays.fill(widths, part);
        return widths;
    }

    private static String valueAt(String[] parts, int index) {
        if (index < 0 || index >= parts.length) {
            return "";
        }
        return sanitize(parts[index]);
    }

    private static boolean parseBoolean(String value) {
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("si") || value.equals("1"));
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception _) {
            return fallback;
        }
    }

    private static String sanitize(String value) {
        return PdfReportShared.sanitize(value, "ñ", "n", "Ñ", "N")
                .replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private sealed interface ReportElement permits HeaderLineElement, SectionElement, SubsectionElement,
            ParagraphElement, BulletElement, KeyValueElement, CardElement, CheckElement,
            TableBlock, SpacerElement, HorizontalRuleElement, PageBreakElement {
    }

    private record HeaderLineElement(String text) implements ReportElement {
    }

    private record SectionElement(String text) implements ReportElement {
    }

    private record SubsectionElement(String text) implements ReportElement {
    }

    private record ParagraphElement(String text) implements ReportElement {
    }

    private record BulletElement(String text) implements ReportElement {
    }

    private record KeyValueElement(String label, String value) implements ReportElement {
    }

    private record CardElement(String title, String value, String accent) implements ReportElement {
    }

    private record CheckElement(String label, boolean checked) implements ReportElement {
    }

    private record TableBlock(List<String> headers, float[] widths, List<List<String>> rows, int endIndex)
            implements ReportElement {

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof TableBlock(List<String> otherHeaders, float[] otherWidths,
                    List<List<String>> otherRows, int otherEndIndex))) return false;
            return Objects.equals(headers, otherHeaders)
                    && Arrays.equals(widths, otherWidths)
                    && Objects.equals(rows, otherRows)
                    && endIndex == otherEndIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(headers, Arrays.hashCode(widths), rows, endIndex);
        }

        @Override
        public String toString() {
            return "TableBlock[headers=" + headers + ", widths=" + Arrays.toString(widths)
                    + ", rows=" + rows + ", endIndex=" + endIndex + "]";
        }
    }

    private record SpacerElement(float height) implements ReportElement {
    }

    private record HorizontalRuleElement() implements ReportElement {
    }

    private record PageBreakElement() implements ReportElement {
    }

    private static final class PdfRenderer {
        private final PDDocument document;
        private final String title;
        private final String generatedOn = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        private final PDExtendedGraphicsState watermarkState = new PDExtendedGraphicsState();
        private final PDImageXObject headerLogo;
        private final PDImageXObject footerLogo;
        private PDPageContentStream content;
        private float cursorY;
        private int pageNumber = 0;
        private final List<KeyValueElement> kvBuffer = new ArrayList<>();
        private final List<CardElement> cardBuffer = new ArrayList<>();
        private final List<CheckElement> checkBuffer = new ArrayList<>();

        private PdfRenderer(PDDocument document, String title) {
            this.document = document;
            this.title = title;
            watermarkState.setNonStrokingAlphaConstant(0.045f);
            watermarkState.setStrokingAlphaConstant(0.045f);
            this.headerLogo = loadImage(document, "/report-assets/logo gob cun.png");
            this.footerLogo = loadImage(document, "/report-assets/STD.png");
        }

        void startPage() throws IOException {
            closeContent();
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            cursorY = HEADER_START_Y;
            drawWatermark();
            drawPageHeader();
        }

        void finish() throws IOException {
            flushBuffers();
            closeContent();
        }

        void render(List<ReportElement> elements) throws IOException {
            for (ReportElement element : elements) {
                if (element instanceof PageBreakElement) {
                    flushBuffers();
                    startPage();
                } else if (element instanceof HeaderLineElement(String text)) {
                    flushBuffers();
                    renderHeaderLine(text);
                } else if (element instanceof SectionElement(String text)) {
                    flushBuffers();
                    renderSection(text);
                } else if (element instanceof SubsectionElement(String text)) {
                    flushBuffers();
                    renderSubsection(text);
                } else if (element instanceof ParagraphElement(String text)) {
                    flushBuffers();
                    renderParagraph(text, FONT_REGULAR, 11f, COLOR_TEXT, 1.35f, 0f);
                } else if (element instanceof BulletElement(String text)) {
                    flushBuffers();
                    renderBullet(text);
                } else if (element instanceof KeyValueElement keyValue) {
                    cardBufferIfNeeded();
                    checkBufferIfNeeded();
                    kvBuffer.add(keyValue);
                } else if (element instanceof CardElement card) {
                    kvBufferIfNeeded();
                    checkBufferIfNeeded();
                    cardBuffer.add(card);
                } else if (element instanceof CheckElement check) {
                    kvBufferIfNeeded();
                    cardBufferIfNeeded();
                    checkBuffer.add(check);
                } else if (element instanceof TableBlock table) {
                    flushBuffers();
                    renderTable(table);
                } else if (element instanceof SpacerElement(float height)) {
                    flushBuffers();
                    ensureSpace(height);
                    cursorY -= height;
                } else if (element instanceof HorizontalRuleElement) {
                    flushBuffers();
                    drawHorizontalRule();
                }
            }
            flushBuffers();
        }

        private void renderHeaderLine(String text) throws IOException {
            ensureSpace(16f);
            drawText(text, FONT_REGULAR, 9f, COLOR_MUTED, LEFT, cursorY);
            cursorY -= 13f;
        }

        private void renderSection(String text) throws IOException {
            ensureSpace(24f);
            drawText(text, FONT_BOLD, 13.5f, COLOR_SECTION, LEFT, cursorY);
            cursorY -= 4f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 16f;
        }

        private void renderSubsection(String text) throws IOException {
            ensureSpace(18f);
            drawText(text, FONT_BOLD, 11.5f, COLOR_TEXT, LEFT, cursorY);
            cursorY -= 14f;
        }

        private void renderBullet(String text) throws IOException {
            float bulletWidth = 8f;
            List<String> wrapped = wrapText(text, FONT_REGULAR, 11f, CONTENT_WIDTH - 22f);
            float height = wrapped.size() * 13f;
            ensureSpace(height + 4f);
            drawText("•", FONT_BOLD, 11f, COLOR_TEXT, LEFT, cursorY);
            drawWrapped(wrapped, FONT_REGULAR, 11f, COLOR_TEXT, LEFT + bulletWidth, cursorY, 13f);
            cursorY -= (height + 4f);
        }

        private void renderParagraph(String text, PDFont font, float fontSize, Color color, float leading, float extraBottom)
                throws IOException {
            List<String> wrapped = wrapText(text, font, fontSize, CONTENT_WIDTH);
            float height = wrapped.size() * (fontSize * leading);
            ensureSpace(height + extraBottom + 2f);
            drawWrapped(wrapped, font, fontSize, color, LEFT, cursorY, fontSize * leading);
            cursorY -= height + extraBottom;
        }

        private void renderKvBuffer() throws IOException {
            if (kvBuffer.isEmpty()) {
                return;
            }
            List<KeyValueElement> items = new ArrayList<>(kvBuffer);
            kvBuffer.clear();

            float cardWidth = (CONTENT_WIDTH - 18f) / 2f;
            for (int i = 0; i < items.size(); i += 2) {
                KeyValueElement left = items.get(i);
                KeyValueElement right = i + 1 < items.size() ? items.get(i + 1) : null;
                float leftHeight = computeKeyValueCardHeight(left, cardWidth);
                float rightHeight = right != null ? computeKeyValueCardHeight(right, cardWidth) : leftHeight;
                float rowHeight = Math.max(Math.max(leftHeight, rightHeight), 38f);
                ensureSpace(rowHeight + 8f);
                drawKeyValueCard(LEFT, cursorY, cardWidth, rowHeight, left);
                if (right != null) {
                    drawKeyValueCard(LEFT + cardWidth + 18f, cursorY, cardWidth, rowHeight, right);
                }
                cursorY -= rowHeight + 10f;
            }
        }

        private float computeKeyValueCardHeight(KeyValueElement item, float width) throws IOException {
            List<String> wrapped = wrapText(item.value(), FONT_BOLD, 10.5f, width - 16f);
            float valueHeight = wrapped.size() * 11f;
            return Math.max(38f, 14f + valueHeight + 12f);
        }

        private void renderCardBuffer() throws IOException {
            if (cardBuffer.isEmpty()) {
                return;
            }
            List<CardElement> items = new ArrayList<>(cardBuffer);
            cardBuffer.clear();

            float gap = 12f;
            int columns = Math.min(4, items.size());
            float cardWidth = (CONTENT_WIDTH - (gap * (columns - 1))) / columns;
            for (int i = 0; i < items.size(); i += columns) {
                int end = Math.min(items.size(), i + columns);
                float rowHeight = 52f;
                ensureSpace(rowHeight + 8f);
                float x = LEFT;
                for (int j = i; j < end; j++) {
                    drawMetricCard(x, cursorY, cardWidth, rowHeight, items.get(j));
                    x += cardWidth + gap;
                }
                cursorY -= rowHeight + 10f;
            }
        }

        private void renderCheckBuffer() throws IOException {
            if (checkBuffer.isEmpty()) {
                return;
            }
            List<CheckElement> items = new ArrayList<>(checkBuffer);
            checkBuffer.clear();
            int columns = Math.min(4, items.size());
            float gap = 12f;
            float itemWidth = (CONTENT_WIDTH - (gap * (columns - 1))) / columns;
            float rowHeight = 24f;

            for (int i = 0; i < items.size(); i += columns) {
                int end = Math.min(items.size(), i + columns);
                ensureSpace(rowHeight + 8f);
                float x = LEFT;
                for (int j = i; j < end; j++) {
                    CheckElement item = items.get(j);
                    drawCheckboxItem(x, cursorY, itemWidth, rowHeight, item);
                    x += itemWidth + gap;
                }
                cursorY -= rowHeight + 8f;
            }
        }

        private void renderTable(TableBlock table) throws IOException {
            float[] widths = table.widths();
            List<String> headers = table.headers();
            List<List<String>> rows = table.rows();
            float headerHeight = 24f;
            drawTableHeader(headers, widths, headerHeight);
            for (int index = 0; index < rows.size(); index++) {
                List<String> row = rows.get(index);
                float rowHeight = computeRowHeight(row, widths, 9f, 2f, 14f);
                if (cursorY - rowHeight < BOTTOM) {
                    startPage();
                    drawTableHeader(headers, widths, headerHeight);
                }
                drawTableRow(row, widths, rowHeight, 9f, index);
            }
            cursorY -= 8f;
        }

        private void flushBuffers() throws IOException {
            renderKvBuffer();
            renderCardBuffer();
            renderCheckBuffer();
        }

        private void cardBufferIfNeeded() throws IOException {
            if (!cardBuffer.isEmpty()) {
                renderCardBuffer();
            }
        }

        private void kvBufferIfNeeded() throws IOException {
            if (!kvBuffer.isEmpty()) {
                renderKvBuffer();
            }
        }

        private void checkBufferIfNeeded() throws IOException {
            if (!checkBuffer.isEmpty()) {
                renderCheckBuffer();
            }
        }

        private void ensureSpace(float required) throws IOException {
            if (cursorY - required < BOTTOM) {
                startPage();
            }
        }

        private void drawPageHeader() throws IOException {
            if (pageNumber == 1) {
                drawFirstPageHeader();
                return;
            }
            drawContinuationHeader();
        }

        private void drawFirstPageHeader() throws IOException {
            PdfReportShared.HeaderLayout header = PdfReportShared.drawFirstPageHeaderBase(content, cursorY, headerLogo, generatedOn, FONT_REGULAR, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);
            List<String> wrappedTitle = wrapText(title, FONT_BOLD, 15.2f, header.titleZoneWidth());
            drawWrappedCentered(wrappedTitle, FONT_BOLD, 15.2f, COLOR_TITLE, header.titleZoneX(), header.titleZoneY(), header.titleZoneWidth(), 17f);

            cursorY -= header.bandHeight() + 14f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 18f;
        }

        private void drawContinuationHeader() throws IOException {
            drawText("PROYECTA | Gobernacion de Cundinamarca", FONT_BOLD, 8.4f, COLOR_SECTION, LEFT, cursorY);
            cursorY -= 11f;
            drawText(title, FONT_BOLD, 12.8f, COLOR_TITLE, LEFT, cursorY);
            cursorY -= 18f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 14f;
        }

        private void drawWatermark() throws IOException {
            content.saveGraphicsState();
            content.setGraphicsStateParameters(watermarkState);
            content.setNonStrokingColor(COLOR_WATERMARK);
            content.beginText();
            content.setFont(FONT_BOLD, 46f);
            content.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(
                    Math.toRadians(33d), PAGE_WIDTH * 0.34f, PAGE_HEIGHT * 0.44f));
            content.showText("PROYECTA");
            content.endText();

            content.beginText();
            content.setFont(FONT_BOLD, 16f);
            content.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(
                    Math.toRadians(33d), PAGE_WIDTH * 0.43f, PAGE_HEIGHT * 0.34f));
            content.showText("GOBERNACION DE CUNDINAMARCA");
            content.endText();
            content.restoreGraphicsState();
            content.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawHorizontalRule() throws IOException {
            ensureSpace(10f);
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 10f;
        }

        private void drawTableHeader(List<String> headers, float[] widths, float rowHeight) throws IOException {
            ensureSpace(rowHeight + 6f);
            PdfReportShared.drawTableHeader(content, cursorY, headers, widths, rowHeight, 8.8f, true, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);
            cursorY -= rowHeight;
        }

        private void drawTableRow(List<String> row, float[] widths, float rowHeight, float fontSize, int rowIndex) throws IOException {
            ensureSpace(rowHeight + 4f);
            PdfReportShared.drawTableRow(content, cursorY, row, widths, rowHeight, fontSize, rowIndex, FONT_REGULAR, COLOR_TEXT, this::drawText, this::wrapText);
            cursorY -= rowHeight;
        }

        private void drawKeyValueCard(float x, float y, float width, float height, KeyValueElement item) throws IOException {
            drawRoundedCard(x, y - height, width, height, Color.WHITE, COLOR_LINE);
            drawText(item.label(), FONT_BOLD, 9.5f, COLOR_MUTED, x + 8f, y - 14f);
            List<String> wrapped = wrapText(item.value(), FONT_BOLD, 10.5f, width - 16f);
            float valueStartY = y - 28f;
            if (wrapped.size() > 1) {
                valueStartY = y - 30f;
            }
            drawWrapped(wrapped, FONT_BOLD, 10.5f, COLOR_TEXT, x + 8f, valueStartY, 11f);
        }

        private void drawMetricCard(float x, float y, float width, float height, CardElement item) throws IOException {
            Color fill = accentColor(item.accent());
            drawRoundedCard(x, y - height, width, height, fill, COLOR_LINE);
            drawText(item.title(), FONT_BOLD, 9.2f, COLOR_MUTED, x + 8f, y - 14f);
            drawText(item.value(), FONT_BOLD, 15.5f, COLOR_TEXT, x + 8f, y - 35f);
        }

        private void drawCheckboxItem(float x, float y, float width, float height, CheckElement item) throws IOException {
            drawRoundedCard(x, y - height, width, height, Color.WHITE, COLOR_LINE);
            float boxX = x + 8f;
            float boxY = y - 16f;
            drawRect(boxX, boxY, 10f, 10f, COLOR_MUTED, 0.7f);
            if (item.checked()) {
                drawText("X", FONT_BOLD, 8.5f, COLOR_SECTION, boxX + 1.6f, boxY - 0.2f);
            }
            drawText(item.label(), FONT_REGULAR, 9.2f, COLOR_TEXT, x + 24f, y - 14f);
        }

        private void drawRoundedCard(float x, float y, float width, float height, Color fill, Color stroke) throws IOException {
            drawFilledRect(x, y, width, height, fill);
            drawRect(x, y, width, height, stroke, 0.8f);
        }

        private void drawFilledRect(float x, float y, float width, float height, Color fill) throws IOException {
            PdfReportShared.drawFilledRect(content, x, y, width, height, fill, COLOR_TEXT);
        }

        private void drawRect(float x, float y, float width, float height, Color stroke, float lineWidth) throws IOException {
            PdfReportShared.drawRect(content, x, y, width, height, stroke, lineWidth, COLOR_TEXT);
        }

        private void drawLine(float x1, float y1, float x2, float y2, Color stroke, float lineWidth) throws IOException {
            content.setStrokingColor(stroke);
            content.setLineWidth(lineWidth);
            content.moveTo(x1, y1);
            content.lineTo(x2, y2);
            content.stroke();
            content.setStrokingColor(COLOR_TEXT);
        }

        private void drawText(String text, PDFont font, float size, Color color, float x, float y) throws IOException {
            content.beginText();
            content.setNonStrokingColor(color);
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(sanitize(text));
            content.endText();
            content.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawWrapped(List<String> lines, PDFont font, float size, Color color, float x, float y, float leading)
                throws IOException {
            PdfReportShared.drawWrapped(lines, font, size, color, x, y, leading, this::drawText);
        }

        @SuppressWarnings("java:S107")
        private void drawWrappedCentered(List<String> lines, PDFont font, float size, Color color, float x, float y, float width, float leading)
                throws IOException {
            PdfReportShared.drawWrappedCentered(lines, font, size, color, x, y, width, leading, this::drawText, this::stringWidth);
        }

        private float computeRowHeight(List<String> row, float[] widths, float fontSize, float padding, float minHeight) throws IOException {
            float maxHeight = minHeight;
            for (int i = 0; i < widths.length; i++) {
                String value = i < row.size() ? row.get(i) : "";
                float available = CONTENT_WIDTH * widths[i] - (padding * 2f);
                List<String> wrapped = wrapText(value, FONT_REGULAR, fontSize, available);
                float height = (wrapped.size() * (fontSize + 2f)) + (padding * 1.5f);
                maxHeight = Math.max(maxHeight, height);
            }
            return maxHeight;
        }

        private static final Pattern WHITESPACE = Pattern.compile("\\s+");
        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String normalized = text == null ? "" : text.trim();
            if (normalized.isEmpty()) {
                return List.of("");
            }

            List<String> result = new ArrayList<>();
            String[] paragraphs = normalized.split("\\r?\\n");
            for (int p = 0; p < paragraphs.length; p++) {
                String paragraph = paragraphs[p];
                StringBuilder current = new StringBuilder();
                for (String word : WHITESPACE.split(paragraph)) {
                    if (word.isBlank()) {
                        continue;
                    }
                    PdfTextWrap.appendWordTrial(word, current, result, maxWidth, s -> stringWidth(font, fontSize, s));
                }
                if (!current.isEmpty()) {
                    result.add(current.toString());
                }
                if (p < paragraphs.length - 1) {
                    result.add("");
                }
            }
            return result.isEmpty() ? List.of("") : result;
        }

        private float stringWidth(PDFont font, float size, String value) throws IOException {
            return PdfReportShared.stringWidth(font, size, value, SimplePdfReportBuilder::sanitize);
        }

        private Color accentColor(String accent) {
            return PdfReportShared.accentColor(accent, Color.WHITE);
        }

        private void closeContent() throws IOException {
            if (content != null) {
                drawFooter();
                content.close();
                content = null;
            }
        }

        private void drawFooter() throws IOException {
            if (content == null) {
                return;
            }
            content.saveGraphicsState();
            PdfReportShared.drawFooterBlock(content, footerLogo, FONT_REGULAR, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);

            drawText("Reporte institucional | Pagina " + pageNumber, FONT_OBLIQUE, 8.2f, COLOR_FOOTER, LEFT, FOOTER_Y);
            content.restoreGraphicsState();
        }

        private PDImageXObject loadImage(PDDocument document, String resourcePath) {
            try (InputStream stream = SimplePdfReportBuilder.class.getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    return null;
                }
                BufferedImage image = javax.imageio.ImageIO.read(stream);
                if (image == null) {
                    return null;
                }
                return LosslessFactory.createFromImage(document, image);
            } catch (Exception _) {
                return null;
            }
        }
    }
}
