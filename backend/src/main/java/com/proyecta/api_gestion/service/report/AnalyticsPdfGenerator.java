package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.dto.analytics.AnalyticsPortfolioDTO;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class AnalyticsPdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    private static final float LEFT = 40f;
    private static final float RIGHT = 40f;
    private static final float TOP = 40f;
    private static final float BOTTOM = 42f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float HEADER_START_Y = PAGE_HEIGHT - TOP;
    private static final float FOOTER_TOP_LIMIT = 92f;

    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    private static final String CHART_FONT_NAME = "SansSerif";
    private static final String LABEL_AVANCE = "Avance";
    private static final String LABEL_EFICACIA = "Eficacia";
    private static final String LABEL_EFICIENCIA = "Eficiencia";
    private static final String LABEL_CODIGO = "Codigo";
    private static final Font CHART_FONT_REGULAR = new Font(CHART_FONT_NAME, Font.PLAIN, 7);
    private static final Font CHART_FONT_BOLD = new Font(CHART_FONT_NAME, Font.BOLD, 8);
    private static final Font CHART_FONT_PIE = new Font(CHART_FONT_NAME, Font.BOLD, 9);

    private static final Color COLOR_TEXT = new Color(18, 18, 18);
    private static final Color COLOR_MUTED = new Color(58, 58, 58);
    private static final Color COLOR_TITLE = new Color(20, 65, 93);
    private static final Color COLOR_SECTION = new Color(25, 85, 61);
    private static final Color COLOR_CARD_BLUE = new Color(232, 243, 255);
    private static final Color COLOR_CARD_YELLOW = new Color(255, 248, 225);
    private static final Color COLOR_WATERMARK = new Color(188, 196, 205);
    private static final Color COLOR_FOOTER = new Color(122, 133, 146);
    private static final Color COLOR_LINE = new Color(223, 229, 237);

    private AnalyticsPdfGenerator() {
    }

    public static byte[] generate(AnalyticsPortfolioDTO data) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfRenderer renderer = new PdfRenderer(document, data);
            renderer.renderAll();
            renderer.finish();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de analitica.", ex);
        }
    }

    private static final class PdfRenderer {
        private final PDDocument document;
        private final AnalyticsPortfolioDTO data;
        private final String generatedOn = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        private final PDExtendedGraphicsState watermarkState = new PDExtendedGraphicsState();
        private final PDImageXObject headerLogo;
        private final PDImageXObject footerLogo;
        private PDPageContentStream cs;
        private float cursorY;
        private int pageNumber = 0;

        PdfRenderer(PDDocument document, AnalyticsPortfolioDTO data) {
            this.document = document;
            this.data = data;
            watermarkState.setNonStrokingAlphaConstant(0.045f);
            watermarkState.setStrokingAlphaConstant(0.045f);
            this.headerLogo = loadImage(document, "/report-assets/logo gob cun.png");
            this.footerLogo = loadImage(document, "/report-assets/STD.png");
        }

        void renderAll() throws IOException {
            startPage();
            drawExecutiveKPIs();
            drawAvancePorDependenciaChart();
            drawPiePetiChart();
            drawDependenciasSection();
            drawEficaciaEficienciaChart();
            drawEstrategiasSection();
            drawFuragSection();
            drawRiesgosSection();
            drawProyectosTable();
        }

        void finish() throws IOException {
            closeContent();
        }

        private void startPage() throws IOException {
            closeContent();
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            pageNumber++;
            cursorY = HEADER_START_Y;
            drawWatermark();
            drawPageHeader();
        }

        private void ensureSpace(float required) throws IOException {
            if (cursorY - required < FOOTER_TOP_LIMIT) {
                startPage();
            }
        }

        private void drawPageHeader() throws IOException {
            if (pageNumber == 1) {
                drawFirstPageHeader();
            } else {
                drawContinuationHeader();
            }
        }

        private void drawFirstPageHeader() throws IOException {
            PdfReportShared.HeaderLayout header = PdfReportShared.drawFirstPageHeaderBase(cs, cursorY, headerLogo, generatedOn, FONT_REGULAR, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);

            String titleText = "ANALITICA DEL PORTAFOLIO";
            List<String> wrappedTitle = wrapText(titleText, FONT_BOLD, 15.2f, header.titleZoneWidth());
            drawWrappedCentered(wrappedTitle, FONT_BOLD, 15.2f, COLOR_TITLE, header.titleZoneX(), header.titleZoneY(), header.titleZoneWidth(), 17f);

            if (data.corte() != null) {
                drawText("Corte: " + data.corte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        FONT_REGULAR, 9f, COLOR_MUTED, header.titleZoneX(), header.titleZoneY() - 42f);
            }

            cursorY -= header.bandHeight() + 14f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 18f;
        }

        private void drawContinuationHeader() throws IOException {
            drawText("PROYECTA | Gobernacion de Cundinamarca", FONT_BOLD, 8.4f, COLOR_SECTION, LEFT, cursorY);
            cursorY -= 11f;
            drawText("ANALITICA DEL PORTAFOLIO", FONT_BOLD, 12.8f, COLOR_TITLE, LEFT, cursorY);
            cursorY -= 18f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 14f;
        }

        private void drawExecutiveKPIs() throws IOException {
            drawSectionTitle("INDICADORES EJECUTIVOS");
            AnalyticsPortfolioDTO.ExecutiveMetrics exec = data.indicadores();
            if (exec == null) return;

            float cardWidth = (CONTENT_WIDTH - 36f) / 4f;
            float cardHeight = 52f;
            float gap = 12f;

            String[][] kpis = {
                    {"PROYECTOS", String.valueOf(exec.totalProyectos())},
                    {"AVANCE PROMEDIO", fmtPct(exec.avancePromedio())},
                    {"EFICACIA PROMEDIO", fmtPct(exec.eficaciaPromedio())},
                    {"EFICIENCIA PROMEDIO", fmtPct(exec.eficienciaPromedio())},
            };
            drawCardRow(kpis, cardWidth, cardHeight, gap, "blue");

            String[][] kpis2 = {
                    {"ENTREGABLES ATRASADOS", String.valueOf(exec.entregablesAtrasados())},
                    {"PROXIMOS A VENCER", String.valueOf(exec.proximosAVencer())},
                    {"ACTIVOS", String.valueOf(exec.activos())},
                    {"CERRADOS", String.valueOf(exec.cerrados())},
            };
            drawCardRow(kpis2, cardWidth, cardHeight, gap, "green");

            cursorY -= 10f;
        }

        private void drawCardRow(String[][] kpis, float cardWidth, float cardHeight, float gap, String accent) throws IOException {
            float x = LEFT;
            for (String[] kpi : kpis) {
                ensureSpace(cardHeight + 10f);
                Color fill = accentColor(accent);
                drawRoundedCard(x, cursorY - cardHeight, cardWidth, cardHeight, fill, COLOR_LINE);
                drawText(kpi[0], FONT_BOLD, 8f, COLOR_MUTED, x + 8f, cursorY - 14f);
                drawText(kpi[1], FONT_BOLD, 15f, COLOR_TEXT, x + 8f, cursorY - 35f);
                x += cardWidth + gap;
            }
            cursorY -= cardHeight + 10f;
        }

        private void drawAvancePorDependenciaChart() throws IOException {
            List<AnalyticsPortfolioDTO.DependenciaMetrics> deps = data.dependencias();
            if (deps == null || deps.isEmpty()) return;

            drawSectionTitle("AVANCE PROMEDIO POR DEPENDENCIA");

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (AnalyticsPortfolioDTO.DependenciaMetrics d : deps) {
                double avance = d.avancePromedio() != null ? d.avancePromedio().doubleValue() : 0;
                dataset.addValue(avance, LABEL_AVANCE, nz(d.dependencia()));
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    null, null, "Avance %", dataset, PlotOrientation.VERTICAL, false, false, false);
            styleBarChart(chart);
            drawChartImage(chart, 280f);
        }

        private void drawPiePetiChart() throws IOException {
            List<AnalyticsPortfolioDTO.ProjectMetrics> proyectos = data.proyectos();
            if (proyectos == null || proyectos.isEmpty()) return;

            long peti = proyectos.stream().filter(p -> Boolean.TRUE.equals(p.peti())).count();
            long noPeti = proyectos.size() - peti;

            if (peti == 0 && noPeti == 0) return;

            drawSectionTitle("DISTRIBUCION PETI");

            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            dataset.setValue("PETI (" + peti + ")", peti);
            dataset.setValue("NO PETI (" + noPeti + ")", noPeti);

            JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, false, false);
            stylePieChart(chart);
            drawChartImage(chart, 220f);
        }

        private void drawEficaciaEficienciaChart() throws IOException {
            List<AnalyticsPortfolioDTO.DependenciaMetrics> deps = data.dependencias();
            if (deps == null || deps.isEmpty()) return;

            drawSectionTitle("EFICACIA vs EFICIENCIA POR DEPENDENCIA");

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (AnalyticsPortfolioDTO.DependenciaMetrics d : deps) {
                double eficacia = d.eficaciaPromedio() != null ? d.eficaciaPromedio().doubleValue() : 0;
                double eficiencia = d.eficienciaPromedio() != null ? d.eficienciaPromedio().doubleValue() : 0;
                dataset.addValue(eficacia, LABEL_EFICACIA, nz(d.dependencia()));
                dataset.addValue(eficiencia, LABEL_EFICIENCIA, nz(d.dependencia()));
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    null, null, "%", dataset, PlotOrientation.VERTICAL, true, false, false);
            styleBarChart(chart);
            drawChartImage(chart, 280f);
        }

        private void drawChartImage(JFreeChart chart, float targetHeight) throws IOException {
            float aspect = targetHeight == 220f ? 320f / 220f : 520f / 240f;
            float targetWidth = targetHeight * aspect;
            int renderWidth = Math.max(1200, Math.round(targetWidth * 3f));
            int renderHeight = Math.max(800, Math.round(targetHeight * 3f));
            BufferedImage img = chart.createBufferedImage(renderWidth, renderHeight);
            ensureSpace(targetHeight + 14f);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, img);
            float x = LEFT + (CONTENT_WIDTH - targetWidth) / 2f;
            cs.drawImage(pdImage, x, cursorY - targetHeight, targetWidth, targetHeight);
            cursorY -= targetHeight + 14f;
        }

        private void styleBarChart(JFreeChart chart) {
            chart.setBackgroundPaint(Color.WHITE);
            chart.setAntiAlias(true);
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(COLOR_LINE);
            plot.setOutlinePaint(COLOR_LINE);
            plot.setOutlineStroke(new java.awt.BasicStroke(0.6f));

            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setTickLabelFont(CHART_FONT_REGULAR);
            domainAxis.setLabelFont(CHART_FONT_BOLD);
            domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setUpperBound(100);
            rangeAxis.setTickLabelFont(CHART_FONT_REGULAR);
            rangeAxis.setLabelFont(CHART_FONT_BOLD);

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(59, 130, 246));
            renderer.setDrawBarOutline(false);
            renderer.setShadowVisible(false);
            renderer.setSeriesOutlineStroke(0, new java.awt.BasicStroke(0f));
        }

        private void stylePieChart(JFreeChart chart) {
            chart.setBackgroundPaint(Color.WHITE);
            chart.setAntiAlias(true);
            PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);
            plot.setSectionPaint("PETI", new Color(16, 185, 129));
            plot.setSectionPaint("NO PETI", new Color(239, 68, 68));
            plot.setLabelFont(CHART_FONT_PIE);
            plot.setLabelPaint(COLOR_TEXT);
            plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1}"));
        }

        private void drawDependenciasSection() throws IOException {
            List<AnalyticsPortfolioDTO.DependenciaMetrics> deps = data.dependencias();
            if (deps == null || deps.isEmpty()) return;

            drawSectionTitle("DESEMPENO POR DEPENDENCIA");

            float[] widths = {0.28f, 0.12f, 0.13f, 0.13f, 0.13f, 0.10f, 0.11f};
            List<String> headers = List.of("Dependencia", "Proyectos", LABEL_AVANCE, LABEL_EFICACIA, LABEL_EFICIENCIA, "PETI", "Riesgos");

            drawTableHeader(headers, widths, 24f);
            for (int i = 0; i < deps.size(); i++) {
                AnalyticsPortfolioDTO.DependenciaMetrics d = deps.get(i);
                List<String> row = List.of(
                        nz(d.dependencia()),
                        String.valueOf(d.totalProyectos()),
                        fmtPct(d.avancePromedio()),
                        fmtPct(d.eficaciaPromedio()),
                        fmtPct(d.eficienciaPromedio()),
                        String.valueOf(d.proyectosPeti()),
                        d.riesgosTratados() + "/" + (d.riesgosTratados() + d.riesgosPendientes())
                );
                float rowHeight = computeRowHeight(row, widths, 9f, 2f, 18f);
                if (cursorY - rowHeight < BOTTOM) {
                    startPage();
                    drawTableHeader(headers, widths, 24f);
                }
                drawTableRow(row, widths, rowHeight, 9f, i);
            }
            cursorY -= 12f;
        }

        private void drawEstrategiasSection() throws IOException {
            List<AnalyticsPortfolioDTO.EstrategiaMetrics> estrategias = data.estrategias();
            if (estrategias == null || estrategias.isEmpty()) return;

            drawSectionTitle("DESEMPENO POR ESTRATEGIA PETI");

            float[] widths = {0.30f, 0.35f, 0.12f, 0.12f, 0.11f};
            List<String> headers = List.of(LABEL_CODIGO, "Estrategia", "Proyectos", LABEL_AVANCE, LABEL_EFICACIA);

            drawTableHeader(headers, widths, 24f);
            for (int i = 0; i < estrategias.size(); i++) {
                AnalyticsPortfolioDTO.EstrategiaMetrics e = estrategias.get(i);
                List<String> row = List.of(
                        nz(e.codigo()),
                        nz(e.nombre()),
                        String.valueOf(e.proyectos()),
                        fmtPct(e.avancePromedio()),
                        fmtPct(e.eficaciaPromedio())
                );
                float rowHeight = computeRowHeight(row, widths, 9f, 2f, 18f);
                if (cursorY - rowHeight < BOTTOM) {
                    startPage();
                    drawTableHeader(headers, widths, 24f);
                }
                drawTableRow(row, widths, rowHeight, 9f, i);
            }
            cursorY -= 12f;
        }

        private void drawFuragSection() throws IOException {
            AnalyticsPortfolioDTO.FuragMetrics furag = data.furag();
            if (furag == null) return;

            drawSectionTitle("COBERTURA FURAG");

            float cardWidth = (CONTENT_WIDTH - 24f) / 3f;
            float cardHeight = 48f;
            float gap = 12f;
            float x = LEFT;

            String[][] kpis = {
                    {"PROYECTOS CON FURAG", String.valueOf(furag.proyectosConFurag())},
                    {"COBERTURA PROMEDIO", fmtPct(furag.coberturaPromedio())},
                    {"RESPUESTAS COMPLETAS", furag.respuestasCompletas() + "/" + furag.respuestasObligatorias()},
            };
            for (String[] kpi : kpis) {
                ensureSpace(cardHeight + 10f);
                drawRoundedCard(x, cursorY - cardHeight, cardWidth, cardHeight, COLOR_CARD_BLUE, COLOR_LINE);
                drawText(kpi[0], FONT_BOLD, 7.8f, COLOR_MUTED, x + 8f, cursorY - 14f);
                drawText(kpi[1], FONT_BOLD, 13f, COLOR_TEXT, x + 8f, cursorY - 32f);
                x += cardWidth + gap;
            }
            cursorY -= cardHeight + 12f;

            List<AnalyticsPortfolioDTO.FuragProjectMetrics> proyectos = furag.proyectos();
            if (proyectos != null && !proyectos.isEmpty()) {
                float[] widths = {0.28f, 0.32f, 0.15f, 0.13f, 0.12f};
                List<String> headers = List.of(LABEL_CODIGO, "Nombre", "Cobertura", "Completas", "Obligatorias");
                drawTableHeader(headers, widths, 24f);
                for (int i = 0; i < proyectos.size(); i++) {
                    AnalyticsPortfolioDTO.FuragProjectMetrics p = proyectos.get(i);
                    List<String> row = List.of(
                            nz(p.proyectoId()),
                            nz(p.nombre()),
                            fmtPct(p.cobertura()),
                            String.valueOf(p.respuestasCompletas()),
                            String.valueOf(p.respuestasObligatorias())
                    );
                    float rowHeight = computeRowHeight(row, widths, 9f, 2f, 18f);
                    if (cursorY - rowHeight < BOTTOM) {
                        startPage();
                        drawTableHeader(headers, widths, 24f);
                    }
                    drawTableRow(row, widths, rowHeight, 9f, i);
                }
            }
            cursorY -= 12f;
        }

        private void drawRiesgosSection() throws IOException {
            AnalyticsPortfolioDTO.RiskMetrics riesgos = data.riesgos();
            if (riesgos == null) return;

            drawSectionTitle("GESTION DE RIESGOS");

            float cardWidth = (CONTENT_WIDTH - 36f) / 4f;
            float cardHeight = 48f;
            float gap = 12f;
            float x = LEFT;

            String[][] kpis = {
                    {"TOTAL RIESGOS", String.valueOf(riesgos.total())},
                    {"TRATADOS", String.valueOf(riesgos.tratados())},
                    {"PENDIENTES", String.valueOf(riesgos.pendientes())},
                    {"INDICE MITIGACION", fmtPct(riesgos.indiceMitigacion())},
            };
            for (String[] kpi : kpis) {
                ensureSpace(cardHeight + 10f);
                drawRoundedCard(x, cursorY - cardHeight, cardWidth, cardHeight, COLOR_CARD_YELLOW, COLOR_LINE);
                drawText(kpi[0], FONT_BOLD, 7.8f, COLOR_MUTED, x + 8f, cursorY - 14f);
                drawText(kpi[1], FONT_BOLD, 13f, COLOR_TEXT, x + 8f, cursorY - 32f);
                x += cardWidth + gap;
            }
            cursorY -= cardHeight + 12f;

            List<AnalyticsPortfolioDTO.RiskLevelMetrics> porNivel = riesgos.porNivel();
            if (porNivel != null && !porNivel.isEmpty()) {
                float[] widths = {0.50f, 0.50f};
                List<String> headers = List.of("Nivel de riesgo", "Cantidad");
                drawTableHeader(headers, widths, 24f);
                for (int i = 0; i < porNivel.size(); i++) {
                    AnalyticsPortfolioDTO.RiskLevelMetrics r = porNivel.get(i);
                    List<String> row = List.of(nz(r.nivel()), String.valueOf(r.cantidad()));
                    float rowHeight = computeRowHeight(row, widths, 9f, 2f, 18f);
                    if (cursorY - rowHeight < BOTTOM) {
                        startPage();
                        drawTableHeader(headers, widths, 24f);
                    }
                    drawTableRow(row, widths, rowHeight, 9f, i);
                }
            }
            cursorY -= 12f;
        }

        private void drawProyectosTable() throws IOException {
            List<AnalyticsPortfolioDTO.ProjectMetrics> proyectos = data.proyectos();
            if (proyectos == null || proyectos.isEmpty()) return;

            drawSectionTitle("DETALLE DE PROYECTOS");

            float[] widths = {0.15f, 0.22f, 0.16f, 0.10f, 0.10f, 0.10f, 0.09f, 0.08f};
            List<String> headers = List.of(LABEL_CODIGO, "Nombre", "Dependencia", LABEL_AVANCE, LABEL_EFICACIA, LABEL_EFICIENCIA, "Estado", "Atrasos");

            drawTableHeader(headers, widths, 24f);
            for (int i = 0; i < proyectos.size(); i++) {
                AnalyticsPortfolioDTO.ProjectMetrics p = proyectos.get(i);
                List<String> row = List.of(
                        nz(p.proyectoId()),
                        nz(p.nombre()),
                        nz(p.dependencia()),
                        fmtPct(p.avance()),
                        fmtPct(p.eficacia()),
                        fmtPct(p.eficiencia()),
                        nz(p.estado()),
                        String.valueOf(p.atrasados())
                );
                float rowHeight = computeRowHeight(row, widths, 8.5f, 2f, 16f);
                if (cursorY - rowHeight < BOTTOM) {
                    startPage();
                    drawTableHeader(headers, widths, 24f);
                }
                drawTableRow(row, widths, rowHeight, 8.5f, i);
            }
        }

        private void drawSectionTitle(String title) throws IOException {
            ensureSpace(30f);
            drawText(title, FONT_BOLD, 12f, COLOR_SECTION, LEFT, cursorY);
            cursorY -= 4f;
            drawLine(LEFT, cursorY, PAGE_WIDTH - RIGHT, cursorY, COLOR_LINE, 0.8f);
            cursorY -= 16f;
        }

        private void drawTableHeader(List<String> headers, float[] widths, float rowHeight) throws IOException {
            ensureSpace(rowHeight + 6f);
            PdfReportShared.drawTableHeader(cs, cursorY, headers, widths, rowHeight, 8.2f, false, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);
            cursorY -= rowHeight;
        }

        private void drawTableRow(List<String> row, float[] widths, float rowHeight, float fontSize, int rowIndex) throws IOException {
            ensureSpace(rowHeight + 4f);
            PdfReportShared.drawTableRow(cs, cursorY, row, widths, rowHeight, fontSize, rowIndex, FONT_REGULAR, COLOR_TEXT, this::drawText, this::wrapText);
            cursorY -= rowHeight;
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

        private void drawWatermark() throws IOException {
            cs.saveGraphicsState();
            cs.setGraphicsStateParameters(watermarkState);
            cs.setNonStrokingColor(COLOR_WATERMARK);
            cs.beginText();
            cs.setFont(FONT_BOLD, 46f);
            cs.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(
                    Math.toRadians(33d), PAGE_WIDTH * 0.34f, PAGE_HEIGHT * 0.44f));
            cs.showText("PROYECTA");
            cs.endText();

            cs.beginText();
            cs.setFont(FONT_BOLD, 16f);
            cs.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(
                    Math.toRadians(33d), PAGE_WIDTH * 0.43f, PAGE_HEIGHT * 0.34f));
            cs.showText("GOBERNACION DE CUNDINAMARCA");
            cs.endText();
            cs.restoreGraphicsState();
            cs.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawLine(float x1, float y1, float x2, float y2, Color stroke, float lineWidth) throws IOException {
            cs.setStrokingColor(stroke);
            cs.setLineWidth(lineWidth);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
            cs.setStrokingColor(COLOR_TEXT);
        }

        private void drawText(String text, PDFont font, float size, Color color, float x, float y) throws IOException {
            cs.beginText();
            cs.setNonStrokingColor(color);
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(sanitize(text));
            cs.endText();
            cs.setNonStrokingColor(COLOR_TEXT);
        }

        @SuppressWarnings("java:S107")
        private void drawWrappedCentered(List<String> lines, PDFont font, float size, Color color, float x, float y, float width, float leading) throws IOException {
            PdfReportShared.drawWrappedCentered(lines, font, size, color, x, y, width, leading, this::drawText, this::stringWidth);
        }

        private void drawRoundedCard(float x, float y, float width, float height, Color fill, Color stroke) throws IOException {
            drawFilledRect(x, y, width, height, fill);
            drawRect(x, y, width, height, stroke, 0.8f);
        }

        private void drawFilledRect(float x, float y, float width, float height, Color fill) throws IOException {
            cs.setNonStrokingColor(fill);
            cs.addRect(x, y, width, height);
            cs.fill();
            cs.setNonStrokingColor(COLOR_TEXT);
        }

        private void drawRect(float x, float y, float width, float height, Color stroke, float lineWidth) throws IOException {
            cs.setStrokingColor(stroke);
            cs.setLineWidth(lineWidth);
            cs.addRect(x, y, width, height);
            cs.stroke();
            cs.setStrokingColor(COLOR_TEXT);
        }

        private void closeContent() throws IOException {
            if (cs != null) {
                drawFooter();
                cs.close();
                cs = null;
            }
        }

        private void drawFooter() throws IOException {
            if (cs == null) return;
            cs.saveGraphicsState();
            PdfReportShared.drawFooterBlock(cs, footerLogo, FONT_REGULAR, FONT_BOLD, COLOR_TEXT, COLOR_MUTED, this::drawText);

            drawText("Reporte institucional | Pagina " + pageNumber, FONT_OBLIQUE, 8.2f, COLOR_FOOTER, LEFT, BOTTOM);
            cs.restoreGraphicsState();
        }

        private static final Pattern WHITESPACE = Pattern.compile("\\s+");
        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String normalized = text == null ? "" : text.trim();
            if (normalized.isEmpty()) return List.of("");

            List<String> result = new ArrayList<>();
            String[] paragraphs = normalized.split("\\r?\\n");
            for (String paragraph : paragraphs) {
                StringBuilder current = new StringBuilder();
                for (String word : WHITESPACE.split(paragraph)) {
                    if (word.isBlank()) continue;
                    PdfTextWrap.appendWordTrial(word, current, result, maxWidth, s -> stringWidth(font, fontSize, s));
                }
                if (!current.isEmpty()) {
                    result.add(current.toString());
                }
            }
            return result.isEmpty() ? List.of("") : result;
        }

        private float stringWidth(PDFont font, float size, String value) throws IOException {
            return PdfReportShared.stringWidth(font, size, value, PdfRenderer::sanitize);
        }

        private Color accentColor(String accent) {
            return PdfReportShared.accentColor(accent, COLOR_CARD_BLUE);
        }

        private PDImageXObject loadImage(PDDocument doc, String path) {
            try (InputStream stream = AnalyticsPdfGenerator.class.getResourceAsStream(path)) {
                if (stream == null) return null;
                BufferedImage image = javax.imageio.ImageIO.read(stream);
                return image == null ? null : LosslessFactory.createFromImage(doc, image);
            } catch (Exception _) {
                return null;
            }
        }

        private static String sanitize(String value) {
            return PdfReportShared.sanitize(value, "ñ", "n", "Ñ", "N");
        }

        private static String nz(String value) {
            return value == null ? "" : value;
        }

        private static String fmtPct(BigDecimal value) {
            if (value == null) return "0%";
            return value.setScale(1, RoundingMode.HALF_UP).toString() + "%";
        }
    }
}
