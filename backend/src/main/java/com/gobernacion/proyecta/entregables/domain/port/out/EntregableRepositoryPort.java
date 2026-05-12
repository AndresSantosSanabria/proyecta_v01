package com.gobernacion.proyecta.entregables.domain.port.out;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import java.util.List;
import java.util.Optional;

public interface EntregableRepositoryPort {
    Entregable save(Entregable entregable);
    Optional<Entregable> findById(Integer id);
    List<Entregable> findByHitoId(Integer hitoId);
    void deleteById(Integer id);
    boolean existsById(Integer id);
}
