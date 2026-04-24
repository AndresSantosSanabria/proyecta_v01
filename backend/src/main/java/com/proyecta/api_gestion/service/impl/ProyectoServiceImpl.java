package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.ProyectoService;
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
        return proyectoRepository.buscarProyectosConAvanceMayorA(minimo);
    }

    @Override
    public Proyecto obtenerPorId(String id) {
        String idLimpio = id.trim();
        return proyectoRepository.findById(idLimpio).orElseThrow(() -> {
            List<Proyecto> todos = proyectoRepository.findAll();
            System.out.println("LOG: Buscando [" + idLimpio + "]. Proyectos en DB: " +
                    todos.stream().map(Proyecto::getId).toList());
            return new RuntimeException("Proyecto no encontrado: " + idLimpio);
        });
    }
}
