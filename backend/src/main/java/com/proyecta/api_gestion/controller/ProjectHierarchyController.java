package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectHierarchyController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaRequest;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaResponse;
import com.proyecta.api_gestion.model.EntregableCambioFecha;
import com.proyecta.api_gestion.repository.EntregableCambioFechaRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProjectHierarchyService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectHierarchyController implements IProjectHierarchyController {

    private final ProjectHierarchyService projectHierarchyService;
    private final EntregableCambioFechaRepository cambioFechaRepository;
    private final IStorageProvider storageProvider;

    public ProjectHierarchyController(ProjectHierarchyService projectHierarchyService,
                                       EntregableCambioFechaRepository cambioFechaRepository,
                                       IStorageProvider storageProvider) {
        this.projectHierarchyService = projectHierarchyService;
        this.cambioFechaRepository = cambioFechaRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    @GetMapping("/{id}/hierarchy")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<ProjectHierarchyDTO>> getProjectHierarchy(@PathVariable String id) {
        ProjectHierarchyDTO hierarchy = projectHierarchyService.getProjectHierarchy(id);
        return ResponseEntity.ok(ApiResponse.success(hierarchy, "Jerarquía obtenida con éxito"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> agregarFase(String id, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto, Authentication authentication) {
        com.proyecta.api_gestion.model.Fase fase = projectHierarchyService.agregarFase(id, dto, authentication);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO(fase.getId(), fase.getNombre(), fase.getDescripcion(), fase.getPonderacion(), fase.getAvanceCalculado(), null), "Fase agregada"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> editarFase(String id, Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto, Authentication authentication) {
        com.proyecta.api_gestion.model.Fase fase = projectHierarchyService.editarFase(id, faseId, dto, authentication);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO(fase.getId(), fase.getNombre(), fase.getDescripcion(), fase.getPonderacion(), fase.getAvanceCalculado(), null), "Fase editada"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<Void> eliminarFase(String id, Integer faseId, Authentication authentication) {
        projectHierarchyService.eliminarFase(id, faseId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> agregarHito(String id, Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto, Authentication authentication) {
        com.proyecta.api_gestion.model.Hito hito = projectHierarchyService.agregarHito(id, faseId, dto, authentication);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO(hito.getId(), hito.getNombre(), hito.getDescripcion(), hito.getPonderacion(), hito.getAvanceCalculado(), null), "Hito agregado"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> editarHito(String id, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto, Authentication authentication) {
        com.proyecta.api_gestion.model.Hito hito = projectHierarchyService.editarHito(id, faseId, hitoId, dto, authentication);
        return ResponseEntity.ok(ApiResponse.success(new com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO(hito.getId(), hito.getNombre(), hito.getDescripcion(), hito.getPonderacion(), hito.getAvanceCalculado(), null), "Hito editado"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<Void> eliminarHito(String id, Integer faseId, Integer hitoId, Authentication authentication) {
        projectHierarchyService.eliminarHito(id, faseId, hitoId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<java.util.List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO>>> listarEntregablesProyecto(String id) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.listarEntregablesProyecto(id), "Lista de entregables obtenida"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> agregarEntregable(String id, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.agregarEntregable(id, faseId, hitoId, dto, authentication), "Entregable agregado"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> editarEntregable(String id, Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(projectHierarchyService.editarEntregable(id, entregableId, dto, authentication), "Entregable editado"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canManageProjectStructure(#id, authentication)")
    public ResponseEntity<Void> eliminarEntregable(String id, Integer entregableId, Authentication authentication) {
        projectHierarchyService.eliminarEntregable(id, entregableId, authentication);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Cambio de fecha limite con justificacion y evidencia
    // -----------------------------------------------------------------------

    @Override
    @PreAuthorize("@proyectoSecurity.canChangeDeadline(#id, authentication)")
    public ResponseEntity<ApiResponse<CambioFechaResponse>> cambiarFecha(
            String id, Integer entregableId,
            CambioFechaRequest request,
            MultipartFile evidencia,
            Authentication authentication) {
        CambioFechaResponse response = projectHierarchyService.cambiarFecha(id, entregableId, request, evidencia, authentication);
        return ResponseEntity.ok(ApiResponse.success(response, "Fecha limite actualizada exitosamente"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canChangeDeadline(#id, authentication)")
    public ResponseEntity<ApiResponse<List<CambioFechaResponse>>> historialFechas(String id, Integer entregableId) {
        List<CambioFechaResponse> historial = projectHierarchyService.obtenerHistorialFechas(entregableId);
        return ResponseEntity.ok(ApiResponse.success(historial, "Historial de cambios de fecha"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canChangeDeadline(#id, authentication)")
    public ResponseEntity<Resource> descargarPdfCambioFecha(String id, Long cambioId) {
        EntregableCambioFecha registro = cambioFechaRepository.findById(cambioId)
                .orElseThrow(() -> new com.proyecta.api_gestion.exception.ResourceNotFoundException("Registro de cambio de fecha no encontrado: " + cambioId));
        Resource resource = storageProvider.loadFileAsResource("cambios-fecha", registro.getArchivoPdf());
        String nombreDescarga = registro.getNombreOriginal() != null ? registro.getNombreOriginal() : "soporte-cambio-fecha.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreDescarga + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
