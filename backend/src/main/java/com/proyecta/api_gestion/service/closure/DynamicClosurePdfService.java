package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DynamicClosurePdfService {

    private static final Logger log = LoggerFactory.getLogger(DynamicClosurePdfService.class);
    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    public DynamicClosurePdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] generatePdf(String templateJson, String formDataJson,
                               String codigoProceso, Integer versionNum, String nombreDocumento) {
        try {
            String html = buildHtml(templateJson, formDataJson, codigoProceso, versionNum, nombreDocumento);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, ".");
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF dinamico del acta de cierre: {}", e.getMessage(), e);
            throw new RuntimeException("No fue posible generar el PDF del acta de cierre.", e);
        }
    }

    private String buildHtml(String templateJson, String formDataJson,
                              String codigoProceso, Integer versionNum, String nombreDocumento) throws JsonProcessingException {
        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode formData = parseFormData(formDataJson);

        StringBuilder body = new StringBuilder();
        body.append(buildHeader(codigoProceso, versionNum, nombreDocumento));

        JsonNode secciones = template.get("secciones");
        if (secciones != null && secciones.isArray()) {
            List<JsonNode> orderedSections = new ArrayList<>();
            secciones.forEach(orderedSections::add);
            orderedSections.sort(Comparator.comparingInt(n -> n.path("orden").asInt(Integer.MAX_VALUE)));
            int sectionCounter = 0;
            for (JsonNode seccion : orderedSections) {
                if (!isSectionActive(seccion)) {
                    continue;
                }
                sectionCounter++;
                String tipo = seccion.has("tipo_seccion") ? seccion.get("tipo_seccion").asText() : "formulario";
                String titulo = seccion.has("titulo") ? seccion.get("titulo").asText() : "";

                body.append("<div class='section'>");
                body.append("<h2>").append(sectionCounter).append(". ").append(escapeHtml(titulo)).append("</h2>");

                if ("tabla".equals(tipo)) {
                    body.append(buildTableSection(seccion, formData));
                } else {
                    body.append(buildFormSection(seccion, formData));
                }
                body.append("</div>");
            }
        }

        body.append(buildFooter());

        return wrapInDocument(body.toString());
    }

    private JsonNode parseFormData(String formDataJson) {
        if (formDataJson == null || formDataJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode data = objectMapper.readTree(formDataJson);
            if (data.has("fields")) {
                return data.get("fields");
            }
            return data;
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear formData: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private boolean isSectionActive(JsonNode seccion) {
        String tipo = seccion.has("tipo_seccion") ? seccion.get("tipo_seccion").asText() : "formulario";
        if ("formulario".equals(tipo)) {
            JsonNode campos = seccion.get("campos");
            if (campos == null || !campos.isArray()) return false;
            for (JsonNode campo : campos) {
                if (isFieldActive(campo)) return true;
            }
            return false;
        }
        if ("tabla".equals(tipo)) {
            JsonNode columnas = seccion.get("columnas");
            if (columnas == null || !columnas.isArray()) return false;
            for (JsonNode col : columnas) {
                if (isColumnActive(col)) return true;
            }
            return false;
        }
        return true;
    }

    private boolean isFieldActive(JsonNode campo) {
        return !campo.has("activo") || campo.get("activo").asBoolean(true);
    }

    private boolean isColumnActive(JsonNode col) {
        return !col.has("activo") || col.get("activo").asBoolean(true);
    }

    private String getCampoId(JsonNode campo) {
        if (campo.has("id_campo") && !campo.get("id_campo").asText("").isBlank()) {
            return campo.get("id_campo").asText();
        }
        return campo.has("id") ? campo.get("id").asText() : "";
    }

    private String buildHeader(String codigoProceso, Integer versionNum, String nombreDocumento) {
        StringBuilder h = new StringBuilder();
        h.append("<div class='header'>");
        h.append("<div class='header-top'>");
        try {
            ClassPathResource logoResource = new ClassPathResource("report-assets/logo gob cun.png");
            if (logoResource.exists()) {
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                h.append("<img src='data:image/png;base64,").append(logoBase64).append("' class='logo' />");
            }
        } catch (IOException e) {
            log.warn("Logo no encontrado para PDF dinamico");
        }
        h.append("<div class='header-info'>");
        h.append("<span class='codigo-proceso'>").append(escapeHtml(codigoProceso)).append("</span>");
        h.append("<span class='version'>V").append(versionNum).append("</span>");
        h.append("</div>");
        h.append("</div>");
        h.append("<h1>").append(escapeHtml(nombreDocumento)).append("</h1>");
        h.append("</div>");
        return h.toString();
    }

    private String buildFormSection(JsonNode seccion, JsonNode formData) {
        StringBuilder sb = new StringBuilder();
        JsonNode campos = seccion.get("campos");
        if (campos == null || !campos.isArray()) return sb.toString();

        List<JsonNode> orderedFields = new ArrayList<>();
        campos.forEach(orderedFields::add);
        orderedFields.sort(Comparator.comparingInt(n -> n.path("orden").asInt(Integer.MAX_VALUE)));

        sb.append("<div class='form-fields'>");
        for (JsonNode campo : orderedFields) {
            if (!isFieldActive(campo)) {
                continue;
            }
            String campoId = getCampoId(campo);
            String label = campo.has("label") ? campo.get("label").asText() : "";
            String tipo = campo.has("tipo_input") ? campo.get("tipo_input").asText() : "texto_corto";
            boolean requerido = campo.has("requerido") && campo.get("requerido").asBoolean(false);

            String value = "";
            if (campo.has("resolvedValue") && !campo.get("resolvedValue").asText("").isBlank()) {
                value = campo.get("resolvedValue").asText();
            } else if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                value = "";
            } else {
                value = resolveValue(formData, campoId);
            }

            sb.append("<div class='field'>");
            sb.append("<label>").append(escapeHtml(label));
            if (requerido) {
                sb.append(" <span style='color:#dc2626;'>*</span>");
            }
            sb.append("</label>");

            if (value.isEmpty()) {
                value = requerido ? "[Sin respuesta]" : "";
            }

            if ("texto_largo".equals(tipo)) {
                sb.append("<div class='text-block'>").append(escapeHtml(value)).append("</div>");
            } else {
                sb.append("<div class='text-inline'>").append(escapeHtml(value)).append("</div>");
            }
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildTableSection(JsonNode seccion, JsonNode formData) {
        StringBuilder sb = new StringBuilder();
        JsonNode columnas = seccion.get("columnas");
        if (columnas == null || !columnas.isArray() || columnas.isEmpty()) return sb.toString();

        String seccionId = seccion.has("id") ? seccion.get("id").asText() : "";

        var activeColumns = new java.util.ArrayList<JsonNode>();
        for (JsonNode col : columnas) {
            if (isColumnActive(col)) {
                activeColumns.add(col);
            }
        }
        activeColumns.sort(Comparator.comparingInt(n -> n.path("orden").asInt(Integer.MAX_VALUE)));
        if (activeColumns.isEmpty()) return sb.toString();

        sb.append("<table class='data-table'>");
        sb.append("<thead><tr>");
        for (JsonNode col : activeColumns) {
            sb.append("<th>").append(escapeHtml(col.get("label").asText())).append("</th>");
        }
        sb.append("</tr></thead>");

        sb.append("<tbody>");
        JsonNode rows = formData.get(seccionId);
        if (rows != null && rows.isArray() && !rows.isEmpty()) {
            for (JsonNode row : rows) {
                sb.append("<tr>");
                for (JsonNode col : activeColumns) {
                    String colId = col.has("id_campo") ? col.get("id_campo").asText() : col.get("id").asText();
                    String val = row.has(colId) ? row.get(colId).asText("") : "";
                    sb.append("<td>").append(escapeHtml(val)).append("</td>");
                }
                sb.append("</tr>");
            }
        } else {
            sb.append("<tr><td colspan='").append(activeColumns.size()).append("' class='empty-row'>Sin datos</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String buildFooter() {
        StringBuilder f = new StringBuilder();
        f.append("<div class='footer'>");
        f.append("<div class='signature-lines'>");
        f.append("<div class='sig-block'><div class='sig-line'></div><span>Director del Proyecto</span></div>");
        f.append("<div class='sig-block'><div class='sig-line'></div><span>Patrocinador</span></div>");
        f.append("<div class='sig-block'><div class='sig-line'></div><span>Gestor TIC</span></div>");
        f.append("</div>");
        f.append("<p class='footer-text'>Documento generado por PROYECTA - ").append(LocalDate.now().format(UI_DATE)).append("</p>");
        f.append("</div>");
        return f.toString();
    }

    private String resolveValue(JsonNode formData, String fieldId) {
        if (formData == null) return "";
        if (formData.has(fieldId)) {
            JsonNode val = formData.get(fieldId);
            return val.isTextual() ? val.asText() : val.toString();
        }
        return "";
    }

    private String wrapInDocument(String body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>" +
                "<style>" + getPdfStyles() + "</style></head><body>" + body + "</body></html>";
    }

    private String getPdfStyles() {
        return """
            @page { size: A4; margin: 40pt 40pt 42pt 40pt; }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: 'Helvetica', Arial, sans-serif; font-size: 10pt; color: #1a1a1a; line-height: 1.5; }
            .header { text-align: center; margin-bottom: 20pt; border-bottom: 2pt solid #16a34a; padding-bottom: 12pt; }
            .header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8pt; }
            .logo { height: 50pt; }
            .header-info { text-align: right; }
            .codigo-proceso { font-size: 9pt; color: #555; display: block; }
            .version { font-size: 9pt; color: #555; }
            h1 { font-size: 16pt; color: #16a34a; margin-top: 6pt; }
            .section { margin-bottom: 16pt; page-break-inside: avoid; }
            h2 { font-size: 12pt; color: #16a34a; border-bottom: 1pt solid #d1d5db; padding-bottom: 4pt; margin-bottom: 8pt; }
            .form-fields { display: flex; flex-direction: column; gap: 6pt; }
            .field { margin-bottom: 4pt; }
            .field label { font-weight: bold; font-size: 9pt; color: #374151; display: block; margin-bottom: 2pt; }
            .text-block { font-size: 10pt; color: #1a1a1a; white-space: pre-wrap; min-height: 14pt; border-bottom: 0.5pt solid #e5e7eb; padding-bottom: 2pt; }
            .text-inline { font-size: 10pt; color: #1a1a1a; border-bottom: 0.5pt solid #e5e7eb; padding-bottom: 2pt; }
            .data-table { width: 100%%; border-collapse: collapse; margin-top: 6pt; font-size: 9pt; }
            .data-table th { background: #f3f4f6; border: 1pt solid #d1d5db; padding: 4pt 6pt; text-align: left; font-weight: bold; }
            .data-table td { border: 1pt solid #d1d5db; padding: 4pt 6pt; }
            .data-table tr:nth-child(even) td { background: #f9fafb; }
            .empty-row { text-align: center; color: #9ca3af; font-style: italic; }
            .footer { margin-top: 30pt; border-top: 1pt solid #d1d5db; padding-top: 12pt; }
            .signature-lines { display: flex; justify-content: space-around; margin-top: 20pt; }
            .sig-block { text-align: center; width: 30%%; }
            .sig-line { border-top: 1pt solid #1a1a1a; margin-bottom: 4pt; margin-top: 40pt; }
            .sig-block span { font-size: 8pt; color: #555; }
            .footer-text { text-align: center; font-size: 8pt; color: #9ca3af; margin-top: 12pt; }
            """;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
