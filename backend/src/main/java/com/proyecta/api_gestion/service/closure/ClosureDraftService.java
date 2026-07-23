package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.closure.ClosureAnswerRepository;
import com.proyecta.api_gestion.repository.closure.ClosureTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ClosureDraftService {

    private final ProyectoRepository proyectoRepository;
    private final ClosureTemplateRepository templateRepository;
    private final ClosureAnswerRepository answerRepository;
    private final TemplateResolver templateResolver;
    private final ObjectMapper objectMapper;

    public ClosureDraftService(ProyectoRepository proyectoRepository,
                               ClosureTemplateRepository templateRepository,
                               ClosureAnswerRepository answerRepository,
                               TemplateResolver templateResolver,
                               ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
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
        return templateResolver.resolveTemplateForProject(template.getTemplateJson(), proyecto, answerMap);
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
