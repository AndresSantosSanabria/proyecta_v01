package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoSolucionAdjuntoDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.Riesgo;
import com.proyecta.api_gestion.model.RiesgoSolucionAdjunto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import com.proyecta.api_gestion.repository.RiesgoSolucionAdjuntoRepository;
import com.proyecta.api_gestion.repository.config.MatrizRiesgoRepository;
import com.proyecta.api_gestion.service.IRiesgoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
    private final RiesgoSolucionAdjuntoRepository solucionRepository;
    private final IStorageProvider storageProvider;
    private final NotificationEventPublisherPort notificationPublisher;

    public RiesgoServiceImpl(RiesgoRepository riesgoRepository,
                             ProyectoRepository proyectoRepository,
                             MatrizRiesgoRepository matrizRiesgoRepository,
                             RiesgoSolucionAdjuntoRepository solucionRepository,
                             IStorageProvider storageProvider,
                             NotificationEventPublisherPort notificationPublisher) {
        this.riesgoRepository = riesgoRepository;
        this.proyectoRepository = proyectoRepository;
        this.matrizRiesgoRepository = matrizRiesgoRepository;
        this.solucionRepository = solucionRepository;
        this.storageProvider = storageProvider;
        this.notificationPublisher = notificationPublisher;
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
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.RISK_CREATED,
                projectId,
                "system",
                java.util.Map.of(
                        "riskCode", savedRisk.getCodigo(),
                        "riskLevel", savedRisk.getNivel(),
                        "projectName", proyecto.getNombre(),
                        "recipients", List.of(proyecto.getCorreoDirector())
                )));

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
        Riesgo saved = riesgoRepository.save(riesgo);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.RISK_UPDATED,
                projectId,
                "system",
                java.util.Map.of(
                        "riskCode", saved.getCodigo(),
                        "riskLevel", saved.getNivel(),
                        "recipients", List.of(saved.getProyecto().getCorreoDirector())
                )));
        return convertToResponseDto(saved);
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
        Riesgo saved = riesgoRepository.save(riesgo);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.RISK_TREATED,
                projectId,
                "system",
                java.util.Map.of(
                        "riskCode", saved.getCodigo(),
                        "riskLevel", saved.getNivel(),
                        "recipients", List.of(saved.getProyecto().getCorreoDirector())
                )));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoSolucionAdjuntoDTO> listarSoluciones(String projectId, Integer riesgoId) {
        Riesgo riesgo = cargarRiesgoDelProyecto(projectId, riesgoId);
        return solucionRepository.findByRiesgo_IdOrderByFechaCargaAsc(riesgo.getId()).stream()
                .map(this::toSolutionDto)
                .toList();
    }

    @Override
    @Transactional
    public List<RiesgoSolucionAdjuntoDTO> agregarSoluciones(String projectId, Integer riesgoId, MultipartFile[] archivos) {
        Riesgo riesgo = cargarRiesgoDelProyecto(projectId, riesgoId);
        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden agregar soluciones a un proyecto cerrado.");
        }

        if (archivos == null || archivos.length == 0) {
            throw new BadRequestException("Debes seleccionar al menos un PDF de solución.");
        }

        List<RiesgoSolucionAdjuntoDTO> resultado = new ArrayList<>();
        for (int i = 0; i < archivos.length; i++) {
            MultipartFile archivo = archivos[i];
            validarPdf(archivo);
            String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
            String nombreBase = generarNombreBaseSolucion(riesgo.getId(), i, nombreOriginal);
            String nombreAlmacenado = storageProvider.storeFile(archivo, "riesgos-soluciones", nombreBase);

            RiesgoSolucionAdjunto adjunto = new RiesgoSolucionAdjunto();
            adjunto.setRiesgo(riesgo);
            adjunto.setNombreOriginal(nombreOriginal);
            adjunto.setNombreAlmacenado(nombreAlmacenado);
            adjunto.setRutaAlmacenamiento("riesgos-soluciones");
            adjunto.setMimeType(detectMimeType(archivo));
            adjunto.setTamanoBytes(archivo.getSize());
            RiesgoSolucionAdjunto guardado = solucionRepository.save(adjunto);
            resultado.add(toSolutionDto(guardado));
        }

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarSolucion(String projectId, Integer riesgoId, Long solucionId) {
        RiesgoSolucionAdjunto adjunto = solucionRepository.findById(solucionId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto de solución no encontrado: " + solucionId));
        if (adjunto.getRiesgo() == null || adjunto.getRiesgo().getId() == null || !adjunto.getRiesgo().getId().equals(riesgoId)) {
            throw new ForbiddenException("El adjunto no pertenece al riesgo solicitado.");
        }
        if (adjunto.getRiesgo().getProyecto() == null || !adjunto.getRiesgo().getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El adjunto no pertenece al proyecto solicitado.");
        }
        return storageProvider.loadFileAsResource(adjunto.getRutaAlmacenamiento(), adjunto.getNombreAlmacenado());
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
                riesgo.getFechaActualizacion(),
                riesgo.getSoluciones() == null ? List.of() : riesgo.getSoluciones().stream().map(this::toSolutionDto).toList()
        );
    }

    private RiesgoSolucionAdjuntoDTO toSolutionDto(RiesgoSolucionAdjunto adjunto) {
        if (adjunto == null) {
            return null;
        }
        Long riesgoId = adjunto.getRiesgo() != null && adjunto.getRiesgo().getId() != null ? adjunto.getRiesgo().getId().longValue() : null;
        Long adjuntoId = adjunto.getId();
        return new RiesgoSolucionAdjuntoDTO(
                adjuntoId,
                adjunto.getNombreOriginal(),
                adjunto.getNombreAlmacenado(),
                adjunto.getMimeType(),
                adjunto.getTamanoBytes(),
                adjuntoId != null && riesgoId != null && adjunto.getRiesgo().getProyecto() != null
                        ? "/api/v1/proyectos/" + adjunto.getRiesgo().getProyecto().getId() + "/riesgos/" + adjunto.getRiesgo().getId() + "/soluciones/" + adjuntoId + "/descargar"
                        : null,
                adjunto.getFechaCarga()
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

    private Riesgo cargarRiesgoDelProyecto(String projectId, Integer riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));
        if (riesgo.getProyecto() == null || !riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }
        return riesgo;
    }

    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Cada archivo de solución debe ser un PDF válido.");
        }
        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException("Solo se permiten archivos PDF para las soluciones.");
        }
        try (var is = archivo.getInputStream()) {
            byte[] encabezado = is.readNBytes(5);
            String firma = new String(encabezado, StandardCharsets.US_ASCII);
            if (!firma.startsWith("%PDF-")) {
                throw new BadRequestException("El archivo cargado no es un PDF válido.");
            }
        } catch (IOException ex) {
            throw new BadRequestException("No fue posible validar el archivo PDF cargado.");
        }
    }

    private String generarNombreBaseSolucion(Integer riesgoId, int indice, String nombreOriginal) {
        return "riesgo_" + riesgoId + "_solucion_" + System.currentTimeMillis() + "_" + indice + "_" + UUID.randomUUID();
    }

    private String extraerExtension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int pos = nombre.lastIndexOf('.');
        return pos > 0 ? nombre.substring(pos).toLowerCase(Locale.ROOT) : "";
    }

    private String detectMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        return "application/pdf";
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
