package com.gobernacion.proyecta.entregables.domain.service;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import com.gobernacion.proyecta.entregables.domain.port.in.EntregableUseCase;
import com.gobernacion.proyecta.entregables.domain.port.out.EntregableRepositoryPort;
import java.util.List;
import java.util.Optional;

public class EntregableService implements EntregableUseCase {

    private final EntregableRepositoryPort repositoryPort;

    public EntregableService(EntregableRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Entregable crearEntregable(Entregable entregable) {
        return repositoryPort.save(entregable);
    }

    @Override
    public Optional<Entregable> obtenerPorId(Integer id) {
        return repositoryPort.findById(id);
    }

    @Override
    public List<Entregable> listarPorHito(Integer hitoId) {
        return repositoryPort.findByHitoId(hitoId);
    }

    @Override
    public Entregable actualizarEntregable(Integer id, Entregable entregable) {
        if (!repositoryPort.existsById(id)) {
            throw new RuntimeException("Entregable no encontrado");
        }
        entregable.setId(id);
        return repositoryPort.save(entregable);
    }

    @Override
    public void eliminarEntregable(Integer id) {
        repositoryPort.deleteById(id);
    }

    @Override
    public void darConformidad(Integer id) {
        Entregable entregable = repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Entregable no encontrado"));
        entregable.marcarConforme();
        repositoryPort.save(entregable);
    }
}
