package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectHierarchyController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import com.proyecta.api_gestion.service.interfaces.ProjectHierarchyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
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

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> agregarFase(String id, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto) {
        com.proyecta.api_gestion.model.Fase fase = projectHierarchyService.agregarFase(id, dto);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO(fase.getId(), fase.getNombre(), fase.getDescripcion(), fase.getPonderacion(), fase.getAvanceCalculado(), null), "Fase agregada"));
    }

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> editarFase(String id, Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto) {
        com.proyecta.api_gestion.model.Fase fase = projectHierarchyService.editarFase(faseId, dto);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO(fase.getId(), fase.getNombre(), fase.getDescripcion(), fase.getPonderacion(), fase.getAvanceCalculado(), null), "Fase editada"));
    }

    @Override
    public ResponseEntity<Void> eliminarFase(String id, Integer faseId) {
        projectHierarchyService.eliminarFase(faseId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> agregarHito(String id, Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        com.proyecta.api_gestion.model.Hito hito = projectHierarchyService.agregarHito(faseId, dto);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO(hito.getId(), hito.getNombre(), hito.getDescripcion(), hito.getPonderacion(), hito.getAvanceCalculado(), null), "Hito agregado"));
    }

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> editarHito(String id, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto) {
        com.proyecta.api_gestion.model.Hito hito = projectHierarchyService.editarHito(hitoId, dto);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO(hito.getId(), hito.getNombre(), hito.getDescripcion(), hito.getPonderacion(), hito.getAvanceCalculado(), null), "Hito editado"));
    }

    @Override
    public ResponseEntity<Void> eliminarHito(String id, Integer faseId, Integer hitoId) {
        projectHierarchyService.eliminarHito(hitoId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApiResponse<java.util.List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO>>> listarEntregablesProyecto(String id) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.listarEntregablesProyecto(id), "Lista de entregables obtenida"));
    }

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> agregarEntregable(String id, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.agregarEntregable(hitoId, dto), "Entregable agregado"));
    }

    @Override
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> editarEntregable(String id, Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.editarEntregable(entregableId, dto), "Entregable editado"));
    }

    @Override
    public ResponseEntity<Void> eliminarEntregable(String id, Integer entregableId) {
        projectHierarchyService.eliminarEntregable(entregableId);
        return ResponseEntity.noContent().build();
    }
}
