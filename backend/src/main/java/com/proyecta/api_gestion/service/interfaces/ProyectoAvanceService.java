package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO;

public interface ProyectoAvanceService {
    ProyectoAvanceDetalleDTO obtenerAvanceDetallado(String proyectoId);
    com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId);
}
