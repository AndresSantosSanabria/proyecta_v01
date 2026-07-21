package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaRequest;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectHierarchyService {
    ProjectHierarchyDTO getProjectHierarchy(String proyectoId);
    
    // CRUD Fases
    com.proyecta.api_gestion.model.Fase agregarFase(String proyectoId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto, Authentication authentication);
    com.proyecta.api_gestion.model.Fase editarFase(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto, Authentication authentication);
    void eliminarFase(String proyectoId, Integer faseId, Authentication authentication);

    // CRUD Hitos
    com.proyecta.api_gestion.model.Hito agregarHito(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto, Authentication authentication);
    com.proyecta.api_gestion.model.Hito editarHito(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto, Authentication authentication);
    void eliminarHito(String proyectoId, Integer faseId, Integer hitoId, Authentication authentication);

    // CRUD Entregables
    com.proyecta.api_gestion.model.Entregable agregarEntregable(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, Authentication authentication);
    com.proyecta.api_gestion.model.Entregable editarEntregable(String proyectoId, Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto, Authentication authentication);
    void eliminarEntregable(String proyectoId, Integer entregableId, Authentication authentication);

    List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO> listarEntregablesProyecto(String proyectoId);

    // Cambio de fecha limite con justificacion y evidencia
    CambioFechaResponse cambiarFecha(String proyectoId, Integer entregableId, CambioFechaRequest request, MultipartFile evidencia, Authentication authentication);
    List<CambioFechaResponse> obtenerHistorialFechas(Integer entregableId);
    boolean tieneHistorialCambiosFecha(Integer entregableId);
}

