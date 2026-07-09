package com.proyecta.api_gestion.repository.closure;

import com.proyecta.api_gestion.model.closure.ClosureAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClosureAnswerRepository extends JpaRepository<ClosureAnswer, Long> {

    List<ClosureAnswer> findByProyectoIdOrderByQuestion_OrdenAsc(String proyectoId);

    Optional<ClosureAnswer> findByProyectoIdAndQuestionId(String proyectoId, Long questionId);

    void deleteByProyectoId(String proyectoId);

    void deleteByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);
}
