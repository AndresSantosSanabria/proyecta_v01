package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.closure.ProjectClosureRecord;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.closure.ClosureAnswerRepository;
import com.proyecta.api_gestion.repository.closure.ClosureTemplateRepository;
import com.proyecta.api_gestion.repository.closure.ProjectClosureRecordRepository;
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

    private final ProyectoRepository proyectoRepository;
    private final ClosureTemplateRepository templateRepository;
    private final ClosureAnswerRepository answerRepository;
    private final ProjectClosureRecordRepository closureRecordRepository;
    private final TemplateResolver templateResolver;
    private final ObjectMapper objectMapper;

    public ClosureDraftService(ProyectoRepository proyectoRepository,
                               ClosureTemplateRepository templateRepository,
                               ClosureAnswerRepository answerRepository,
                               ProjectClosureRecordRepository closureRecordRepository,
                               TemplateResolver templateResolver,
                               ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
        this.closureRecordRepository = closureRecordRepository;
        this.templateResolver = templateResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String getResolvedTemplateJson(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));
        var template = templateRepository.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));
        Map<Long, String> answerMap = answerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a.getRespuesta() == null ? "" : a.getRespuesta(),
                        (a, b) -> b
                ));

        boolean allBlank = answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank());
        if (allBlank) {
            Map<Long, String> fromRecord = buildAnswerMapFromClosureRecord(projectId, template.getTemplateJson());
            if (!fromRecord.isEmpty()) {
                answerMap = fromRecord;
            }
        }

        if (answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank())) {
            if (proyecto.getCierreBorradorJson() != null && !proyecto.getCierreBorradorJson().isBlank()) {
                Map<Long, String> fromBorrador = buildAnswerMapFromBorrador(proyecto.getCierreBorradorJson(), template.getTemplateJson());
                if (!fromBorrador.isEmpty()) {
                    answerMap = fromBorrador;
                }
            }
        }

        return templateResolver.resolveTemplateForProject(template.getTemplateJson(), proyecto, answerMap);
    }

    private Map<Long, String> buildAnswerMapFromClosureRecord(String projectId, String templateJson) {
        Map<Long, String> answerMap = new HashMap<>();
        try {
            ProjectClosureRecord record = closureRecordRepository.findByProyectoId(projectId).orElse(null);
            if (record == null || record.getFormData() == null || record.getFormData().isBlank()) {
                return answerMap;
            }
            JsonNode formDataNode = objectMapper.readTree(record.getFormData());
            JsonNode fieldsNode = formDataNode.get("fields");
            if (fieldsNode == null || !fieldsNode.isObject()) {
                return answerMap;
            }

            JsonNode template = objectMapper.readTree(templateJson);
            JsonNode secciones = template.get("secciones");
            if (secciones == null || !secciones.isArray()) return answerMap;

            for (JsonNode seccion : secciones) {
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    if (!campo.has("questionId") || campo.get("questionId").isNull()) continue;
                    Long questionId = campo.get("questionId").asLong();
                    String qIdStr = String.valueOf(questionId);
                    if (fieldsNode.has(qIdStr) && !fieldsNode.get(qIdStr).isNull()) {
                        String value = fieldsNode.get(qIdStr).asText("");
                        if (value != null && !value.isBlank()) {
                            answerMap.put(questionId, value);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear closure_record para proyecto {}: {}", projectId, e.getMessage());
        }
        return answerMap;
    }

    private Map<Long, String> buildAnswerMapFromBorrador(String borradorJson, String templateJson) {
        Map<Long, String> answerMap = new HashMap<>();
        try {
            CierreProyectoRequest request = objectMapper.readValue(borradorJson, CierreProyectoRequest.class);
            JsonNode template = objectMapper.readTree(templateJson);
            JsonNode secciones = template.get("secciones");
            if (secciones == null || !secciones.isArray()) return answerMap;

            Map<String, String> fieldValues = new HashMap<>();
            if (request.resumenEjecutivo() != null) fieldValues.put("resumen_ejecutivo", request.resumenEjecutivo());
            if (request.leccionesPositivas() != null) fieldValues.put("aspectos_positivos", request.leccionesPositivas());
            if (request.leccionesMejorar() != null) fieldValues.put("aspectos_mejorar", request.leccionesMejorar());
            if (request.recomendaciones() != null) fieldValues.put("recomendaciones", request.recomendaciones());
            if (request.transferenciaActividad() != null) fieldValues.put("transferencia_actividad", request.transferenciaActividad());
            if (request.fechaCierre() != null) fieldValues.put("fecha_cierre", request.fechaCierre().toString());

            for (JsonNode seccion : secciones) {
                JsonNode campos = seccion.get("campos");
                if (campos == null || !campos.isArray()) continue;
                for (JsonNode campo : campos) {
                    if (!campo.has("questionId") || campo.get("questionId").isNull()) continue;
                    Long questionId = campo.get("questionId").asLong();
                    String fieldId = campo.path("id").asText("");
                    String value = fieldValues.getOrDefault(fieldId, null);
                    if (value == null || value.isBlank()) {
                        if (request.formData() != null && !request.formData().isBlank()) {
                            try {
                                JsonNode fdNode = objectMapper.readTree(request.formData());
                                JsonNode fdFields = fdNode.get("fields");
                                if (fdFields != null && fdFields.isObject()) {
                                    String qIdStr = String.valueOf(questionId);
                                    if (fdFields.has(qIdStr) && !fdFields.get(qIdStr).isNull()) {
                                        value = fdFields.get(qIdStr).asText("");
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (value != null && !value.isBlank()) {
                        answerMap.put(questionId, value);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear cierre_borrador_json para proyecto: {}", e.getMessage());
        }
        return answerMap;
    }

    @Transactional(readOnly = true)
    public List<ClosureQuestionDTO> getMissingQuestions(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));
        var template = templateRepository.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));
        Map<Long, String> answerMap = answerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a.getRespuesta() == null ? "" : a.getRespuesta(),
                        (a, b) -> b
                ));

        boolean allBlank = answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank());
        if (allBlank) {
            Map<Long, String> fromRecord = buildAnswerMapFromClosureRecord(projectId, template.getTemplateJson());
            if (!fromRecord.isEmpty()) {
                answerMap = fromRecord;
            }
        }

        if (answerMap.isEmpty() || answerMap.values().stream().allMatch(v -> v == null || v.isBlank())) {
            if (proyecto.getCierreBorradorJson() != null && !proyecto.getCierreBorradorJson().isBlank()) {
                Map<Long, String> fromBorrador = buildAnswerMapFromBorrador(proyecto.getCierreBorradorJson(), template.getTemplateJson());
                if (!fromBorrador.isEmpty()) {
                    answerMap = fromBorrador;
                }
            }
        }

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
}
