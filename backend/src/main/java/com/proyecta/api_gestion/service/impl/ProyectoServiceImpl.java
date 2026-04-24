package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;

    public ProyectoServiceImpl(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @Override
    public List<Proyecto> obtenerProyectosActivosConAvance(BigDecimal minimo) {
        if (minimo == null || minimo.compareTo(BigDecimal.ZERO) < 0 || minimo.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El avance mínimo debe ser un valor entre 0 y 100");
        }
        return proyectoRepository.buscarProyectosConAvanceMayorA(minimo);
    }

    @Override
    public Proyecto obtenerPorId(String id) {
        String idLimpio = id.trim();
        return proyectoRepository.findById(idLimpio).orElseThrow(() ->
            new ResourceNotFoundException("Proyecto no encontrado: " + idLimpio)
        );
    }
}
