package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
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
import com.proyecta.api_gestion.repository.config.MatrizRiesgoRepository;
import com.proyecta.api_gestion.service.IRiesgoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class RiesgoServiceImpl implements IRiesgoService {

    private static final List<MatrixRule> MATRIX_RULES = List.of(
            new MatrixRule("BAJA", "BAJO", "BAJO", "#22c55e"),
            new MatrixRule("BAJA", "MEDIO", "BAJO", "#22c55e"),
            new MatrixRule("BAJA", "ALTO", "MODERADO", "#f59e0b"),
            new MatrixRule("MEDIA", "BAJO", "BAJO", "#22c55e"),
            new MatrixRule("MEDIA", "MEDIO", "MODERADO", "#f59e0b"),
            new MatrixRule("MEDIA", "ALTO", "ALTO", "#ef4444"),
            new MatrixRule("ALTA", "BAJO", "MODERADO", "#f59e0b"),
            new MatrixRule("ALTA", "MEDIO", "ALTO", "#ef4444"),
            new MatrixRule("ALTA", "ALTO", "EXTREMO", "#dc2626")
    );

    private final RiesgoRepository riesgoRepository;
    private final ProyectoRepository proyectoRepository;
    private final MatrizRiesgoRepository matrizRiesgoRepository;

    public RiesgoServiceImpl(RiesgoRepository riesgoRepository,
                             ProyectoRepository proyectoRepository,
                             MatrizRiesgoRepository matrizRiesgoRepository) {
        this.riesgoRepository = riesgoRepository;
        this.proyectoRepository = proyectoRepository;
        this.matrizRiesgoRepository = matrizRiesgoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RiesgoListResponseDTO getRisksByProject(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        List<RiesgoResponseDTO> riesgos = riesgoRepository.findByProyectoId(projectId).stream()
                .map(this::convertToResponseDto)
                .toList();

        return new RiesgoListResponseDTO(projectId, proyecto.getNombre(), riesgos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatrizRiesgoDTO> getRiskMatrix() {
        List<MatrizRiesgoDTO> catalogo = matrizRiesgoRepository.findAllByOrderByIdAsc().stream()
                .map(rule -> new MatrizRiesgoDTO(rule.getProbabilidad(), rule.getImpacto(), rule.getNivelResultante(), rule.getColor()))
                .toList();

        if (!catalogo.isEmpty()) {
            return catalogo;
        }

        return MATRIX_RULES.stream()
                .map(rule -> new MatrizRiesgoDTO(rule.probabilidad(), rule.impacto(), rule.nivel(), rule.color()))
                .toList();
    }

    @Override
    @Transactional
    public RiesgoCreatedResponseDTO createRisk(String projectId, RiesgoRequestDTO requestDto) {
        Proyecto proyecto = cargarProyecto(projectId);
        if (esEstadoCerrado(proyecto)) {
            throw new ForbiddenException("No se pueden agregar riesgos a un proyecto cerrado.");
        }

        Riesgo riesgo = new Riesgo();
        aplicarRequest(riesgo, requestDto, proyecto);

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

        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden editar riesgos de un proyecto cerrado.");
        }

        aplicarRequest(riesgo, requestDto, riesgo.getProyecto());
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

        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden eliminar riesgos de un proyecto cerrado.");
        }

        riesgoRepository.delete(riesgo);
    }

    @Override
    @Transactional
    public void verificarTratamiento(String projectId, Integer riesgoId, String verificacion) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));

        if (!riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }

        riesgo.setTratamiento((riesgo.getTratamiento() == null ? "" : riesgo.getTratamiento()) + "\nVERIFICACION: " + verificacion);
        riesgo.setEstado(EstadoRiesgo.TRATADO);
        riesgoRepository.save(riesgo);
    }

    private void aplicarRequest(Riesgo riesgo, RiesgoRequestDTO requestDto, Proyecto proyecto) {
        riesgo.setProyecto(proyecto);
        riesgo.setCategoriaRiesgo(trimToNull(requestDto.categoriaRiesgo()));
        riesgo.setDescripcion(requestDto.descripcion());
        riesgo.setCausa(trimToNull(requestDto.causa()));
        riesgo.setConsecuencia(trimToNull(requestDto.consecuencia()));
        riesgo.setProbabilidad(requestDto.probabilidad());
        riesgo.setImpacto(requestDto.impacto());
        riesgo.setNivel(parseNivelRiesgo(calcularNivelDesdeMatriz(requestDto.probabilidad(), requestDto.impacto())));
        riesgo.setControlesExistentes(trimToNull(requestDto.controlesExistentes()));
        riesgo.setTipoControl(trimToNull(requestDto.tipoControl()));
        riesgo.setValoracionControl(trimToNull(requestDto.valoracionControl()));
        riesgo.setProbabilidadResidual(requestDto.probabilidadResidual());
        riesgo.setImpactoResidual(requestDto.impactoResidual());
        riesgo.setNivelResidual(
                requestDto.probabilidadResidual() != null && requestDto.impactoResidual() != null
                        ? parseNivelRiesgo(calcularNivelDesdeMatriz(requestDto.probabilidadResidual(), requestDto.impactoResidual()))
                        : null
        );
        riesgo.setTratamiento(requestDto.tratamiento());
        riesgo.setAccionesMitigacion(trimToNull(requestDto.accionesMitigacion()));
        riesgo.setEntidadResponsable(trimToNull(requestDto.entidadResponsable()));
        riesgo.setRolResponsable(trimToNull(requestDto.rolResponsable()));
        riesgo.setFechaAccion(requestDto.fechaAccion());
        riesgo.setEvidenciaIndicador(trimToNull(requestDto.evidenciaIndicador()));
        riesgo.setEstado(requestDto.estado());
    }

    private RiesgoResponseDTO convertToResponseDto(Riesgo riesgo) {
        return new RiesgoResponseDTO(
                riesgo.getId(),
                riesgo.getCodigo(),
                riesgo.getCategoriaRiesgo(),
                riesgo.getDescripcion(),
                riesgo.getCausa(),
                riesgo.getConsecuencia(),
                riesgo.getProbabilidad(),
                riesgo.getImpacto(),
                calcularCalificacionInherente(riesgo.getProbabilidad(), riesgo.getImpacto()),
                riesgo.getNivel(),
                riesgo.getControlesExistentes(),
                riesgo.getTipoControl(),
                riesgo.getValoracionControl(),
                riesgo.getProbabilidadResidual(),
                riesgo.getImpactoResidual(),
                riesgo.getNivelResidual(),
                riesgo.getTratamiento(),
                riesgo.getAccionesMitigacion(),
                riesgo.getEntidadResponsable(),
                riesgo.getRolResponsable(),
                riesgo.getFechaAccion(),
                riesgo.getEvidenciaIndicador(),
                riesgo.getEstado(),
                riesgo.getFechaActualizacion()
        );
    }

    private String calcularNivelDesdeMatriz(Probabilidad prob, Impacto imp) {
        String probabilidad = prob == null ? null : prob.name();
        String impacto = imp == null ? null : imp.name();
        if (probabilidad == null || impacto == null) {
            throw new BadRequestException("La probabilidad e impacto son obligatorios para calcular el nivel de riesgo.");
        }

        return matrizRiesgoRepository.findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(probabilidad, impacto)
                .map(com.proyecta.api_gestion.model.config.MatrizRiesgo::getNivelResultante)
                .orElseGet(() -> MATRIX_RULES.stream()
                        .filter(rule -> rule.probabilidad().equalsIgnoreCase(probabilidad) && rule.impacto().equalsIgnoreCase(impacto))
                        .map(MatrixRule::nivel)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("No existe una formula de matriz de riesgo para la combinacion enviada.")));
    }

    private Integer calcularCalificacionInherente(Probabilidad prob, Impacto imp) {
        return escalaProbabilidad(prob) + escalaImpacto(imp);
    }

    private Integer escalaProbabilidad(Probabilidad probabilidad) {
        if (probabilidad == null) {
            return 0;
        }
        return switch (probabilidad) {
            case BAJA -> 1;
            case MEDIA -> 2;
            case ALTA -> 3;
        };
    }

    private Integer escalaImpacto(Impacto impacto) {
        if (impacto == null) {
            return 0;
        }
        return switch (impacto) {
            case BAJO -> 1;
            case MEDIO -> 2;
            case ALTO -> 3;
        };
    }

    private NivelRiesgo parseNivelRiesgo(String nivelStr) {
        try {
            String normalized = nivelStr == null ? null : nivelStr.trim().toUpperCase(Locale.ROOT);
            if (normalized == null || normalized.isBlank()) {
                return NivelRiesgo.BAJO;
            }
            if ("CRITICO".equals(normalized)) {
                normalized = "EXTREMO";
            }
            return NivelRiesgo.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return NivelRiesgo.BAJO;
        }
    }

    private boolean esEstadoCerrado(Proyecto proyecto) {
        if (proyecto.getEstadoConfig() != null) {
            return proyecto.getEstadoConfig().getEsTerminal();
        }
        return EstadoProyecto.CERRADO.equals(proyecto.getEstado()) || EstadoProyecto.FINALIZADO.equals(proyecto.getEstado());
    }

    private Proyecto cargarProyecto(String projectId) {
        return proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record MatrixRule(String probabilidad, String impacto, String nivel, String color) {}
}
