package com.proyecta.api_gestion.adapter.out.persistence;

import com.proyecta.api_gestion.domain.model.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.SystemParameterRepositoryPort;
@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, String>, SystemParameterRepositoryPort {
    Optional<SystemParameter> findByKey(String key);
}

