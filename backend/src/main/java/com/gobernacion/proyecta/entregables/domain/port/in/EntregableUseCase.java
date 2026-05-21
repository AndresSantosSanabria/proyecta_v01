package com.gobernacion.proyecta.entregables.domain.port.in;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntregableUseCase {
    Entregable crearEntregable(Entregable entregable);
    Optional<Entregable> obtenerPorId(Integer id);
    List<Entregable> listarPorHito(Integer hitoId);
    Entregable actualizarEntregable(Integer id, Entregable entregable);
    void eliminarEntregable(Integer id);
    void darConformidad(Integer id, LocalDate fechaEntrega, String archivoPdf);
}
