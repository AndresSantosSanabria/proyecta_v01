package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoUpdateDTO;
import com.proyecta.api_gestion.model.Proyecto;

import java.math.BigDecimal;
import java.util.List;

public interface ProyectoService {
    List<Proyecto> obtenerProyectosActivosConAvance(BigDecimal minimo);
    Proyecto obtenerPorId(String id);
    Proyecto crearProyecto(ProyectoCreateDTO dto);
    Proyecto actualizarProyecto(String id, ProyectoUpdateDTO dto);
    void eliminarProyecto(String id);
}
