package com.proyecta.api_gestion.application.port.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureQuestion;
import java.util.List;

public interface ClosureQuestionRepositoryPort {

    List<ClosureQuestion> findAllByOrderByOrdenAsc();

    List<ClosureQuestion> findByActivoTrueOrderByOrdenAsc();

    <E extends ClosureQuestion> E save(E entity);

    java.util.Optional<ClosureQuestion> findById(Long id);

    void delete(ClosureQuestion entity);
}

