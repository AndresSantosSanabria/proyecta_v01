package com.gobernacion.proyecta.proyectos.domain.port.out;

import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de Salida (Output Port): Define cómo el dominio se comunica con la persistencia u otros sistemas.
 */
public interface ProyectoRepositoryPort {
    Proyecto save(Proyecto proyecto);
    Optional<Proyecto> findById(String id);
    List<Proyecto> findAll();
    void deleteById(String id);
    boolean existsById(String id);
}
