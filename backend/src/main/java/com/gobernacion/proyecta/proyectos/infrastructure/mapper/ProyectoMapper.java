package com.gobernacion.proyecta.proyectos.infrastructure.mapper;

import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import com.gobernacion.proyecta.proyectos.infrastructure.adapter.out.persistence.ProyectoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para convertir entre el modelo de dominio y la entidad de persistencia.
 */
@Component
public class ProyectoMapper {

    public Proyecto toDomain(ProyectoEntity entity) {
        if (entity == null) return null;
        Proyecto domain = new Proyecto();
        domain.setId(entity.getId());
        domain.setNombre(entity.getNombre());
        domain.setDependencia(entity.getDependencia());
        domain.setDirector(entity.getDirector());
        domain.setCorreoDirector(entity.getCorreoDirector());
        domain.setObjetivoGeneral(entity.getObjetivoGeneral());
        domain.setFechaInicio(entity.getFechaInicio());
        domain.setPeti(entity.getPeti());
        domain.setVigenciaPeti(entity.getVigenciaPeti());
        domain.setEstrategiaPeti(entity.getEstrategiaPeti());
        domain.setTienePlanComunicaciones(entity.getTienePlanComunicaciones());
        domain.setCronogramaPdf(entity.getCronogramaPdf());
        domain.setActaConstitucionPdf(entity.getActaConstitucionPdf());
        domain.setPlanComunicacionesPdf(entity.getPlanComunicacionesPdf());
        domain.setViabilizacionPdf(entity.getViabilizacionPdf());
        domain.setEstado(entity.getEstado());
        domain.setAvanceTotal(entity.getAvanceTotal());
        domain.setFechaRegistro(entity.getFechaRegistro());
        return domain;
    }

    public ProyectoEntity toEntity(Proyecto domain) {
        if (domain == null) return null;
        ProyectoEntity entity = new ProyectoEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setDependencia(domain.getDependencia());
        entity.setDirector(domain.getDirector());
        entity.setCorreoDirector(domain.getCorreoDirector());
        entity.setObjetivoGeneral(domain.getObjetivoGeneral());
        entity.setFechaInicio(domain.getFechaInicio());
        entity.setPeti(domain.getPeti());
        entity.setVigenciaPeti(domain.getVigenciaPeti());
        entity.setEstrategiaPeti(domain.getEstrategiaPeti());
        entity.setTienePlanComunicaciones(domain.getTienePlanComunicaciones());
        entity.setCronogramaPdf(domain.getCronogramaPdf());
        entity.setActaConstitucionPdf(domain.getActaConstitucionPdf());
        entity.setPlanComunicacionesPdf(domain.getPlanComunicacionesPdf());
        entity.setViabilizacionPdf(domain.getViabilizacionPdf());
        entity.setEstado(domain.getEstado());
        entity.setAvanceTotal(domain.getAvanceTotal());
        entity.setFechaRegistro(domain.getFechaRegistro());
        return entity;
    }
}
