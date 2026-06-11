package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.report.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

public interface ReporteService {
    List<ReporteConfigDTO> obtenerConfiguracionReportes();
    Optional<ReporteVistaPreviaDTO> obtenerVistaPrevia(String proyectoId);
    List<ProyectoReporteResumenDTO> obtenerTodosLosProyectos();
    List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos();
    Optional<FuragReporteDTO> obtenerFurag(String proyectoId);
    List<RiesgoVerificacionReporteDTO> obtenerVerificacionRiesgos();
    
    // Reportes Binarios
    byte[] generarReporteProyectoPdf(String id, String detailMode);
    byte[] generarReportePortafolioPdf();
    byte[] generarReporteProyectosConRetrasosPdf();
    byte[] generarReportePlanComunicacionesPdf();
    byte[] generarReporteFuragPdf(String proyectoId);
    byte[] generarReporteRiesgosPdf();
    byte[] generarReportePortafolioExcel();
    byte[] generarReportePortafolioExcel(Authentication authentication, String query, String dependency, String status, String peti);
}
