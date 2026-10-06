package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.application.readmodel.EntregablePendienteDTO;
import com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO;
import com.proyecta.api_gestion.domain.model.enums.DetailMode;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProyectosConRetrasosPdfGenerator {

    public byte[] build(List<ProyectoReporteResumenDTO> proyectos, List<EntregablePendienteDTO> entregables, LocalDate corte, String detailMode) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = FontPack.load(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);
            canvas.render(proyectos, entregables, corte, mode);
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de proyectos con retrasos.", ex);
        }
    }

    private record DeliverableRow(String numero, String nombre, String fecha) {
    }

    private static final class PdfCanvas extends PdfReportCanvas {

        private static final float[] PROJECT_CELL_SIZES = {9.2f, 9.2f, 9.6f, 9.2f};
        private static final float[] DELIVERABLE_CELL_SIZES = {9.8f, 9.2f, 9.2f};

        private PdfCanvas(PDDocument document, FontPack fonts) throws IOException {
            super(document, fonts);
        }

        void render(List<ProyectoReporteResumenDTO> proyectos, List<EntregablePendienteDTO> entregables, LocalDate corte, DetailMode mode) throws IOException {
            List<ProyectoReporteResumenDTO> rows = proyectos == null ? List.of() : proyectos.stream()
                    .sorted(Comparator.comparing(ProyectoReporteResumenDTO::id, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
            List<DeliverableRow> detailRows = buildDeliverableRows(entregables);

            startPage();
            renderHeader();
            renderTitle();
            renderMeta(corte);
            renderDetailMode(mode);
            renderProjectsTable(rows, mode);
            renderDeliverablesHeading();
            renderDeliverablesTable(detailRows);
            finishPage();
        }

        private void renderDetailMode(DetailMode mode) throws IOException {
            if (mode.isDetailed()) {
                drawLabelValue("NIVEL DE DETALLE:", "[Detallado]", 570f, true);
            } else {
                drawLabelValue("NIVEL DE DETALLE:", "[Resumido]", 570f, true);
            }
        }

        private List<DeliverableRow> buildDeliverableRows(List<EntregablePendienteDTO> entregables) {
            if (entregables == null || entregables.isEmpty()) {
                return List.of(
                        new DeliverableRow("1", "", FECHA_PLACEHOLDER),
                        new DeliverableRow("2", "", FECHA_PLACEHOLDER),
                        new DeliverableRow("3", "", FECHA_PLACEHOLDER)
                );
            }
            List<DeliverableRow> rows = new ArrayList<>();
            int index = 1;
            for (EntregablePendienteDTO entregable : entregables) {
                rows.add(new DeliverableRow(
                        String.valueOf(index++),
                        safe(entregable.nombre()),
                        entregable.fechaEntrega() != null ? formatDate(entregable.fechaEntrega()) : FECHA_PLACEHOLDER
                ));
            }
            return rows;
        }

        private void renderTitle() throws IOException {
            drawText("REPORTE: PROYECTOS CON RETRASOS EN LA FECHA DE", fonts.bold(), 17.2f, LEFT, 730f, COLOR_TEXT);
            drawText("ENTREGA.", fonts.bold(), 17.2f, LEFT, 702f, COLOR_TEXT);
        }

        private void renderMeta(LocalDate corte) throws IOException {
            drawLabelValue("FILTRO:", "[Todos los proyectos]", 660f, true);
            drawLabelValue("Fecha del reporte:", formatDate(corte), 620f, false);
            drawLabelValue("Vigencia:", "[2024-2027]", 595f, false);
            cursorY = 530f;
        }

        private void renderProjectsTable(List<ProyectoReporteResumenDTO> rows, DetailMode mode) throws IOException {
            float[] widths;
            String[] headers;
            float[] sizes;

            if (mode.isDetailed()) {
                widths = new float[]{0.15f, 0.35f, 0.20f, 0.30f};
                headers = new String[]{
                        "Codigo del\nproyecto",
                        "Nombre del proyecto",
                        "Avance total (%)",
                        "Dependencia"
                };
                sizes = new float[]{8.5f, 8.5f, 8.3f, 8.5f};
            } else {
                widths = new float[]{0.20f, 0.45f, 0.35f};
                headers = new String[]{
                        "Codigo del\nproyecto",
                        "Nombre del proyecto",
                        "Avance total del proyecto (%)"
                };
                sizes = new float[]{8.8f, 8.8f, 8.6f};
            }

            float topY = cursorY;
            float headerHeight = drawTableHeader(topY, headers, widths, sizes, 28f);
            cursorY = topY - headerHeight;

            if (rows.isEmpty()) {
                cursorY -= 6f;
                return;
            }

            for (ProyectoReporteResumenDTO row : rows) {
                float rowHeight = 22f;
                drawProjectRow(row, widths, rowHeight, mode);
                cursorY -= rowHeight;
            }

            cursorY -= 60f;
        }

        private void renderDeliverablesHeading() throws IOException {
            drawText("Nombre de los entregables que tienen fecha vencida", fonts.bold(), 10.5f, LEFT, cursorY, COLOR_TEXT);
            cursorY -= 26f;
        }

        private void renderDeliverablesTable(List<DeliverableRow> rows) throws IOException {
            float[] widths = new float[]{0.09f, 0.51f, 0.40f};
            String[] headers = {
                    "",
                    "Nombre de los entregables que tienen fecha\nvencida",
                    "Fecha de entrega programada"
            };
            float[] sizes = {8.8f, 8.5f, 8.5f};
            float topY = cursorY;
            float headerHeight = drawTableHeader(topY, headers, widths, sizes, 28f);
            cursorY = topY - headerHeight;

            for (DeliverableRow row : rows) {
                float rowHeight = 22f;
                drawDeliverableRow(row, widths, rowHeight);
                cursorY -= rowHeight;
            }
        }

        private void drawProjectRow(ProyectoReporteResumenDTO row, float[] widths, float rowHeight, DetailMode mode) throws IOException {
            String[] values;
            if (mode.isDetailed()) {
                values = new String[]{safe(row.id()), safe(row.nombre()), formatPercent(row.avance()), safe(row.dependencia())};
            } else {
                values = new String[]{safe(row.id()), safe(row.nombre()), formatPercent(row.avance())};
            }
            drawPlainRow(values, widths, PROJECT_CELL_SIZES, rowHeight);
        }

        private void drawDeliverableRow(DeliverableRow row, float[] widths, float rowHeight) throws IOException {
            drawPlainRow(new String[]{row.numero(), row.nombre(), row.fecha()}, widths, DELIVERABLE_CELL_SIZES, rowHeight);
        }

        private static String formatPercent(java.math.BigDecimal value) {
            if (value == null) return "0.00%";
            java.math.BigDecimal normalized = value;
            if (normalized.compareTo(java.math.BigDecimal.ONE) <= 0) {
                normalized = normalized.multiply(java.math.BigDecimal.valueOf(100));
            }
            return normalized.setScale(2, java.math.RoundingMode.HALF_UP) + "%";
        }
    }
}
