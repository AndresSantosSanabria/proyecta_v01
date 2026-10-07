package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.beneficioimpacto.ProyectoBeneficioImpacto;
import java.util.Optional;

public interface ProyectoBeneficioImpactoRepositoryPort {

    Optional<ProyectoBeneficioImpacto> findByProyectoId(String proyectoId);

    <E extends ProyectoBeneficioImpacto> E save(E entity);
}

