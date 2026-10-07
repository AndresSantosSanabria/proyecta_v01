package com.proyecta.api_gestion.application.port.out.persistence.config;

import com.proyecta.api_gestion.domain.model.config.ListaParametricaConfig;
import java.util.List;
import java.util.Optional;

public interface ListaParametricaConfigRepositoryPort {

    List<ListaParametricaConfig> findByListaClaveAndActivoTrueOrderByOrdenAsc(String listaClave);

    List<ListaParametricaConfig> findByListaClaveOrderByOrdenAsc(String listaClave);

    Optional<ListaParametricaConfig> findByListaClaveAndItemCodigo(String listaClave, String itemCodigo);

    <E extends ListaParametricaConfig> E save(E entity);

    void delete(ListaParametricaConfig entity);
}

