package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.Hito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HitoRepository extends JpaRepository<Hito, Integer> {
    List<Hito> findByFaseIdOrderByNumeroAsc(Integer faseId);
}
