package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoUpdateDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.PatrocinadorRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PatrocinadorRepository patrocinadorRepository;

    public ProyectoServiceImpl(ProyectoRepository proyectoRepository,
                               UsuarioRepository usuarioRepository,
                               PatrocinadorRepository patrocinadorRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.patrocinadorRepository = patrocinadorRepository;
    }

    @Override
    public List<Proyecto> obtenerProyectosActivosConAvance(BigDecimal minimo) {
        if (minimo == null || minimo.compareTo(BigDecimal.ZERO) < 0 || minimo.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("El avance mínimo debe ser un valor entre 0 y 100");
        }
        return proyectoRepository.buscarProyectosConAvanceMayorA(minimo);
    }

    @Override
    public Proyecto obtenerPorId(String id) {
        String idLimpio = id.trim();
        return proyectoRepository.findById(idLimpio).orElseThrow(() ->
            new ResourceNotFoundException("Proyecto no encontrado: " + idLimpio)
        );
    }

    @Override
    @Transactional
    public Proyecto crearProyecto(ProyectoCreateDTO dto) {
        if (proyectoRepository.existsById(dto.getId())) {
            throw new BadRequestException("Ya existe un proyecto con el ID: " + dto.getId());
        }

        Proyecto proyecto = new Proyecto();
        proyecto.setId(dto.getId());
        mapDtoToEntity(dto, proyecto);

        return proyectoRepository.save(proyecto);
    }

    @Override
    @Transactional
    public Proyecto actualizarProyecto(String id, ProyectoUpdateDTO dto) {
        Proyecto proyecto = obtenerPorId(id);
        mapUpdateDtoToEntity(dto, proyecto);
        return proyectoRepository.save(proyecto);
    }

    @Override
    @Transactional
    public void eliminarProyecto(String id) {
        Proyecto proyecto = obtenerPorId(id);
        proyectoRepository.delete(proyecto);
    }

    private void mapDtoToEntity(ProyectoCreateDTO dto, Proyecto entity) {
        entity.setNombre(dto.getNombre());
        entity.setDependencia(dto.getDependencia());
        entity.setObjetivoGeneral(dto.getObjetivoGeneral());
        entity.setEsPeti(dto.getEsPeti());
        entity.setEstrategiaPeti(dto.getEstrategiaPeti());
        entity.setVigenciaPeti(dto.getVigenciaPeti());
        entity.setFechaInicio(dto.getFechaInicio());
        entity.setFechaCierre(dto.getFechaCierre());
        entity.setEstado(dto.getEstado());

        if (dto.getGestorId() != null) {
            entity.setGestor(usuarioRepository.findById(dto.getGestorId())
                .orElseThrow(() -> new ResourceNotFoundException("Gestor no encontrado con ID: " + dto.getGestorId())));
        }
        if (dto.getDirectorId() != null) {
            entity.setDirector(usuarioRepository.findById(dto.getDirectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Director no encontrado con ID: " + dto.getDirectorId())));
        }
        if (dto.getPatrocinadorId() != null) {
            entity.setPatrocinador(patrocinadorRepository.findById(dto.getPatrocinadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador no encontrado con ID: " + dto.getPatrocinadorId())));
        }
    }

    private void mapUpdateDtoToEntity(ProyectoUpdateDTO dto, Proyecto entity) {
        if (dto.getNombre() != null) entity.setNombre(dto.getNombre());
        if (dto.getDependencia() != null) entity.setDependencia(dto.getDependencia());
        if (dto.getObjetivoGeneral() != null) entity.setObjetivoGeneral(dto.getObjetivoGeneral());
        if (dto.getEsPeti() != null) entity.setEsPeti(dto.getEsPeti());
        if (dto.getEstrategiaPeti() != null) entity.setEstrategiaPeti(dto.getEstrategiaPeti());
        if (dto.getVigenciaPeti() != null) entity.setVigenciaPeti(dto.getVigenciaPeti());
        if (dto.getFechaInicio() != null) entity.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaCierre() != null) entity.setFechaCierre(dto.getFechaCierre());
        if (dto.getEstado() != null) entity.setEstado(dto.getEstado());
        if (dto.getCerrado() != null) entity.setCerrado(dto.getCerrado());

        if (dto.getGestorId() != null) {
            entity.setGestor(usuarioRepository.findById(dto.getGestorId())
                .orElseThrow(() -> new ResourceNotFoundException("Gestor no encontrado con ID: " + dto.getGestorId())));
        }
        if (dto.getDirectorId() != null) {
            entity.setDirector(usuarioRepository.findById(dto.getDirectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Director no encontrado con ID: " + dto.getDirectorId())));
        }
        if (dto.getPatrocinadorId() != null) {
            entity.setPatrocinador(patrocinadorRepository.findById(dto.getPatrocinadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador no encontrado con ID: " + dto.getPatrocinadorId())));
        }
    }
}
