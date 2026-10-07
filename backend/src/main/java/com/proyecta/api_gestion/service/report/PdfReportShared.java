package com.proyecta.api_gestion.service.report;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

final class PdfReportShared {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float LEFT = 40f;
    private static final float RIGHT = 40f;
    private static final float BOTTOM = 42f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;

    private static final Color COLOR_LINE = new Color(223, 229, 237);
    private static final Color COLOR_TITLE = new Color(20, 65, 93);
    private static final Color COLOR_SECTION = new Color(25, 85, 61);
    private static final Color COLOR_TABLE_HEAD = new Color(238, 242, 247);
    private static final Color COLOR_TABLE_BORDER = new Color(201, 210, 220);
    private static final Color COLOR_ROW_ALT = new Color(248, 250, 252);
    private static final Color COLOR_BANNER = new Color(250, 252, 255);
    private static final Color COLOR_CARD_GREEN = new Color(235, 249, 239);
    private static final Color COLOR_CARD_YELLOW = new Color(255, 248, 225);
    private static final Color COLOR_CARD_RED = new Color(255, 236, 236);
    private static final Color COLOR_CARD_BLUE = new Color(232, 243, 255);

    private PdfReportShared() {
    }

    @FunctionalInterface
    interface TextDrawer {
        void draw(String text, PDFont font, float size, Color color, float x, float y) throws IOException;
    }

    @FunctionalInterface
    interface WidthMeasurer {
        float measure(PDFont font, float size, String value) throws IOException;
    }

    @FunctionalInterface
    interface TextWrapper {
        List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException;
    }

    record HeaderLayout(float bandHeight, float titleZoneX, float titleZoneY, float titleZoneWidth) {
    }

    static String sanitize(String value, String from, String to, String from2, String to2) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace(from, to)
                .replace(from2, to2);
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

    static float stringWidth(PDFont font, float size, String value, UnaryOperator<String> encoder) throws IOException {
        return font.getStringWidth(encoder.apply(value)) / 1000f * size;
    }

    static Color accentColor(String accent, Color fallback) {
        if (accent == null) {
            return Color.WHITE;
        }
        return switch (accent.trim().toLowerCase(Locale.ROOT)) {
            case "green", "success" -> COLOR_CARD_GREEN;
            case "yellow", "warn", "warning" -> COLOR_CARD_YELLOW;
            case "red", "danger" -> COLOR_CARD_RED;
            case "blue", "info" -> COLOR_CARD_BLUE;
            default -> fallback;
        };
    }

    static void drawFilledRect(PDPageContentStream cs, float x, float y, float width, float height, Color fill, Color resetColor) throws IOException {
        cs.setNonStrokingColor(fill);
        cs.addRect(x, y, width, height);
        cs.fill();
        cs.setNonStrokingColor(resetColor);
    }

    @SuppressWarnings("java:S107")
    static void drawRect(PDPageContentStream cs, float x, float y, float width, float height, Color stroke, float lineWidth, Color resetColor) throws IOException {
        cs.setStrokingColor(stroke);
        cs.setLineWidth(lineWidth);
        cs.addRect(x, y, width, height);
        cs.stroke();
        cs.setStrokingColor(resetColor);
    }

    private static void drawVerticalLine(PDPageContentStream cs, float x, float yTop, float yBottom, Color stroke, float lineWidth, Color resetColor) throws IOException {
        cs.setStrokingColor(stroke);
        cs.setLineWidth(lineWidth);
        cs.moveTo(x, yTop);
        cs.lineTo(x, yBottom);
        cs.stroke();
        cs.setStrokingColor(resetColor);
    }

    @SuppressWarnings("java:S107")
    private static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color stroke, float lineWidth, Color resetColor) throws IOException {
        cs.setStrokingColor(stroke);
        cs.setLineWidth(lineWidth);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        cs.setStrokingColor(resetColor);
    }

    private static void drawImage(PDPageContentStream cs, PDImageXObject image, float x, float yTop, float width, float height) throws IOException {
        if (image == null) {
            return;
        }
        cs.drawImage(image, x, yTop - height, width, height);
    }

    @SuppressWarnings("java:S107")
    static void drawWrapped(List<String> lines, PDFont font, float size, Color color, float x, float y, float leading, TextDrawer drawer) throws IOException {
        float currentY = y;
        for (String line : lines) {
            drawer.draw(line, font, size, color, x, currentY);
            currentY -= leading;
        }
    }

    @SuppressWarnings("java:S107")
    static void drawWrappedCentered(List<String> lines, PDFont font, float size, Color color, float x, float y, float width, float leading, TextDrawer drawer, WidthMeasurer measurer) throws IOException {
        float currentY = y;
        for (String line : lines) {
            float lineWidth = measurer.measure(font, size, line);
            float centeredX = x + Math.max(0f, (width - lineWidth) / 2f);
            drawer.draw(line, font, size, color, centeredX, currentY);
            currentY -= leading;
        }
    }

    @SuppressWarnings("java:S107")
    static void drawTableHeader(PDPageContentStream cs, float cursorY, List<String> headers, float[] widths, float rowHeight,
                                float headerFontSize, boolean allColumnBorders, PDFont fontBold, Color colorText,
                                Color colorMuted, TextDrawer drawer) throws IOException {
        float x = LEFT;
        float y = cursorY;
        drawFilledRect(cs, LEFT, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_TABLE_HEAD, colorText);
        drawRect(cs, LEFT, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_TABLE_BORDER, 0.7f, colorText);
        for (int i = 0; i < headers.size(); i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            if (allColumnBorders) {
                drawVerticalLine(cs, x + colWidth, y, y - rowHeight, COLOR_TABLE_BORDER, 0.5f, colorText);
            } else if (i > 0) {
                drawVerticalLine(cs, x, y, y - rowHeight, COLOR_TABLE_BORDER, 0.5f, colorText);
            }
            drawer.draw(headers.get(i), fontBold, headerFontSize, colorMuted, x + 4f, y - 15f);
            x += colWidth;
        }
    }

    @SuppressWarnings("java:S107")
    static void drawTableRow(PDPageContentStream cs, float cursorY, List<String> row, float[] widths, float rowHeight,
                             float fontSize, int rowIndex, PDFont fontRegular, Color colorText, TextDrawer drawer,
                             TextWrapper wrapper) throws IOException {
        float x = LEFT;
        float y = cursorY;
        Color rowFill = rowIndex % 2 == 0 ? Color.WHITE : COLOR_ROW_ALT;
        drawFilledRect(cs, LEFT, y - rowHeight, CONTENT_WIDTH, rowHeight, rowFill, colorText);
        drawRect(cs, LEFT, y - rowHeight, CONTENT_WIDTH, rowHeight, COLOR_TABLE_BORDER, 0.6f, colorText);
        for (int i = 0; i < widths.length; i++) {
            float colWidth = CONTENT_WIDTH * widths[i];
            if (i > 0) {
                drawVerticalLine(cs, x, y, y - rowHeight, COLOR_TABLE_BORDER, 0.5f, colorText);
            }
            String value = i < row.size() ? row.get(i) : "";
            List<String> wrapped = wrapper.wrap(value, fontRegular, fontSize, colWidth - 8f);
            drawWrapped(wrapped, fontRegular, fontSize, colorText, x + 4f, y - 11f, fontSize + 2f, drawer);
            x += colWidth;
        }
    }

    @SuppressWarnings("java:S107")
    static HeaderLayout drawFirstPageHeaderBase(PDPageContentStream cs, float cursorY, PDImageXObject headerLogo,
                                                String generatedOn, PDFont fontRegular, PDFont fontBold,
                                                Color colorText, Color colorMuted, TextDrawer drawer) throws IOException {
        float bandHeight = 186f;
        drawFilledRect(cs, LEFT, cursorY - bandHeight, CONTENT_WIDTH, bandHeight, COLOR_BANNER, colorText);
        drawRect(cs, LEFT, cursorY - bandHeight, CONTENT_WIDTH, bandHeight, COLOR_LINE, 0.8f, colorText);

        float leftBlockX = LEFT + 12f;
        float logoWidth = 118f;
        float logoHeight = 34f;
        float stampWidth = 146f;
        float stampX = PAGE_WIDTH - RIGHT - stampWidth;

        if (headerLogo != null) {
            drawImage(cs, headerLogo, leftBlockX, cursorY - 16f, logoWidth, logoHeight);
            drawer.draw("Sistema integral de seguimiento institucional", fontRegular, 8.2f, colorMuted, leftBlockX + 4f, cursorY - 56f);
        } else {
            drawer.draw("GOBERNACION DE CUNDINAMARCA", fontBold, 8.8f, COLOR_SECTION, leftBlockX, cursorY - 18f);
            drawer.draw("Sistema integral de seguimiento institucional", fontRegular, 8.2f, colorMuted, leftBlockX + 4f, cursorY - 34f);
        }

        float boxY = cursorY - 92f;
        drawFilledRect(cs, stampX, boxY, stampWidth, 50f, Color.WHITE, colorText);
        drawRect(cs, stampX, boxY, stampWidth, 50f, COLOR_LINE, 0.8f, colorText);
        drawer.draw("Fecha del reporte", fontBold, 8.4f, colorMuted, stampX + 10f, boxY + 34f);
        drawer.draw(generatedOn, fontBold, 10.7f, colorText, stampX + 10f, boxY + 19f);
        drawer.draw("Documento oficial", fontBold, 8.2f, colorMuted, stampX + 10f, boxY + 6f);

        float titleZoneY = cursorY - 74f;
        float titleZoneX = LEFT + 18f;
        float titleZoneWidth = CONTENT_WIDTH - 36f;
        drawLine(cs, LEFT + 18f, titleZoneY + 14f, PAGE_WIDTH - RIGHT - 18f, titleZoneY + 14f, COLOR_LINE, 0.5f, colorText);
        return new HeaderLayout(bandHeight, titleZoneX, titleZoneY, titleZoneWidth);
    }

    static void drawFooterBlock(PDPageContentStream cs, PDImageXObject footerLogo, PDFont fontRegular, PDFont fontBold,
                                Color colorText, Color colorMuted, TextDrawer drawer) throws IOException {
        float lineY = BOTTOM + 20f;
        drawLine(cs, LEFT, lineY, PAGE_WIDTH - RIGHT, lineY, COLOR_LINE, 0.7f, colorText);

        float blockY = BOTTOM + 22f;
        if (footerLogo != null) {
            float logoWidth = 108f;
            float logoHeight = 16f;
            float logoX = PAGE_WIDTH - RIGHT - logoWidth;
            drawImage(cs, footerLogo, logoX, blockY + logoHeight, logoWidth, logoHeight);
        } else {
            float logoX = PAGE_WIDTH - RIGHT - 120f;
            drawer.draw("Transformacion Digital", fontBold, 13f, COLOR_TITLE, logoX, blockY + 16f);
        }

        float textX = LEFT + 8f;
        drawer.draw("Calle 26 #51-53 Bogota D.C.", fontRegular, 8.8f, colorText, textX, blockY + 18f);
        drawer.draw("Sede Administrativa - Torre Central Piso 7.", fontRegular, 8.8f, colorText, textX, blockY + 6f);
        drawer.draw("Codigo Postal: 111321 - Telefono: 7491513", fontRegular, 8.8f, colorText, textX, blockY - 6f);
        drawer.draw("www.cundinamarca.gov.co", fontRegular, 8.2f, colorMuted, textX, blockY - 18f);
    }
}
