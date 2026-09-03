package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;

import java.util.List;

public interface ProjectEvidenceService {

    /**
     * Retorna la totalidad de archivos/evidencias asociados a un proyecto,
     * consolidando documentos del proyecto, evidencias de entregables,
     * cronograma, soluciones de riesgos, cambios de fecha y evidencias de cierre.
     *
     * @param proyectoId ID del proyecto
     * @param categoria  Filtro opcional por categoria (DOCUMENTO_PROYECTO, EVIDENCIA_ENTREGABLE, CRONOGRAMA, RIESGO, CAMBIO_FECHA, CIERRE)
     * @return Lista unificada de evidencias
     */
    List<ProjectEvidenceDTO> listarEvidencias(String proyectoId, String categoria);
}
