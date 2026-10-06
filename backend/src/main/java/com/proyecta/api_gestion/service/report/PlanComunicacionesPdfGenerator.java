package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.enums.DetailMode;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class PlanComunicacionesPdfGenerator {

    public byte[] build(List<Proyecto> proyectos, LocalDate corte, String detailMode) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = FontPack.load(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);
            canvas.render(proyectos, corte, mode);
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de plan de comunicaciones.", ex);
        }
    }

    private static final class PdfCanvas extends PdfReportCanvas {

        private PdfCanvas(PDDocument document, FontPack fonts) throws IOException {
            super(document, fonts);
        }

        void render(List<Proyecto> proyectos, LocalDate corte, DetailMode mode) throws IOException {
            List<FilaProyecto> rows = buildRows(proyectos);
            renderReporteCinco(rows, "No hay proyectos No PETI registrados.",
                    this::renderTitle,
                    () -> renderMeta(corte),
                    () -> renderModoDetalle(mode, "[Detallado - Incluye detalle del plan]"),
                    () -> renderResumenFilas(rows, "Conteo de proyectos con plan de comunicaciones:",
                            "Conteo de proyectos sin plan de comunicaciones:"),
                    () -> renderEncabezadoTablaCinco("Plan de\ncomunicaciones", 8.4f, 28f));
        }

        private static List<FilaProyecto> buildRows(List<Proyecto> proyectos) {
            if (proyectos == null || proyectos.isEmpty()) {
                return List.of();
            }
            return proyectos.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getPeti()))
                    .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .map(p -> new FilaProyecto(
                            safe(p.getId()),
                            safe(p.getNombre()),
                            safe(p.getDirector()),
                            safe(p.getDependencia()),
                            p.getPlanComunicacionesPdf() != null
                    ))
                    .toList();
        }

        private void renderTitle() throws IOException {
            drawText("REPORTE: PLAN DE COMUNICACIONES.", fonts.bold(), 17.2f, LEFT, 730f, COLOR_TEXT);
        }

        private void renderMeta(LocalDate corte) throws IOException {
            drawLabelValue("FILTRO:", "[Proyectos No PETI]", 690f, true);
            drawLabelValue("Fecha del reporte:", formatDate(corte), 650f, false);
            drawLabelValue("Vigencia:", "[2024-2027]", 625f, false);
            cursorY = 560f;
        }
    }
}
