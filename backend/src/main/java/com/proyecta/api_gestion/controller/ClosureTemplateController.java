package com.proyecta.api_gestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateDTO;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateRequest;
import com.proyecta.api_gestion.dto.closure.ProjectClosureRecordDTO;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.closure.ProjectClosureRecordService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/closure-templates")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ClosureTemplateController {

    private final ClosureTemplateService templateService;
    private final ProjectClosureRecordService recordService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ObjectMapper objectMapper;

    public ClosureTemplateController(ClosureTemplateService templateService,
                                      ProjectClosureRecordService recordService,
                                      KeycloakIdentityExtractor identityExtractor,
                                      ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.recordService = recordService;
        this.identityExtractor = identityExtractor;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/active")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureTemplateDTO>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getActive(), "Plantilla activa"));
    }

    @PostMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureTemplateDTO>> save(
            @Valid @RequestBody ClosureTemplateRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        ClosureTemplateDTO dto = templateService.save(request, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Plantilla guardada exitosamente"));
    }

    @GetMapping("/closure-record/{projectId}")
    public ResponseEntity<ApiResponse<ProjectClosureRecordDTO>> getClosureRecord(@PathVariable String projectId) {
        ProjectClosureRecordDTO dto = recordService.getByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Registro de cierre"));
    }

    @PostMapping("/closure-record/{projectId}")
    public ResponseEntity<ApiResponse<ProjectClosureRecordDTO>> saveClosureRecord(
            @PathVariable String projectId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        String formData = body.getOrDefault("formData", "{}");
        ProjectClosureRecordDTO dto = recordService.saveOrUpdate(projectId, formData, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Datos de cierre guardados"));
    }
}
