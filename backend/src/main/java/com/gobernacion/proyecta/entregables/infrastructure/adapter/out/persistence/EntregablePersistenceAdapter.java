package com.gobernacion.proyecta.entregables.infrastructure.adapter.out.persistence;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import com.gobernacion.proyecta.entregables.domain.port.out.EntregableRepositoryPort;
import com.gobernacion.proyecta.entregables.infrastructure.mapper.EntregableMapper;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EntregablePersistenceAdapter implements EntregableRepositoryPort {

    private final JpaEntregableRepository jpaRepository;
    private final EntregableMapper mapper;

    public EntregablePersistenceAdapter(JpaEntregableRepository jpaRepository, EntregableMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Entregable save(Entregable entregable) {
        EntregableEntity entity = mapper.toEntity(entregable);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Entregable> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Entregable> findByHitoId(Integer hitoId) {
        return jpaRepository.findByHitoId(hitoId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return jpaRepository.existsById(id);
    }
}
