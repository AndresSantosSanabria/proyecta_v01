package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import org.springframework.stereotype.Service;

@Service
public class ProjectAdvanceServiceImpl implements ProyectoAvanceService {

    private final ProyectoRepository proyectoRepository;

    public ProjectAdvanceServiceImpl(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @Override
    public ProyectoAvanceDetalleDTO obtenerAvanceDetallado(String proyectoId) {
        return proyectoRepository.getAvanceProyecto(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró avance para el proyecto: " + proyectoId));
    }
}
