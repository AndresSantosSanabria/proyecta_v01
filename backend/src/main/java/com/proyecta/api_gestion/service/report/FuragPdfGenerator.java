package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.enums.DetailMode;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Genera el PDF institucional de FURAG por dependencia con el formato de referencia.
 */
public final class FuragPdfGenerator {

    public byte[] build(Proyecto proyecto, List<Proyecto> proyectosDependencia, LocalDate corte, String detailMode,
                        java.util.Set<String> proyectosFuragCompletos) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontPack fonts = FontPack.load(document);
            PdfCanvas canvas = new PdfCanvas(document, fonts);
            DetailMode mode = DetailMode.from(detailMode);
            canvas.render(proyecto, proyectosDependencia, corte, mode,
                    proyectosFuragCompletos == null ? java.util.Set.of() : proyectosFuragCompletos);
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte FURAG.", ex);
        }
    }

    private static final class PdfCanvas extends PdfReportCanvas {

        private PdfCanvas(PDDocument document, FontPack fonts) throws IOException {
            super(document, fonts);
        }

        void render(Proyecto proyecto, List<Proyecto> proyectosDependencia, LocalDate corte, DetailMode mode,
                    java.util.Set<String> proyectosFuragCompletos) throws IOException {
            List<FilaProyecto> rows = buildRows(proyectosDependencia, proyectosFuragCompletos);
            renderReporteCinco(rows, "No hay proyectos registrados para la dependencia seleccionada.",
                    this::renderTitle,
                    () -> renderMeta(proyecto, corte),
                    () -> renderModoDetalle(mode, "[Detallado - Incluye respuestas por pregunta]"),
                    () -> renderResumenFilas(rows, "Conteo de proyectos que responden preguntas FURAG:",
                            "Conteo de proyectos que NO responden preguntas FURAG:"),
                    () -> renderEncabezadoTablaCinco("Responde todas\nlas preguntas\nFURAG", 8.1f, 28f));
        }

        private void renderTitle() throws IOException {
            drawText("REPORTE: PREGUNTAS FURAG.", fonts.bold(), 17.2f, LEFT, 730f, COLOR_TEXT);
        }

        private void renderMeta(Proyecto proyecto, LocalDate corte) throws IOException {
            drawLabelValue("FILTRO:", "[Proyectos por dependencia]", 690f, true);
            drawLabelValue("Fecha del reporte:", formatDate(corte), 650f, false);
            drawLabelValue("Vigencia:", formatVigencia(proyecto), 625f, false);
            cursorY = 560f;
        }

        private static String formatVigencia(Proyecto proyecto) {
            if (proyecto == null || proyecto.getVigenciaPeti() == null || proyecto.getVigenciaPeti().isBlank()) {
                return "[2024-2027]";
            }
            return proyecto.getVigenciaPeti().trim();
        }

        private static List<FilaProyecto> buildRows(List<Proyecto> proyectosDependencia, java.util.Set<String> proyectosFuragCompletos) {
            if (proyectosDependencia == null || proyectosDependencia.isEmpty()) {
                return List.of();
            }
            return proyectosDependencia.stream()
                    .sorted(Comparator.comparing(Proyecto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .map(proyecto -> new FilaProyecto(
                            safe(proyecto.getId()),
                            safe(proyecto.getNombre()),
                            safe(proyecto.getDirector()),
                            safe(proyecto.getDependencia()),
                            proyecto != null && proyecto.getId() != null && proyectosFuragCompletos.contains(proyecto.getId())
                    ))
                    .toList();
        }
    }
}
