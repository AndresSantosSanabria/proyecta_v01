package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.closure.ProjectClosureRecord;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureAnswerRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureTemplateRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ProjectClosureRecordRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClosureDraftService {

    private static final Logger log = LoggerFactory.getLogger(ClosureDraftService.class);
    private static final String JSON_QUESTION_ID = "questionId";

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final ClosureTemplateRepositoryPort templateRepositoryPort;
    private final ClosureAnswerRepositoryPort answerRepositoryPort;
    private final ProjectClosureRecordRepositoryPort closureRecordRepositoryPort;
    private final TemplateResolver templateResolver;
    private final ObjectMapper objectMapper;

    public ClosureDraftService(ProyectoRepositoryPort proyectoRepositoryPort,
                               ClosureTemplateRepositoryPort templateRepositoryPort,
                               ClosureAnswerRepositoryPort answerRepositoryPort,
                               ProjectClosureRecordRepositoryPort closureRecordRepositoryPort,
                               TemplateResolver templateResolver,
                               ObjectMapper objectMapper) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.templateRepositoryPort = templateRepositoryPort;
        this.answerRepositoryPort = answerRepositoryPort;
        this.closureRecordRepositoryPort = closureRecordRepositoryPort;
        this.templateResolver = templateResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String getResolvedTemplateJson(String projectId) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));
        var template = templateRepositoryPort.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));
        Map<Long, String> answerMap = loadAnswerMap(projectId, proyecto, template.getTemplateJson());

        return templateResolver.resolveTemplateForProject(template.getTemplateJson(), proyecto, answerMap);
    }

    private Map<Long, String> loadAnswerMap(String projectId, Proyecto proyecto, String templateJson) {
        Map<Long, String> answerMap = answerRepositoryPort.findByProyectoIdOrderByQuestionOrdenAsc(projectId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a.getRespuesta() == null ? "" : a.getRespuesta(),
                        (a, b) -> b
                ));

        boolean allBlank = answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank());
        if (allBlank) {
            Map<Long, String> fromRecord = buildAnswerMapFromClosureRecord(projectId, templateJson);
            if (!fromRecord.isEmpty()) {
                answerMap = fromRecord;
            }
        }

        if ((answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank()))
                && proyecto.getCierreBorradorJson() != null && !proyecto.getCierreBorradorJson().isBlank()) {
            Map<Long, String> fromBorrador = buildAnswerMapFromBorrador(proyecto.getCierreBorradorJson(), templateJson);
            if (!fromBorrador.isEmpty()) {
                answerMap = fromBorrador;
            }
        }
        return answerMap;
    }

    private Map<Long, String> buildAnswerMapFromClosureRecord(String projectId, String templateJson) {
        Map<Long, String> answerMap = new HashMap<>();
        try {
            ProjectClosureRecord closureRecord = closureRecordRepositoryPort.findByProyectoId(projectId).orElse(null);
            if (closureRecord == null) {
                return answerMap;
            }
            if (closureRecord.getFormData() == null || closureRecord.getFormData().isBlank()) {
                return answerMap;
            }
            JsonNode formDataNode = objectMapper.readTree(closureRecord.getFormData());
            JsonNode fieldsNode = formDataNode.get("fields");
            if (fieldsNode == null || !fieldsNode.isObject()) {
                return answerMap;
            }

            JsonNode template = objectMapper.readTree(templateJson);
            JsonNode secciones = template.get("secciones");
            if (secciones == null || !secciones.isArray()) return answerMap;

            for (JsonNode seccion : secciones) {
                collectClosureRecordAnswers(seccion, fieldsNode, answerMap);
            }
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear closure_record para proyecto {}: {}", projectId, e.getMessage());
        }
        return answerMap;
    }

    private void collectClosureRecordAnswers(JsonNode seccion, JsonNode fieldsNode, Map<Long, String> answerMap) {
        JsonNode campos = seccion.get("campos");
        if (campos == null || !campos.isArray()) return;
        for (JsonNode campo : campos) {
            if (!campo.has(JSON_QUESTION_ID) || campo.get(JSON_QUESTION_ID).isNull()) continue;
            Long questionId = campo.get(JSON_QUESTION_ID).asLong();
            String value = readFieldAnswer(fieldsNode, questionId);
            if (value != null && !value.isBlank()) {
                answerMap.put(questionId, value);
            }
        }
    }

    private String readFieldAnswer(JsonNode fieldsNode, Long questionId) {
        String qIdStr = String.valueOf(questionId);
        if (!fieldsNode.has(qIdStr) || fieldsNode.get(qIdStr).isNull()) {
            return null;
        }
        return fieldsNode.get(qIdStr).asText("");
    }

    private Map<Long, String> buildAnswerMapFromBorrador(String borradorJson, String templateJson) {
        Map<Long, String> answerMap = new HashMap<>();
        try {
            CierreProyectoRequest request = objectMapper.readValue(borradorJson, CierreProyectoRequest.class);
            JsonNode template = objectMapper.readTree(templateJson);
            JsonNode secciones = template.get("secciones");
            if (secciones == null || !secciones.isArray()) return answerMap;

            Map<String, String> fieldValues = buildBorradorFieldValues(request);

            for (JsonNode seccion : secciones) {
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    collectBorradorAnswer(campo, request, fieldValues, answerMap);
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear cierre_borrador_json para proyecto: {}", e.getMessage());
        }
        return answerMap;
    }

    private Map<String, String> buildBorradorFieldValues(CierreProyectoRequest request) {
        Map<String, String> fieldValues = new HashMap<>();
        if (request.resumenEjecutivo() != null) fieldValues.put("resumen_ejecutivo", request.resumenEjecutivo());
        if (request.leccionesPositivas() != null) fieldValues.put("aspectos_positivos", request.leccionesPositivas());
        if (request.leccionesMejorar() != null) fieldValues.put("aspectos_mejorar", request.leccionesMejorar());
        if (request.recomendaciones() != null) fieldValues.put("recomendaciones", request.recomendaciones());
        if (request.transferenciaActividad() != null) fieldValues.put("transferencia_actividad", request.transferenciaActividad());
        if (request.fechaCierre() != null) fieldValues.put("fecha_cierre", request.fechaCierre().toString());
        return fieldValues;
    }

    private void collectBorradorAnswer(JsonNode campo, CierreProyectoRequest request, Map<String, String> fieldValues, Map<Long, String> answerMap) {
        if (!campo.has(JSON_QUESTION_ID) || campo.get(JSON_QUESTION_ID).isNull()) return;
        Long questionId = campo.get(JSON_QUESTION_ID).asLong();
        String fieldId = campo.path("id").asText("");
        String value = fieldValues.getOrDefault(fieldId, null);
        if (value == null || value.isBlank()) {
            value = resolveBorradorFieldValue(request, questionId);
        }
        if (value != null && !value.isBlank()) {
            answerMap.put(questionId, value);
        }
    }

    private String resolveBorradorFieldValue(CierreProyectoRequest request, Long questionId) {
        if (request.formData() == null || request.formData().isBlank()) {
            return null;
        }
        return readFormFieldValue(request.formData(), questionId);
    }

    @Transactional(readOnly = true)
    public List<ClosureQuestionDTO> getMissingQuestions(String projectId) {
        Proyecto proyecto = proyectoRepositoryPort.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));
        var template = templateRepositoryPort.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));
        Map<Long, String> answerMap = loadAnswerMap(projectId, proyecto, template.getTemplateJson());

        Map<Long, String> missing = templateResolver.extractMissingQuestions(template.getTemplateJson(), proyecto, answerMap);
        return missing.entrySet().stream()
                .map(entry -> new ClosureQuestionDTO(
                        entry.getKey(),
                        entry.getValue(),
                        "texto_libre",
                        null,
                        true,
                        0,
                        null,
                        null,
                        null,
                        null))
                .toList();
    }

    private String readFormFieldValue(String formDataJson, Long questionId) {
        try {
            JsonNode fdNode = objectMapper.readTree(formDataJson);
            JsonNode fdFields = fdNode.get("fields");
            if (fdFields != null && fdFields.isObject()) {
                String qIdStr = String.valueOf(questionId);
                if (fdFields.has(qIdStr) && !fdFields.get(qIdStr).isNull()) {
                    return fdFields.get(qIdStr).asText("");
                }
            }
        } catch (Exception ex) {
            // CWE-390: el campo del borrador no era parseable; se omite sin romper la carga.
            log.debug("No se pudo leer el campo '{}' del borrador de cierre: {}", questionId, ex.toString());
        }
        return null;
    }
}
