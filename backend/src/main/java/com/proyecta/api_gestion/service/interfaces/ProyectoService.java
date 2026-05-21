package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProyectoService {
    Page<ProyectoListDTO> listarProyectos(String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti, Pageable pageable);
    ProyectoResponseDTO obtenerPorId(String id);
    ProyectoCreatedDTO crearProyecto(ProyectoCreateDTO dto);
    ProyectoResponseDTO actualizarProyecto(String id, ProyectoUpdateDTO dto);
    void eliminarProyecto(String id);
    ProyectoResumenDTO obtenerResumen(String id);
    void cerrarProyecto(String id);
    DashboardDTO obtenerDashboard();
    void recalcularAvances();
    
    // FURAG
    com.proyecta.api_gestion.model.Furag obtenerFurag(String id);
    void actualizarFurag(String id, com.proyecta.api_gestion.model.Furag furag);
}
