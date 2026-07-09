package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateResolver {

    private static final Logger log = LoggerFactory.getLogger(TemplateResolver.class);
    private final ObjectMapper objectMapper;

    public TemplateResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String resolveTemplateWithAnswers(String templateJson, Map<Long, String> answerMap) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray()) return templateJson;

            for (JsonNode seccion : secciones) {
                String tipo = seccion.has("tipo_seccion") ? seccion.get("tipo_seccion").asText() : "formulario";
                if ("formulario".equals(tipo)) {
                    JsonNode campos = seccion.get("campos");
                    if (campos == null || !campos.isArray()) continue;
                    for (JsonNode campo : campos) {
                        if (campo.has("questionId") && !campo.get("questionId").isNull()) {
                            Long questionId = campo.get("questionId").asLong();
                            String answer = answerMap.getOrDefault(questionId, "");
                            if (answer != null && !answer.isBlank()) {
                                ((ObjectNode) campo).put("resolvedValue", answer);
                            } else {
                                ((ObjectNode) campo).put("resolvedValue", "");
                                ((ObjectNode) campo).put("isEmpty", true);
                            }
                        }
                    }
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("Error resolviendo plantilla: {}", e.getMessage());
            return templateJson;
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
