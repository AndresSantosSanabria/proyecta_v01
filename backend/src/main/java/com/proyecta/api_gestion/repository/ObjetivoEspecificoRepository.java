package com.proyecta.api_gestion.repository;

import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjetivoEspecificoRepository extends JpaRepository<ObjetivoEspecifico, Integer> {
    List<ObjetivoEspecifico> findByProyectoIdOrderByOrdenAsc(String proyectoId);
}
