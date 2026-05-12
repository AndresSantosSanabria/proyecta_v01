package com.gobernacion.proyecta.proyectos.domain.service;

import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import com.gobernacion.proyecta.proyectos.domain.port.in.ProyectoUseCase;
import com.gobernacion.proyecta.proyectos.domain.port.out.ProyectoRepositoryPort;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de Dominio: Implementa la lógica de negocio pura.
 * No depende de Spring ni de la infraestructura.
 */
public class ProyectoService implements ProyectoUseCase {

    private final ProyectoRepositoryPort repositoryPort;

    public ProyectoService(ProyectoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Proyecto crearProyecto(Proyecto proyecto) {
        // Lógica de negocio para creación
        if (proyecto.getId() == null || proyecto.getId().isEmpty()) {
            throw new IllegalArgumentException("El ID del proyecto es obligatorio.");
        }
        return repositoryPort.save(proyecto);
    }

    @Override
    public Optional<Proyecto> obtenerPorId(String id) {
        return repositoryPort.findById(id);
    }

    @Override
    public List<Proyecto> listarTodos() {
        return repositoryPort.findAll();
    }

    @Override
    public Proyecto actualizarProyecto(String id, Proyecto proyecto) {
        if (!repositoryPort.existsById(id)) {
            throw new RuntimeException("Proyecto no encontrado");
        }
        proyecto.setId(id);
        return repositoryPort.save(proyecto);
    }

    @Override
    public void eliminarProyecto(String id) {
        repositoryPort.deleteById(id);
    }

    @Override
    public void cerrarProyecto(String id) {
        Proyecto proyecto = repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
        
        proyecto.cerrar(); // Uso de lógica de dominio pura
        repositoryPort.save(proyecto);
    }
}
