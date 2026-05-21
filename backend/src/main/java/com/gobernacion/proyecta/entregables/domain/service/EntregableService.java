package com.gobernacion.proyecta.entregables.domain.service;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import com.gobernacion.proyecta.entregables.domain.port.in.EntregableUseCase;
import com.gobernacion.proyecta.entregables.domain.port.out.EntregableRepositoryPort;
import java.time.LocalDate;
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
        Entregable existente = repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Entregable no encontrado"));
        existente.asegurarModificable();
        entregable.setId(id);
        return repositoryPort.save(entregable);
    }

    @Override
    public void eliminarEntregable(Integer id) {
        Entregable existente = repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Entregable no encontrado"));
        existente.asegurarModificable();
        repositoryPort.deleteById(id);
    }

    @Override
    public void darConformidad(Integer id, LocalDate fechaEntrega, String archivoPdf) {
        Entregable entregable = repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Entregable no encontrado"));
        entregable.completar(archivoPdf, fechaEntrega);
        repositoryPort.save(entregable);
    }
}
