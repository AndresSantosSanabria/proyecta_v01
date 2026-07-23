package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proyecta.api_gestion.model.Patrocinador;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Entregable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;

@Service
public class TemplateResolver {

    private static final Logger log = LoggerFactory.getLogger(TemplateResolver.class);
    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObjectMapper objectMapper;

    public TemplateResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String resolveTemplateWithAnswers(String templateJson, Map<Long, String> answerMap) {
        return resolveTemplate(templateJson, Map.of(), answerMap);
    }

    public String resolveTemplateForProject(String templateJson, Proyecto proyecto, Map<Long, String> answerMap) {
        return resolveTemplate(templateJson, buildProjectValues(proyecto), answerMap);
    }

    public Map<String, Object> resolveVisibleFieldValues(String templateJson, Proyecto proyecto, Map<Long, String> answerMap) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return Map.of();

            Map<String, Object> values = new HashMap<>();
            for (JsonNode seccion : secciones) {
                String tipo = seccion.has("tipo_seccion") ? seccion.get("tipo_seccion").asText() : "formulario";
                if ("formulario".equals(tipo)) {
                    JsonNode campos = seccion.get("campos");
                    if (campos == null || !campos.isArray()) continue;
                    for (JsonNode campo : campos) {
                        if (!campo.has("id")) {
                            continue;
                        }
                        String fieldId = campo.get("id").asText("");
                        if (fieldId.isBlank()) {
                            continue;
                        }
                        String resolved = resolveProjectValue(fieldId, proyecto);
                        if (resolved == null || resolved.isBlank()) {
                            if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                                Long questionId = campo.get("questionId").asLong();
                                resolved = answerMap.getOrDefault(questionId, "");
                            }
                        }
                        if (resolved != null && !resolved.isBlank()) {
                            values.put(fieldId, resolved);
                        }
                    }
                }
            }
            return values;
        } catch (JsonProcessingException e) {
            log.error("Error leyendo plantilla para resolver valores: {}", e.getMessage());
            return Map.of();
        }
    }

    public Map<Long, String> extractMissingQuestions(String templateJson, Proyecto proyecto, Map<Long, String> answerMap) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return Map.of();

            Map<Long, String> missing = new HashMap<>();
            for (JsonNode seccion : secciones) {
                if (!"formulario".equals(seccion.path("tipo_seccion").asText("formulario"))) {
                    continue;
                }
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    if (!campo.has("questionId") || campo.get("questionId").isNull()) {
                        continue;
                    }
                    Long questionId = campo.get("questionId").asLong();
                    if (answerMap.containsKey(questionId) && answerMap.get(questionId) != null && !answerMap.get(questionId).isBlank()) {
                        continue;
                    }
                    String fieldId = campo.path("id").asText("");
                    if (!fieldId.isBlank() && resolveProjectValue(fieldId, proyecto) != null && !resolveProjectValue(fieldId, proyecto).isBlank()) {
                        continue;
                    }
                    String label = campo.path("label").asText("");
                    missing.put(questionId, label);
                }
            }
            return missing;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public boolean hasLinkedQuestions(String templateJson) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return false;

            for (JsonNode seccion : secciones) {
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private String resolveTemplate(String templateJson, Map<String, Object> projectValues, Map<Long, String> answerMap) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return templateJson;

            for (JsonNode seccion : secciones) {
                if (!seccion.isObject()) {
                    continue;
                }
                ObjectNode sectionNode = (ObjectNode) seccion;
                String tipo = seccion.has("tipo_seccion") ? seccion.get("tipo_seccion").asText() : "formulario";
                if (!"formulario".equals(tipo) && !"tabla".equals(tipo)) {
                    continue;
                }
                if ("formulario".equals(tipo)) {
                    JsonNode campos = seccion.get("campos");
                    if (campos == null || !campos.isArray()) continue;
                    for (JsonNode campo : campos) {
                        if (!campo.isObject()) {
                            continue;
                        }
                        ObjectNode fieldNode = (ObjectNode) campo;
                        String fieldId = campo.path("id").asText("");
                        if (!fieldId.isBlank() && projectValues.containsKey(fieldId)) {
                            Object value = projectValues.get(fieldId);
                            if (value != null && !String.valueOf(value).isBlank()) {
                                fieldNode.put("resolvedValue", String.valueOf(value));
                                fieldNode.put("readonly", true);
                            }
                        }
                        if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                            Long questionId = campo.get("questionId").asLong();
                            String answer = answerMap.getOrDefault(questionId, "");
                            if (answer != null && !answer.isBlank()) {
                                fieldNode.put("resolvedValue", answer);
                            }
                        }
                    }
                }
                sectionNode.put("orden", seccion.path("orden").asInt(Integer.MAX_VALUE));
            }

            sortSections(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("Error resolviendo plantilla: {}", e.getMessage());
            return templateJson;
        }
    }

    private void sortSections(JsonNode root) {
        JsonNode secciones = root.get("secciones");
        if (secciones == null || !secciones.isArray()) return;
        List<JsonNode> ordered = new java.util.ArrayList<>();
        secciones.forEach(ordered::add);
        ordered.sort(Comparator.comparingInt(node -> node.path("orden").asInt(Integer.MAX_VALUE)));
        ((ObjectNode) root).set("secciones", objectMapper.valueToTree(ordered));
    }

    private Map<String, Object> buildProjectValues(Proyecto proyecto) {
        if (proyecto == null) {
            return Map.of();
        }
        Map<String, Object> values = new HashMap<>();
        values.put("codigo_proyecto", proyecto.getId());
        values.put("nombre_proyecto", proyecto.getNombre());
        values.put("patrocinador", formatPatrocinador(proyecto.getPatrocinador()));
        values.put("patrocinador_nombre", proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : null);
        values.put("patrocinador_cargo", proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getCargo() : null);
        values.put("patrocinador_entidad", proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getEntidad() : null);
        values.put("director", proyecto.getDirector());
        values.put("director_nombre", proyecto.getDirector());
        values.put("fecha_inicio", formatDate(proyecto.getFechaInicio()));
        values.put("objetivo_general", proyecto.getObjetivoGeneral());
        values.put("objetivos_especificos", formatObjetivos(proyecto.getObjetivosEspecificos()));
        values.put("alcance_detallado", proyecto.getAlcanceDetallado());
        values.put("presupuesto_estimado", proyecto.getPresupuestoEstimado() != null ? proyecto.getPresupuestoEstimado().toPlainString() : null);
        values.put("avance_total", projectNumber(proyecto.getAvanceTotal()));
        values.put("estado", proyecto.getEstadoCodigo());
        values.put("fecha_cierre", formatDate(LocalDate.now()));
        values.put("duracion_meses", calculateDurationMonths(proyecto.getFechaInicio(), LocalDate.now()));
        return values;
    }

    private String resolveProjectValue(String fieldId, Proyecto proyecto) {
        if (proyecto == null || fieldId == null || fieldId.isBlank()) {
            return null;
        }
        return switch (fieldId) {
            case "codigo_proyecto" -> proyecto.getId();
            case "nombre_proyecto" -> proyecto.getNombre();
            case "patrocinador" -> formatPatrocinador(proyecto.getPatrocinador());
            case "patrocinador_nombre" -> proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : null;
            case "patrocinador_cargo" -> proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getCargo() : null;
            case "patrocinador_entidad" -> proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getEntidad() : null;
            case "director", "director_nombre" -> proyecto.getDirector();
            case "fecha_inicio" -> formatDate(proyecto.getFechaInicio());
            case "objetivo_general" -> proyecto.getObjetivoGeneral();
            case "objetivos_especificos" -> formatObjetivos(proyecto.getObjetivosEspecificos());
            case "alcance_detallado" -> proyecto.getAlcanceDetallado();
            case "presupuesto_estimado" -> proyecto.getPresupuestoEstimado() != null ? proyecto.getPresupuestoEstimado().toPlainString() : null;
            case "avance_total" -> projectNumber(proyecto.getAvanceTotal());
            case "estado" -> proyecto.getEstadoCodigo();
            case "fecha_cierre" -> formatDate(LocalDate.now());
            case "duracion_meses" -> calculateDurationMonths(proyecto.getFechaInicio(), LocalDate.now());
            default -> null;
        };
    }

    private String formatPatrocinador(Patrocinador patrocinador) {
        if (patrocinador == null) {
            return null;
        }
        return java.util.stream.Stream.of(patrocinador.getNombre(), patrocinador.getCargo(), patrocinador.getEntidad())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .reduce((a, b) -> a + " - " + b)
                .orElse(null);
    }

    private String formatObjetivos(List<ObjetivoEspecifico> objetivos) {
        if (objetivos == null || objetivos.isEmpty()) {
            return null;
        }
        String joined = objetivos.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ObjetivoEspecifico::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ObjetivoEspecifico::getDescripcion)
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + "\n- " + b)
                .orElse(null);
        return joined == null ? null : "- " + joined;
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.format(UI_DATE);
    }

    private String calculateDurationMonths(LocalDate fechaInicio, LocalDate fechaCierre) {
        if (fechaInicio == null || fechaCierre == null) {
            return null;
        }
        long months = java.time.temporal.ChronoUnit.MONTHS.between(fechaInicio.withDayOfMonth(1), fechaCierre.withDayOfMonth(1));
        return String.valueOf(Math.max(months, 0L));
    }

    private String projectNumber(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public Map<Long, String> extractQuestionIds(String templateJson) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return Map.of();

            var result = new java.util.HashMap<Long, String>();
            for (JsonNode seccion : secciones) {
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                        Long questionId = campo.get("questionId").asLong();
                        String label = campo.has("label") ? campo.get("label").asText() : "";
                        result.put(questionId, label);
                    }
                }
            }
            return result;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
