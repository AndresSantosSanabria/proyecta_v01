package com.gobernacion.proyecta.entregables.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaEntregableRepository extends JpaRepository<EntregableEntity, Integer> {
    List<EntregableEntity> findByHitoId(Integer hitoId);
}
