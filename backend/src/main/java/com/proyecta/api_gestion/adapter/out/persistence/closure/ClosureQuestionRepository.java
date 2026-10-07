package com.proyecta.api_gestion.adapter.out.persistence.closure;

import com.proyecta.api_gestion.domain.model.closure.ClosureQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureQuestionRepositoryPort;
@Repository
public interface ClosureQuestionRepository extends JpaRepository<ClosureQuestion, Long>, ClosureQuestionRepositoryPort {

    List<ClosureQuestion> findAllByOrderByOrdenAsc();

    List<ClosureQuestion> findByActivoTrueOrderByOrdenAsc();
}

