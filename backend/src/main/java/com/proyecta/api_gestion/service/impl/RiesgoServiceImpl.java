package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Riesgo;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import com.proyecta.api_gestion.service.IRiesgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiesgoServiceImpl implements IRiesgoService {

    @Autowired
    private RiesgoRepository riesgoRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Override
    public RiesgoListResponseDTO getRisksByProject(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        List<RiesgoResponseDTO> riesgos = riesgoRepository.findByProyectoId(projectId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return new RiesgoListResponseDTO(projectId, riesgos);
    }

    @Override
    @Transactional
    public RiesgoCreatedResponseDTO createRisk(String projectId, RiesgoRequestDTO requestDto) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        if (EstadoProyecto.CERRADO.equals(proyecto.getEstado())) {
            throw new ForbiddenException("No se pueden agregar riesgos a un proyecto cerrado.");
        }

        Riesgo riesgo = new Riesgo();
        riesgo.setDescripcion(requestDto.descripcion());
        riesgo.setProbabilidad(requestDto.probabilidad());
        riesgo.setImpacto(requestDto.impacto());
        riesgo.setProyecto(proyecto);
        riesgo.setEstado(requestDto.estado());
        riesgo.setTratamiento(requestDto.tratamiento());
        
        NivelRiesgo nivelCalculado = calculateLevel(requestDto.probabilidad(), requestDto.impacto());
        riesgo.setNivel(nivelCalculado);

        Riesgo savedRisk = riesgoRepository.save(riesgo);
        savedRisk.setCodigo("R" + String.format("%02d", savedRisk.getId()));
        riesgoRepository.save(savedRisk);

        return new RiesgoCreatedResponseDTO(
                savedRisk.getId(),
                savedRisk.getCodigo(),
                savedRisk.getNivel(),
                "Riesgo agregado exitosamente"
        );
    }

    @Override
    @Transactional
    public RiesgoResponseDTO updateRisk(String projectId, Integer riesgoId, RiesgoRequestDTO requestDto) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));

        if (!riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }

        if (EstadoProyecto.CERRADO.equals(riesgo.getProyecto().getEstado())) {
            throw new ForbiddenException("No se pueden editar riesgos de un proyecto cerrado.");
        }

        riesgo.setDescripcion(requestDto.descripcion());
        riesgo.setProbabilidad(requestDto.probabilidad());
        riesgo.setImpacto(requestDto.impacto());
        riesgo.setTratamiento(requestDto.tratamiento());
        riesgo.setEstado(requestDto.estado());
        riesgo.setNivel(calculateLevel(requestDto.probabilidad(), requestDto.impacto()));

        return convertToResponseDto(riesgoRepository.save(riesgo));
    }

    @Override
    @Transactional
    public void deleteRisk(String projectId, Integer riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));

        if (!riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }

        if (EstadoProyecto.CERRADO.equals(riesgo.getProyecto().getEstado())) {
            throw new ForbiddenException("No se pueden eliminar riesgos de un proyecto cerrado.");
        }

        riesgoRepository.delete(riesgo);
    }

    private NivelRiesgo calculateLevel(Probabilidad prob, Impacto imp) {
        // probabilidad ALTA + impacto ALTO → nivel CRITICO
        if (prob == Probabilidad.ALTA && imp == Impacto.ALTO) {
            return NivelRiesgo.CRITICO;
        }
        // probabilidad ALTA + impacto MEDIO → nivel ALTO
        if (prob == Probabilidad.ALTA && imp == Impacto.MEDIO) {
            return NivelRiesgo.ALTO;
        }
        // probabilidad MEDIA + impacto ALTO → nivel ALTO
        if (prob == Probabilidad.MEDIA && imp == Impacto.ALTO) {
            return NivelRiesgo.ALTO;
        }
        // probabilidad MEDIA + impacto MEDIO → nivel MODERADO
        if (prob == Probabilidad.MEDIA && imp == Impacto.MEDIO) {
            return NivelRiesgo.MODERADO;
        }
        // probabilidad BAJA + cualquiera → nivel BAJO
        if (prob == Probabilidad.BAJA) {
            return NivelRiesgo.BAJO;
        }

        // Casos faltantes en la tabla, asumo:
        if (prob == Probabilidad.ALTA && imp == Impacto.BAJO) {
            return NivelRiesgo.MODERADO;
        }
        if (prob == Probabilidad.MEDIA && imp == Impacto.BAJO) {
            return NivelRiesgo.BAJO;
        }

        return NivelRiesgo.BAJO;
    }

    private RiesgoResponseDTO convertToResponseDto(Riesgo riesgo) {
        return new RiesgoResponseDTO(
                riesgo.getId(),
                riesgo.getCodigo(),
                riesgo.getDescripcion(),
                riesgo.getProbabilidad(),
                riesgo.getImpacto(),
                riesgo.getNivel(),
                riesgo.getTratamiento(),
                riesgo.getEstado()
        );
    }
}
