package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proyecta.api_gestion.domain.model.Patrocinador;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.ObjetivoEspecifico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final String SECCIONES_KEY = "secciones";
    private static final String TIPO_FORMULARIO = "formulario";
    private static final String TIPO_SECCION_KEY = "tipo_seccion";
    private static final String QUESTION_ID_KEY = "questionId";
    private static final String ORDEN_KEY = "orden";
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

    public Map<Long, String> extractMissingQuestions(String templateJson, Proyecto proyecto, Map<Long, String> answerMap) {
        if (proyecto == null) return Map.of();
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get(SECCIONES_KEY);
            if (secciones == null || !secciones.isArray()) return Map.of();

            Map<Long, String> missing = new HashMap<>();
            for (JsonNode seccion : secciones) {
                collectMissingFromSeccion(seccion, proyecto, answerMap, missing);
            }
            return missing;
        } catch (JsonProcessingException _) {
            return Map.of();
        }
    }

    private void collectMissingFromSeccion(JsonNode seccion, Proyecto proyecto, Map<Long, String> answerMap, Map<Long, String> missing) {
        boolean esFormulario = TIPO_FORMULARIO.equals(seccion.path(TIPO_SECCION_KEY).asText(TIPO_FORMULARIO));
        JsonNode campos = seccion.get("campos");
        if (!esFormulario || campos == null || !campos.isArray()) return;
        for (JsonNode campo : campos) {
            collectMissingFromCampo(campo, proyecto, answerMap, missing);
        }
    }

    private void collectMissingFromCampo(JsonNode campo, Proyecto proyecto, Map<Long, String> answerMap, Map<Long, String> missing) {
        if (!campo.has(QUESTION_ID_KEY) || campo.get(QUESTION_ID_KEY).isNull()) return;
        Long questionId = campo.get(QUESTION_ID_KEY).asLong();
        if (isAnswered(questionId, answerMap)) return;
        String fieldId = campo.path("id").asText("");
        String projectValue = resolveProjectValue(fieldId, proyecto);
        if (isAvailableInProject(fieldId, projectValue)) return;
        missing.put(questionId, campo.path("label").asText(""));
    }

    private boolean isAnswered(Long questionId, Map<Long, String> answerMap) {
        return answerMap.containsKey(questionId)
                && answerMap.get(questionId) != null
                && !answerMap.get(questionId).isBlank();
    }

    private boolean isAvailableInProject(String fieldId, String projectValue) {
        return !fieldId.isBlank() && projectValue != null && !projectValue.isBlank();
    }

    private String resolveTemplate(String templateJson, Map<String, Object> projectValues, Map<Long, String> answerMap) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get(SECCIONES_KEY);
            if (secciones == null || !secciones.isArray()) return templateJson;

            for (JsonNode seccion : secciones) {
                processSeccion(seccion, projectValues, answerMap);
            }

            sortSections(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("Error resolviendo plantilla: {}", e.getMessage());
            return templateJson;
        }
    }

    private void processSeccion(JsonNode seccion, Map<String, Object> projectValues, Map<Long, String> answerMap) {
        if (!seccion.isObject()) return;
        ObjectNode sectionNode = (ObjectNode) seccion;
        String tipo = seccion.has(TIPO_SECCION_KEY) ? seccion.get(TIPO_SECCION_KEY).asText() : TIPO_FORMULARIO;
        boolean esProcesable = TIPO_FORMULARIO.equals(tipo) || "tabla".equals(tipo);
        if (!esProcesable) return;
        boolean camposValidos = processCampos(seccion, tipo, projectValues, answerMap);
        if (camposValidos) {
            sectionNode.put(ORDEN_KEY, seccion.path(ORDEN_KEY).asInt(Integer.MAX_VALUE));
        }
    }

    private boolean processCampos(JsonNode seccion, String tipo, Map<String, Object> projectValues, Map<Long, String> answerMap) {
        if (!TIPO_FORMULARIO.equals(tipo)) return true;
        JsonNode campos = seccion.get("campos");
        if (campos == null || !campos.isArray()) return false;
        for (JsonNode campo : campos) {
            processCampo(campo, projectValues, answerMap);
        }
        return true;
    }

    private void processCampo(JsonNode campo, Map<String, Object> projectValues, Map<Long, String> answerMap) {
        if (!campo.isObject()) return;
        ObjectNode fieldNode = (ObjectNode) campo;
        String fieldId = campo.path("id").asText("");
        applyProjectValue(fieldNode, fieldId, projectValues);
        applyAnswer(campo, fieldNode, answerMap);
    }

    private void applyProjectValue(ObjectNode fieldNode, String fieldId, Map<String, Object> projectValues) {
        if (fieldId.isBlank() || !projectValues.containsKey(fieldId)) return;
        Object value = projectValues.get(fieldId);
        if (value == null || String.valueOf(value).isBlank()) return;
        fieldNode.put("resolvedValue", String.valueOf(value));
        fieldNode.put("readonly", true);
    }

    private void applyAnswer(JsonNode campo, ObjectNode fieldNode, Map<Long, String> answerMap) {
        if (!campo.has(QUESTION_ID_KEY) || campo.get(QUESTION_ID_KEY).isNull()) return;
        Long questionId = campo.get(QUESTION_ID_KEY).asLong();
        String answer = answerMap.getOrDefault(questionId, "");
        if (answer == null || answer.isBlank()) return;
        fieldNode.put("resolvedValue", answer);
    }

    private void sortSections(JsonNode root) {
        JsonNode secciones = root.get(SECCIONES_KEY);
        if (secciones == null || !secciones.isArray()) return;
        List<JsonNode> ordered = new java.util.ArrayList<>();
        secciones.forEach(ordered::add);
        ordered.sort(Comparator.comparingInt(node -> node.path(ORDEN_KEY).asInt(Integer.MAX_VALUE)));
        ((ObjectNode) root).set(SECCIONES_KEY, objectMapper.valueToTree(ordered));
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
        values.put("fecha_cierre", formatDate(LocalDate.now(ZoneId.systemDefault())));
        values.put("duracion_meses", calculateDurationMonths(proyecto.getFechaInicio(), LocalDate.now(ZoneId.systemDefault())));
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
            case "fecha_cierre" -> formatDate(LocalDate.now(ZoneId.systemDefault()));
            case "duracion_meses" -> calculateDurationMonths(proyecto.getFechaInicio(), LocalDate.now(ZoneId.systemDefault()));
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
}
