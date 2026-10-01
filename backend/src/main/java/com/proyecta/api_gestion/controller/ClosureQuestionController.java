package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.closure.ClosureAnswerDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionDTO;
import com.proyecta.api_gestion.dto.closure.ClosureQuestionRequest;
import com.proyecta.api_gestion.service.closure.ClosureDraftService;
import com.proyecta.api_gestion.service.closure.ClosureQuestionService;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/closure-questions")
@Tag(name = "Administración - Preguntas de Cierre", description = "Endpoints CRUD de preguntas del checklist de cierre de proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ClosureQuestionController {

    private final ClosureQuestionService service;
    private final ClosureTemplateService templateService;
    private final ClosureDraftService draftService;
    private final KeycloakIdentityExtractor identityExtractor;

    public ClosureQuestionController(ClosureQuestionService service,
                                      ClosureTemplateService templateService,
                                      ClosureDraftService draftService,
                                      KeycloakIdentityExtractor identityExtractor) {
        this.service = service;
        this.templateService = templateService;
        this.draftService = draftService;
        this.identityExtractor = identityExtractor;
    }

    @Operation(summary = "Listar todas las preguntas de cierre")
    @GetMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<List<ClosureQuestionDTO>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(service.listAll(), "Preguntas listadas"));
    }

    @Operation(summary = "Listar las preguntas de cierre activas")
    @GetMapping("/active")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<List<ClosureQuestionDTO>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(service.listActive(), "Preguntas activas"));
    }

    @Operation(summary = "Crear una pregunta de cierre")
    @PostMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureQuestionDTO>> create(
            @Valid @RequestBody ClosureQuestionRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.create(request, username), "Pregunta creada"));
    }

    @Operation(summary = "Actualizar una pregunta de cierre")
    @PatchMapping("/{id}")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureQuestionDTO>> update(
            @Parameter(description = "Identificador de la pregunta") @PathVariable Long id,
            @Valid @RequestBody ClosureQuestionRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request, username), "Pregunta actualizada"));
    }

    @Operation(summary = "Alternar el estado activo de una pregunta de cierre")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> toggleActivo(
            @Parameter(description = "Identificador de la pregunta") @PathVariable Long id,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        service.toggleActivo(id, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Estado actualizado"));
    }

    @Operation(summary = "Eliminar una pregunta de cierre")
    @DeleteMapping("/{id}")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Identificador de la pregunta") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Pregunta eliminada"));
    }

    // CWE-862: los endpoints con {projectId} exigen pertenencia al proyecto.
    // Lecturas -> PROYECTO:VER; escritura de respuestas -> CIERRE:SOLICITAR
    // (el Director diligencia el cierre; los Gestores tambien lo poseen).
    @Operation(summary = "Listar las respuestas de cierre de un proyecto")
    @GetMapping("/answers/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<List<ClosureAnswerDTO>>> getAnswers(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAnswersByProject(projectId), "Respuestas obtenidas"));
    }

    @Operation(summary = "Guardar las respuestas del checklist de cierre", description = "Crea o actualiza cada respuesta enviada (questionId y respuesta); no elimina las respuestas no incluidas. Rol con permiso CIERRE:SOLICITAR.")
    @PostMapping("/answers/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:SOLICITAR', #projectId, authentication)")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @RequestBody List<Map<String, Object>> body,
            Authentication authentication) {
        List<ClosureQuestionService.ClosureAnswerRequest> answers = body.stream()
                .map(m -> new ClosureQuestionService.ClosureAnswerRequest(
                        Long.valueOf(m.get("questionId").toString()),
                        m.get("respuesta") != null ? m.get("respuesta").toString() : null))
                .toList();
        service.saveAnswers(projectId, answers);
        return ResponseEntity.ok(ApiResponse.success(null, "Respuestas guardadas"));
    }

    @Operation(summary = "Obtener la plantilla de cierre resuelta con las respuestas", description = "Sustituye los marcadores de la plantilla activa por las respuestas registradas del proyecto.")
    @GetMapping("/resolved-template/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> getResolvedTemplate(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId) {
        Map<Long, String> answerMap = service.getAnswersMapByProject(projectId);
        String resolved = templateService.getActiveTemplateJsonResolved(answerMap);
        return ResponseEntity.ok(ApiResponse.success(resolved, "Plantilla resuelta"));
    }

    @Operation(summary = "Obtener el borrador de plantilla de cierre del proyecto", description = "Resuelve la plantilla con las respuestas existentes, el registro de cierre o el borrador guardado del proyecto.")
    @GetMapping("/draft/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> getDraftTemplate(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                draftService.getResolvedTemplateJson(projectId),
                "Plantilla de cierre resuelta con datos existentes"));
    }

    @Operation(summary = "Listar las preguntas de cierre faltantes de un proyecto")
    @GetMapping("/missing/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<List<ClosureQuestionDTO>>> getMissingQuestions(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                draftService.getMissingQuestions(projectId),
                "Preguntas faltantes"));
    }
}
