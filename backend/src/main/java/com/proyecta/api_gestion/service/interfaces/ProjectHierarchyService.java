package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;

public interface ProjectHierarchyService {
    ProjectHierarchyDTO getProjectHierarchy(String proyectoId);
    
    // CRUD Fases
    com.proyecta.api_gestion.model.Fase agregarFase(String proyectoId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);
    com.proyecta.api_gestion.model.Fase editarFase(Integer faseId, com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);
    void eliminarFase(Integer faseId);

    // CRUD Hitos
    com.proyecta.api_gestion.model.Hito agregarHito(Integer faseId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);
    com.proyecta.api_gestion.model.Hito editarHito(Integer hitoId, com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);
    void eliminarHito(Integer hitoId);

    // CRUD Entregables
    com.proyecta.api_gestion.model.Entregable agregarEntregable(Integer hitoId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);
    com.proyecta.api_gestion.model.Entregable editarEntregable(Integer entregableId, com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);
    void eliminarEntregable(Integer entregableId);

    java.util.List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO> listarEntregablesProyecto(String proyectoId);
}
