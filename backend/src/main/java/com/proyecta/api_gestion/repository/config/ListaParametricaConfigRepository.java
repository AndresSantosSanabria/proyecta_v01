package com.proyecta.api_gestion.repository.config;

import com.proyecta.api_gestion.model.config.ListaParametricaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaParametricaConfigRepository extends JpaRepository<ListaParametricaConfig, Long> {
    List<ListaParametricaConfig> findByListaClaveAndActivoTrueOrderByOrdenAsc(String listaClave);
    List<ListaParametricaConfig> findByListaClaveOrderByOrdenAsc(String listaClave);
    Optional<ListaParametricaConfig> findByListaClaveAndItemCodigo(String listaClave, String itemCodigo);
    boolean existsByListaClaveAndItemCodigo(String listaClave, String itemCodigo);
}
