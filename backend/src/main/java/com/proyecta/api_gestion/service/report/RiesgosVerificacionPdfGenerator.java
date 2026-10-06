package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.dto.report.RiesgoVerificacionReporteDTO;
import com.proyecta.api_gestion.domain.model.enums.DetailMode;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class RiesgosVerificacionPdfGenerator {

    public byte[] build(List<RiesgoVerificacionReporteDTO> reportes, LocalDate corte, String detailMode) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = FontPack.load(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);
            canvas.render(reportes, corte, mode);
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte de riesgos.", ex);
        }
    }

    private static final class PdfCanvas extends PdfReportCanvas {

        private PdfCanvas(PDDocument document, FontPack fonts) throws IOException {
            super(document, fonts);
        }

        void render(List<RiesgoVerificacionReporteDTO> reportes, LocalDate corte, DetailMode mode) throws IOException {
            List<FilaProyecto> rows = buildRows(reportes);
            renderReporteCinco(rows, "No hay proyectos con cierre registrados.",
                    this::renderTitle,
                    () -> renderMeta(corte),
                    () -> renderModoDetalle(mode, "[Detallado - Incluye detalle por proyecto]"),
                    () -> renderResumenFilas(rows, "Conteo de proyectos que diligenciaron tratamiento de riesgos:",
                            "Conteo de proyectos que NO diligenciaron tratamiento de riesgos:"),
                    () -> renderEncabezadoTablaCinco("Diligencio\ntratamiento de\nriesgos", 8.1f, 30f));
        }

        private List<FilaProyecto> buildRows(List<RiesgoVerificacionReporteDTO> reportes) {
            if (reportes == null || reportes.isEmpty()) {
                return List.of();
            }
            return reportes.stream()
                    .sorted(Comparator.comparing(RiesgoVerificacionReporteDTO::proyectoId, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .map(reporte -> new FilaProyecto(
                            safe(reporte.proyectoId()),
                            safe(reporte.nombreProyecto()),
                            safe(reporte.directorProyecto()),
                            safe(reporte.dependencia()),
                            reporte.diligencioTratamiento()
                    ))
                    .toList();
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
    }
}
