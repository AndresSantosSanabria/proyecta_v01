package com.proyecta.api_gestion.application.port.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureAnswer;
import java.util.List;
import java.util.Optional;

public interface ClosureAnswerRepositoryPort {

    List<ClosureAnswer> findByProyectoIdOrderByQuestionOrdenAsc(String proyectoId);

    Optional<ClosureAnswer> findByProyectoIdAndQuestionId(String proyectoId, Long questionId);

    boolean existsByQuestionId(Long questionId);

    <E extends ClosureAnswer> E save(E entity);
}

