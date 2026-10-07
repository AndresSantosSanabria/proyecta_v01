package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.SystemParameter;
import java.util.Optional;

public interface SystemParameterRepositoryPort {

    Optional<SystemParameter> findByKey(String key);

    <E extends SystemParameter> E save(E entity);

    java.util.Optional<SystemParameter> findById(String id);

    boolean existsById(String id);
}

