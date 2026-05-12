package com.gobernacion.proyecta.proyectos.infrastructure.adapter.out.persistence;

import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import com.gobernacion.proyecta.proyectos.domain.port.out.ProyectoRepositoryPort;
import com.gobernacion.proyecta.proyectos.infrastructure.mapper.ProyectoMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de Persistencia: Implementa el puerto de salida usando JPA.
 */
@Component
public class ProyectoPersistenceAdapter implements ProyectoRepositoryPort {

    private final JpaProyectoRepository jpaRepository;
    private final ProyectoMapper mapper;

    public ProyectoPersistenceAdapter(JpaProyectoRepository jpaRepository, ProyectoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Proyecto save(Proyecto proyecto) {
        ProyectoEntity entity = mapper.toEntity(proyecto);
        ProyectoEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Proyecto> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Proyecto> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }
}
