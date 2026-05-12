package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.report.*;

import java.util.List;
import java.util.Optional;

public interface ReporteService {
    List<ReporteConfigDTO> obtenerConfiguracionReportes();
    Optional<ReporteVistaPreviaDTO> obtenerVistaPrevia(String proyectoId);
    List<ProyectoReporteResumenDTO> obtenerTodosLosProyectos();
    List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos();
    Optional<PlanComunicacionesDTO> obtenerPlanComunicaciones(String proyectoId);
    Optional<FuragReporteDTO> obtenerFurag(String proyectoId);
    List<RiesgoReporteDTO> obtenerRiesgos(String proyectoId);
    
    // Reportes Binarios
    byte[] generarReporteProyectoPdf(String id);
    byte[] generarReportePortafolioPdf();
    byte[] generarReportePortafolioExcel();
}
