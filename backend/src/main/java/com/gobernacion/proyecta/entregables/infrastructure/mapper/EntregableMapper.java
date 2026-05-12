package com.gobernacion.proyecta.entregables.infrastructure.mapper;

import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import com.gobernacion.proyecta.entregables.infrastructure.adapter.out.persistence.EntregableEntity;
import org.springframework.stereotype.Component;

@Component
public class EntregableMapper {

    public Entregable toDomain(EntregableEntity entity) {
        if (entity == null) return null;
        Entregable domain = new Entregable();
        domain.setId(entity.getId());
        domain.setNombre(entity.getNombre());
        domain.setPonderacion(entity.getPonderacion());
        domain.setEstado(entity.getEstado());
        domain.setConforme(entity.getConforme());
        domain.setFechaLimite(entity.getFechaLimite());
        domain.setFechaEntregaReal(entity.getFechaEntregaReal());
        domain.setArchivoPdf(entity.getArchivoPdf());
        domain.setHitoId(entity.getHitoId());
        domain.setFechaCreacion(entity.getFechaCreacion());
        return domain;
    }

    public EntregableEntity toEntity(Entregable domain) {
        if (domain == null) return null;
        EntregableEntity entity = new EntregableEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setPonderacion(domain.getPonderacion());
        entity.setEstado(domain.getEstado());
        entity.setConforme(domain.getConforme());
        entity.setFechaLimite(domain.getFechaLimite());
        entity.setFechaEntregaReal(domain.getFechaEntregaReal());
        entity.setArchivoPdf(domain.getArchivoPdf());
        entity.setHitoId(domain.getHitoId());
        entity.setFechaCreacion(domain.getFechaCreacion());
        return entity;
    }
}
