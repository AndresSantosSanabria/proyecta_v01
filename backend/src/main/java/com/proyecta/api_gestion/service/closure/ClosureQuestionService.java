package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ClosureAnswerDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionRequest;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.closure.ClosureAnswer;
import com.proyecta.api_gestion.model.closure.ClosureQuestion;
import com.proyecta.api_gestion.repository.closure.ClosureAnswerRepository;
import com.proyecta.api_gestion.repository.closure.ClosureQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClosureQuestionService {

    private final ClosureQuestionRepository questionRepository;
    private final ClosureAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    public ClosureQuestionService(ClosureQuestionRepository questionRepository,
                                   ClosureAnswerRepository answerRepository,
                                   ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ClosureQuestionDTO> listAll() {
        return questionRepository.findAllByOrderByOrdenAsc().stream()
                .map(this::toQuestionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClosureQuestionDTO> listActive() {
        return questionRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(this::toQuestionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClosureQuestionDTO getById(Long id) {
        ClosureQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + id));
        return toQuestionDTO(q);
    }

    @Transactional
    public ClosureQuestionDTO create(ClosureQuestionRequest request, String username) {
        ClosureQuestion q = new ClosureQuestion();
        q.setTexto(request.texto());
        q.setTipoRespuesta(request.tipoRespuesta() != null ? request.tipoRespuesta() : "texto_libre");
        q.setOpciones(serializeOpciones(request.opciones()));
        q.setActivo(request.activo() != null ? request.activo() : true);
        q.setOrden(request.orden() != null ? request.orden() : getNextOrden());
        q.setCreatedBy(username);
        q.setUpdatedBy(username);
        return toQuestionDTO(questionRepository.save(q));
    }

    @Transactional
    public ClosureQuestionDTO update(Long id, ClosureQuestionRequest request, String username) {
        ClosureQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + id));
        q.setTexto(request.texto());
        if (request.tipoRespuesta() != null) q.setTipoRespuesta(request.tipoRespuesta());
        q.setOpciones(serializeOpciones(request.opciones()));
        if (request.activo() != null) q.setActivo(request.activo());
        if (request.orden() != null) q.setOrden(request.orden());
        q.setUpdatedBy(username);
        return toQuestionDTO(questionRepository.save(q));
    }

    @Transactional
    public void toggleActivo(Long id, String username) {
        ClosureQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + id));
        q.setActivo(!Boolean.TRUE.equals(q.getActivo()));
        q.setUpdatedBy(username);
        questionRepository.save(q);
    }

    @Transactional
    public void delete(Long id) {
        ClosureQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + id));
        if (answerRepository.existsByQuestionId(id)) {
            throw new BadRequestException("No se puede eliminar una pregunta que tiene respuestas registradas. Desactivala en su lugar.");
        }
        questionRepository.delete(q);
    }

    @Transactional(readOnly = true)
    public List<ClosureAnswerDTO> getAnswersByProject(String projectId) {
        return answerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                .map(this::toAnswerDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getAnswersMapByProject(String projectId) {
        return answerRepository.findByProyectoIdOrderByQuestion_OrdenAsc(projectId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a.getRespuesta() != null ? a.getRespuesta() : "",
                        (a, b) -> b
                ));
    }

    @Transactional
    public void saveAnswers(String projectId, List<ClosureAnswerRequest> answers) {
        for (ClosureAnswerRequest ans : answers) {
            ClosureQuestion q = questionRepository.findById(ans.questionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + ans.questionId()));
            ClosureAnswer existing = answerRepository.findByProyectoIdAndQuestionId(projectId, ans.questionId()).orElse(null);
            if (existing != null) {
                existing.setRespuesta(ans.respuesta());
            } else {
                ClosureAnswer answer = new ClosureAnswer();
                answer.setProyectoId(projectId);
                answer.setQuestion(q);
                answer.setRespuesta(ans.respuesta());
                answerRepository.save(answer);
            }
        }
    }

    private int getNextOrden() {
        List<ClosureQuestion> all = questionRepository.findAllByOrderByOrdenAsc();
        return all.isEmpty() ? 1 : all.get(all.size() - 1).getOrden() + 1;
    }

    private String serializeOpciones(Object opciones) {
        if (opciones == null) return null;
        if (opciones instanceof String s) {
            try {
                objectMapper.readTree(s);
                return s;
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Las opciones no son JSON valido.");
            }
        }
        try {
            return objectMapper.writeValueAsString(opciones);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("No fue posible serializar las opciones.");
        }
    }

    private ClosureQuestionDTO toQuestionDTO(ClosureQuestion q) {
        Object ops = null;
        if (q.getOpciones() != null && !q.getOpciones().isBlank()) {
            try {
                ops = objectMapper.readValue(q.getOpciones(), Object.class);
            } catch (JsonProcessingException e) {
                ops = q.getOpciones();
            }
        }
        return new ClosureQuestionDTO(
                q.getId(), q.getTexto(), q.getTipoRespuesta(), ops,
                q.getActivo(), q.getOrden(),
                q.getCreatedAt(), q.getUpdatedAt(),
                q.getCreatedBy(), q.getUpdatedBy()
        );
    }

    private ClosureAnswerDTO toAnswerDTO(ClosureAnswer a) {
        return new ClosureAnswerDTO(
                a.getId(), a.getProyectoId(),
                a.getQuestion().getId(), a.getQuestion().getTexto(),
                a.getRespuesta(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }

    public record ClosureAnswerRequest(Long questionId, String respuesta) {}
}
