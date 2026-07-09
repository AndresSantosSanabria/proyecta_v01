package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.closure.ClosureAnswerDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionRequest;
import com.proyecta.api_gestion.service.closure.ClosureQuestionService;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/closure-questions")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ClosureQuestionController {

    private final ClosureQuestionService service;
    private final ClosureTemplateService templateService;
    private final KeycloakIdentityExtractor identityExtractor;

    public ClosureQuestionController(ClosureQuestionService service,
                                      ClosureTemplateService templateService,
                                      KeycloakIdentityExtractor identityExtractor) {
        this.service = service;
        this.templateService = templateService;
        this.identityExtractor = identityExtractor;
    }

    @GetMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<List<ClosureQuestionDTO>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(service.listAll(), "Preguntas listadas"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ClosureQuestionDTO>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(service.listActive(), "Preguntas activas"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureQuestionDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id), "Pregunta obtenida"));
    }

    @PostMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureQuestionDTO>> create(
            @Valid @RequestBody ClosureQuestionRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.create(request, username), "Pregunta creada"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureQuestionDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ClosureQuestionRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request, username), "Pregunta actualizada"));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> toggleActivo(
            @PathVariable Long id,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        service.toggleActivo(id, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Estado actualizado"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Pregunta eliminada"));
    }

    @GetMapping("/answers/{projectId}")
    public ResponseEntity<ApiResponse<List<ClosureAnswerDTO>>> getAnswers(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAnswersByProject(projectId), "Respuestas obtenidas"));
    }

    @PostMapping("/answers/{projectId}")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @PathVariable String projectId,
            @RequestBody List<Map<String, Object>> body,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        List<ClosureQuestionService.ClosureAnswerRequest> answers = body.stream()
                .map(m -> new ClosureQuestionService.ClosureAnswerRequest(
                        Long.valueOf(m.get("questionId").toString()),
                        m.get("respuesta") != null ? m.get("respuesta").toString() : null))
                .toList();
        service.saveAnswers(projectId, answers);
        return ResponseEntity.ok(ApiResponse.success(null, "Respuestas guardadas"));
    }

    @GetMapping("/resolved-template/{projectId}")
    public ResponseEntity<ApiResponse<String>> getResolvedTemplate(@PathVariable String projectId) {
        Map<Long, String> answerMap = service.getAnswersMapByProject(projectId);
        String resolved = templateService.getActiveTemplateJsonResolved(answerMap);
        return ResponseEntity.ok(ApiResponse.success(resolved, "Plantilla resuelta"));
    }
}
