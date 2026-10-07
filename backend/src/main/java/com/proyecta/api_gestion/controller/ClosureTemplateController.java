package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateDTO;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateRequest;
import com.proyecta.api_gestion.dto.closure.ProjectClosureRecordDTO;
import com.proyecta.api_gestion.service.closure.ClosureTemplateService;
import com.proyecta.api_gestion.service.closure.ProjectClosureRecordService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/closure-templates")
@Tag(name = "Administración - Plantillas de Cierre", description = "Endpoints de plantilla, acta y registro formal de cierre de proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ClosureTemplateController {

    private final ClosureTemplateService templateService;
    private final ProjectClosureRecordService recordService;
    private final KeycloakIdentityExtractor identityExtractor;

    public ClosureTemplateController(ClosureTemplateService templateService,
                                      ProjectClosureRecordService recordService,
                                      KeycloakIdentityExtractor identityExtractor) {
        this.templateService = templateService;
        this.recordService = recordService;
        this.identityExtractor = identityExtractor;
    }

    @Operation(summary = "Obtener la plantilla de cierre activa")
    @GetMapping("/active")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureTemplateDTO>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getActive(), "Plantilla activa"));
    }

    @Operation(summary = "Guardar una nueva versión de la plantilla de cierre", description = "Crea la plantilla con el siguiente número de versión y la activa, desactivando las anteriores.")
    @PostMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<ClosureTemplateDTO>> save(
            @Valid @RequestBody ClosureTemplateRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        ClosureTemplateDTO dto = templateService.save(request, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Plantilla guardada exitosamente"));
    }

    // CWE-862: registro de cierre por proyecto; lectura -> PROYECTO:VER,
    // escritura -> CIERRE:SOLICITAR (mismo flujo que las respuestas de cierre).
    @Operation(summary = "Obtener el registro formal de cierre de un proyecto")
    @GetMapping("/closure-record/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<ProjectClosureRecordDTO>> getClosureRecord(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId) {
        ProjectClosureRecordDTO dto = recordService.getByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Registro de cierre"));
    }

    @Operation(summary = "Guardar o actualizar el registro formal de cierre de un proyecto", description = "El campo formData (JSON con los datos diligenciados) se almacena en bruto; usa el formato de la plantilla activa.")
    @PostMapping("/closure-record/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:SOLICITAR', #projectId, authentication)")
    public ResponseEntity<ApiResponse<ProjectClosureRecordDTO>> saveClosureRecord(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        String formData = body.getOrDefault("formData", "{}");
        ProjectClosureRecordDTO dto = recordService.saveOrUpdate(projectId, formData, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Datos de cierre guardados"));
    }
}
