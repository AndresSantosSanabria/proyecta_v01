package com.gobernacion.proyecta.proyectos.domain.port.in;

import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de Entrada (Input Port): Define las operaciones que el mundo exterior puede solicitar al dominio.
 */
public interface ProyectoUseCase {
    Proyecto crearProyecto(Proyecto proyecto);
    Optional<Proyecto> obtenerPorId(String id);
    List<Proyecto> listarTodos();
    Proyecto actualizarProyecto(String id, Proyecto proyecto);
    void eliminarProyecto(String id);
    void cerrarProyecto(String id);
}
