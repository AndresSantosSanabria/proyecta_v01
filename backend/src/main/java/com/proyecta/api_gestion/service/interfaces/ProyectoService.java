package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.domain.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface ProyectoService {
    PageResult<ProyectoListDTO> listarProyectos(String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti, PageQuery query);
    List<ProyectoListDTO> listarProyectosAsignados(String username);
    List<SeguridadUsuarioDTO> listarDirectoresAsignables();
    ProyectoResponseDTO obtenerPorId(String id);
    ProyectoCreatedDTO registrarProyectoInicial(ProyectoRegistroInicialDTO dto, String gestorUsername);
    String obtenerSiguienteCodigo();
    ProyectoCompletionStatusDTO obtenerEstadoCompletitud(String id, String username);
    ProyectoResponseDTO completarInformacionInicial(String id, ProyectoCompletarInformacionDTO dto, String username);
    ProyectoResponseDTO actualizarProyecto(String id, ProyectoUpdateDTO dto, Authentication authentication);
    void eliminarProyecto(String id, Authentication authentication);
    ProyectoResumenDTO obtenerResumen(String id);
    void cerrarProyecto(String id);
    DashboardDTO obtenerDashboard();
    void recalcularAvances();
    
    // FURAG
    com.proyecta.api_gestion.domain.model.Furag obtenerFurag(String id);
    void actualizarFurag(String id, com.proyecta.api_gestion.domain.model.Furag furag);

    // Completitud por fases (borrador)
    CompletitudBorradorDTO guardarBorradorCompletitud(String id, CompletitudBorradorDTO dto, String username);
    CompletitudBorradorDTO obtenerBorradorCompletitud(String id, String username);
    ProyectoResponseDTO completarFaseCompletitud(String id, Integer fase, String username);

    // Quality Gate: Viabilidad
    void cerrarForzoso(String id, String gestorUsername, String comentario);
}
