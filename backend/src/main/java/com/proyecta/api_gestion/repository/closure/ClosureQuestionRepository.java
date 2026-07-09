package com.proyecta.api_gestion.repository.closure;

import com.proyecta.api_gestion.model.closure.ClosureQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClosureQuestionRepository extends JpaRepository<ClosureQuestion, Long> {

    List<ClosureQuestion> findAllByOrderByOrdenAsc();

    List<ClosureQuestion> findByActivoTrueOrderByOrdenAsc();

    List<ClosureQuestion> findByActivoFalseOrderByOrdenAsc();

    long countByActivoTrue();
}
