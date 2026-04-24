package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.model.Proyecto;
import java.math.BigDecimal;
import java.util.List;

public interface ProyectoService {
    List<Proyecto> obtenerProyectosActivosConAvance(BigDecimal minimo);
    Proyecto obtenerPorId(String id);
}
