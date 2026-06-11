package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;

public interface ProjectHierarchyService {
    ProjectHierarchyDTO getProjectHierarchy(String proyectoId);
    
    // CRUD Fases
    com.proyecta.api_gestion.model.Fase agregarFase(String proyectoId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);
    com.proyecta.api_gestion.model.Fase editarFase(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);
    void eliminarFase(String proyectoId, Integer faseId);

    // CRUD Hitos
    com.proyecta.api_gestion.model.Hito agregarHito(String proyectoId, Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);
    com.proyecta.api_gestion.model.Hito editarHito(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);
    void eliminarHito(String proyectoId, Integer faseId, Integer hitoId);

    // CRUD Entregables
    com.proyecta.api_gestion.model.Entregable agregarEntregable(String proyectoId, Integer faseId, Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);
    com.proyecta.api_gestion.model.Entregable editarEntregable(String proyectoId, Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);
    void eliminarEntregable(String proyectoId, Integer entregableId);

    java.util.List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO> listarEntregablesProyecto(String proyectoId);
}
