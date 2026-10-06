package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Patrocinador;
import java.util.Optional;

public interface PatrocinadorRepositoryPort {

    Optional<Patrocinador> findFirstByNombreOrderByIdAsc(String nombre);

    java.util.List<Patrocinador> findUniquePatrocinadores();

    <E extends Patrocinador> E save(E entity);
}

