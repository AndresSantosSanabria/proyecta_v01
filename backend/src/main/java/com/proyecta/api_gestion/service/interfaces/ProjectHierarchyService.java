package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;

public interface ProjectHierarchyService {
    ProjectHierarchyDTO getProjectHierarchy(String proyectoId);
}
