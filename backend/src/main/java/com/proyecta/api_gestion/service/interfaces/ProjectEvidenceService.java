package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;

import java.util.List;

public interface ProjectEvidenceService {

    /**
     * Retorna la totalidad de archivos/evidencias asociados a un proyecto,
     * consolidando documentos del proyecto, documentos avanzados, documentos dinamicos,
     * evidencias de entregables, cronograma, soluciones de riesgos, cambios de fecha,
     * cambios de descripcion y acta de cierre.
     *
     * @param proyectoId ID del proyecto
     * @param categoria  Filtro opcional por categoria:
     *                   DOCUMENTO_PROYECTO, DOCUMENTO_PROYECTO_AVANZADO, DOCUMENTO_DINAMICO,
     *                   EVIDENCIA_ENTREGABLE, CRONOGRAMA, RIESGO, MATRIZ_RIESGOS,
     *                   CAMBIO_FECHA, CAMBIO_DESCRIPCION, ACTA_CIERRE, TODOS
     * @return Lista unificada de evidencias
     */
    List<ProjectEvidenceDTO> listarEvidencias(String proyectoId, String categoria);
}
