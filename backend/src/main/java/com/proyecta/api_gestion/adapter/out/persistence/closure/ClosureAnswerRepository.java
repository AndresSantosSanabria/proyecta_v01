package com.proyecta.api_gestion.adapter.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureAnswerRepositoryPort;
@Repository
public interface ClosureAnswerRepository extends JpaRepository<ClosureAnswer, Long>, ClosureAnswerRepositoryPort {

    List<ClosureAnswer> findByProyectoIdOrderByQuestionOrdenAsc(String proyectoId);

    Optional<ClosureAnswer> findByProyectoIdAndQuestionId(String proyectoId, Long questionId);

    boolean existsByQuestionId(Long questionId);
}

