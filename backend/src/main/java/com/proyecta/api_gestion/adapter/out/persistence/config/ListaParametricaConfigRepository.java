package com.proyecta.api_gestion.adapter.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.ListaParametricaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.config.ListaParametricaConfigRepositoryPort;
@Repository
public interface ListaParametricaConfigRepository extends JpaRepository<ListaParametricaConfig, Long>, ListaParametricaConfigRepositoryPort {
    List<ListaParametricaConfig> findByListaClaveAndActivoTrueOrderByOrdenAsc(String listaClave);
    List<ListaParametricaConfig> findByListaClaveOrderByOrdenAsc(String listaClave);
    Optional<ListaParametricaConfig> findByListaClaveAndItemCodigo(String listaClave, String itemCodigo);
}

