package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.RiesgoCreateDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoUpdateDTO;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Riesgo;
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
    public List<RiesgoResponseDTO> getRisksByProject(String projectId) {
        return riesgoRepository.findByProyectoId(projectId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RiesgoResponseDTO createRisk(RiesgoCreateDTO createDto) {
        Proyecto proyecto = proyectoRepository.findById(createDto.getProyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + createDto.getProyectoId()));

        if (Boolean.TRUE.equals(proyecto.getCerrado())) {
            throw new ForbiddenException("No se pueden agregar riesgos a un proyecto cerrado.");
        }

        Riesgo riesgo = new Riesgo();
        riesgo.setDescripcion(createDto.getDescripcion());
        riesgo.setProbabilidad(createDto.getProbabilidad());
        riesgo.setImpacto(createDto.getImpacto());
        riesgo.setProyecto(proyecto);
        riesgo.setEstado("Pendiente");
        riesgo.setNivel(calculateLevel(createDto.getProbabilidad(), createDto.getImpacto()));

        Riesgo savedRisk = riesgoRepository.save(riesgo);
        savedRisk.setCodigo("R-" + String.format("%03d", savedRisk.getId()));
        riesgoRepository.save(savedRisk);

        return convertToResponseDto(savedRisk);
    }

    @Override
    @Transactional
    public RiesgoResponseDTO updateRisk(RiesgoUpdateDTO updateDto) {
        Riesgo riesgo = riesgoRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + updateDto.getId()));

        if (Boolean.TRUE.equals(riesgo.getProyecto().getCerrado())) {
            throw new ForbiddenException("No se pueden editar riesgos de un proyecto cerrado.");
        }

        riesgo.setDescripcion(updateDto.getDescripcion());
        riesgo.setProbabilidad(updateDto.getProbabilidad());
        riesgo.setImpacto(updateDto.getImpacto());
        riesgo.setNivel(calculateLevel(updateDto.getProbabilidad(), updateDto.getImpacto()));

        return convertToResponseDto(riesgoRepository.save(riesgo));
    }

    @Override
    @Transactional
    public RiesgoResponseDTO treatRisk(RiesgoTratamientoDTO treatmentDto) {
        Riesgo riesgo = riesgoRepository.findById(treatmentDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + treatmentDto.getId()));

        if (Boolean.TRUE.equals(riesgo.getProyecto().getCerrado())) {
            throw new ForbiddenException("No se pueden tratar riesgos de un proyecto cerrado.");
        }

        riesgo.setTratamiento(treatmentDto.getTratamiento());
        riesgo.setEstado("Tratado");

        return convertToResponseDto(riesgoRepository.save(riesgo));
    }

    private String calculateLevel(int prob, int imp) {
        int score = prob * imp;
        if (score >= 20) return "Crítico";
        if (score >= 12) return "Alto";
        if (score >= 6) return "Moderado";
        return "Bajo";
    }

    private String getColorTag(String level) {
        switch (level) {
            case "Crítico": return "#DC3545"; // Rojo
            case "Alto": return "#FD7E14";    // Naranja
            case "Moderado": return "#0D6EFD"; // Azul
            case "Bajo": return "#198754";    // Verde
            default: return "#6C757D";
        }
    }

    private RiesgoResponseDTO convertToResponseDto(Riesgo riesgo) {
        RiesgoResponseDTO dto = new RiesgoResponseDTO();
        dto.setId(riesgo.getId());
        dto.setCodigo(riesgo.getCodigo());
        dto.setDescripcion(riesgo.getDescripcion());
        dto.setProbabilidad(riesgo.getProbabilidad());
        dto.setImpacto(riesgo.getImpacto());
        dto.setNivel(riesgo.getNivel());
        dto.setColorTag(getColorTag(riesgo.getNivel()));
        dto.setTratamiento(riesgo.getTratamiento());
        dto.setEstado(riesgo.getEstado());
        dto.setFechaActualizacion(riesgo.getFechaActualizacion());
        return dto;
    }
}
