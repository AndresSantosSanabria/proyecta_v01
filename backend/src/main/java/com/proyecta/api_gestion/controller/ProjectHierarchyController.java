package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectHierarchyController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import com.proyecta.api_gestion.service.interfaces.ProjectHierarchyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectHierarchyController implements IProjectHierarchyController {

    private final ProjectHierarchyService projectHierarchyService;

    public ProjectHierarchyController(ProjectHierarchyService projectHierarchyService) {
        this.projectHierarchyService = projectHierarchyService;
    }

    @Override
    @GetMapping("/{id}/hierarchy")
    public ResponseEntity<ApiResponse<ProjectHierarchyDTO>> getProjectHierarchy(@PathVariable String id) {
        ProjectHierarchyDTO hierarchy = projectHierarchyService.getProjectHierarchy(id);
        return ResponseEntity.ok(ApiResponse.success(hierarchy, "Jerarquía obtenida con éxito"));
    }
}
