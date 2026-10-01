package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoSolucionAdjuntoDTO;
import com.proyecta.api_gestion.config.PublicUrlProperties;
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
import com.proyecta.api_gestion.service.PublicEvidenceUrlSigner;
import com.proyecta.api_gestion.service.report.RiesgoExcelExporter;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RiesgoServiceImpl implements IRiesgoService {

    private static final Logger log = LoggerFactory.getLogger(RiesgoServiceImpl.class);

    private static final String RISK_CODE = "riskCode";
    private static final String RISK_LEVEL = "riskLevel";
    private static final String PROJECT_NAME = "projectName";
    private static final String RECIPIENTS = "recipients";
    private static final String ACTOR_SYSTEM = "system";
    private static final String MSG_RIESGO_NO_ENCONTRADO = "Riesgo no encontrado con ID: ";
    private static final String USUARIO_DESCONOCIDO = "desconocido";
    private static final String NIVEL_MODERADO = "MODERADO";
    private static final String NIVEL_EXTREMO = "EXTREMO";
    private static final String NIVEL_CUATRO = "CUATRO";
    private static final String NIVEL_CINCO = "CINCO";
    private static final String COLOR_BAJO = "#22c55e";
    private static final String COLOR_MODERADO = "#f59e0b";
    private static final String COLOR_ALTO = "#ef4444";
    private static final String COLOR_EXTREMO = "#dc2626";

    private static final List<MatrixRule> MATRIX_RULES = List.of(
            new MatrixRule("UNO", "UNO", "BAJO", COLOR_BAJO),
            new MatrixRule("UNO", "DOS", "BAJO", COLOR_BAJO),
            new MatrixRule("UNO", "TRES", "BAJO", COLOR_BAJO),
            new MatrixRule("UNO", NIVEL_CUATRO, NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("UNO", NIVEL_CINCO, NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("DOS", "UNO", "BAJO", COLOR_BAJO),
            new MatrixRule("DOS", "DOS", "BAJO", COLOR_BAJO),
            new MatrixRule("DOS", "TRES", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("DOS", NIVEL_CUATRO, NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("DOS", NIVEL_CINCO, "ALTO", COLOR_ALTO),
            new MatrixRule("TRES", "UNO", "BAJO", COLOR_BAJO),
            new MatrixRule("TRES", "DOS", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("TRES", "TRES", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule("TRES", NIVEL_CUATRO, "ALTO", COLOR_ALTO),
            new MatrixRule("TRES", NIVEL_CINCO, "ALTO", COLOR_ALTO),
            new MatrixRule(NIVEL_CUATRO, "UNO", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule(NIVEL_CUATRO, "DOS", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule(NIVEL_CUATRO, "TRES", "ALTO", COLOR_ALTO),
            new MatrixRule(NIVEL_CUATRO, NIVEL_CUATRO, "ALTO", COLOR_ALTO),
            new MatrixRule(NIVEL_CUATRO, NIVEL_CINCO, NIVEL_EXTREMO, COLOR_EXTREMO),
            new MatrixRule(NIVEL_CINCO, "UNO", NIVEL_MODERADO, COLOR_MODERADO),
            new MatrixRule(NIVEL_CINCO, "DOS", "ALTO", COLOR_ALTO),
            new MatrixRule(NIVEL_CINCO, "TRES", "ALTO", COLOR_ALTO),
            new MatrixRule(NIVEL_CINCO, NIVEL_CUATRO, NIVEL_EXTREMO, COLOR_EXTREMO),
            new MatrixRule(NIVEL_CINCO, NIVEL_CINCO, NIVEL_EXTREMO, COLOR_EXTREMO)
    );

    private final RiesgoRepository riesgoRepository;
    private final ProyectoRepository proyectoRepository;
    private final MatrizRiesgoRepository matrizRiesgoRepository;
    private final RiesgoSolucionAdjuntoRepository solucionRepository;
    private final IStorageProvider storageProvider;
    private final NotificationEventPublisherPort notificationPublisher;
    private final RiesgoExcelExporter riesgoExcelExporter;
    private final String publicUrlBase;
    private final PublicEvidenceUrlSigner urlSigner;

    public RiesgoServiceImpl(RiesgoRepository riesgoRepository,
                             ProyectoRepository proyectoRepository,
                             MatrizRiesgoRepository matrizRiesgoRepository,
                             RiesgoSolucionAdjuntoRepository solucionRepository,
                             IStorageProvider storageProvider,
                             NotificationEventPublisherPort notificationPublisher,
                             RiesgoExcelExporter riesgoExcelExporter,
                             PublicUrlProperties publicUrlProperties,
                             PublicEvidenceUrlSigner urlSigner) {
        this.riesgoRepository = riesgoRepository;
        this.proyectoRepository = proyectoRepository;
        this.matrizRiesgoRepository = matrizRiesgoRepository;
        this.solucionRepository = solucionRepository;
        this.storageProvider = storageProvider;
        this.notificationPublisher = notificationPublisher;
        this.riesgoExcelExporter = riesgoExcelExporter;
        this.urlSigner = urlSigner;
        String base = publicUrlProperties.getBase();
        this.publicUrlBase = (base == null) ? "" : base.replaceAll("/+$", "");
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
    public RiesgoCreatedResponseDTO createRisk(String projectId, RiesgoRequestDTO requestDto, Authentication authentication) {
        Proyecto proyecto = cargarProyecto(projectId);
        if (esEstadoCerrado(proyecto)) {
            throw new ForbiddenException("No se pueden agregar riesgos a un proyecto cerrado.");
        }

        Riesgo riesgo = new Riesgo();
        aplicarRequest(riesgo, requestDto, proyecto);
        String resolvedUser = resolveUsername(authentication);
        riesgo.setCreatedBy(resolvedUser);
        log.info("[createRisk] createdBy seteado: '{}' para riesgo en proyecto {}", resolvedUser, projectId);

        Riesgo savedRisk = riesgoRepository.save(riesgo);
        savedRisk.setCodigo("R" + String.format("%02d", savedRisk.getId()));
        riesgoRepository.save(savedRisk);
        var riskRecipients = ProjectNotificationRecipients.resolve(proyecto);
        if (!riskRecipients.isEmpty()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.RISK_CREATED,
                    projectId,
                    resolvedUser,
                    java.util.Map.of(
                            RISK_CODE, savedRisk.getCodigo(),
                            RISK_LEVEL, savedRisk.getNivel() != null ? savedRisk.getNivel() : "",
                            PROJECT_NAME, proyecto.getNombre() != null ? proyecto.getNombre() : "",
                            RECIPIENTS, riskRecipients
                    )));
        }

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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_RIESGO_NO_ENCONTRADO + riesgoId));

        if (!riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }

        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden editar riesgos de un proyecto cerrado.");
        }

        aplicarRequest(riesgo, requestDto, riesgo.getProyecto());
        Riesgo saved = riesgoRepository.save(riesgo);
        var updateRecipients = ProjectNotificationRecipients.resolve(saved.getProyecto());
        if (!updateRecipients.isEmpty()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.RISK_UPDATED,
                    projectId,
                    ACTOR_SYSTEM,
                    java.util.Map.of(
                            RISK_CODE, saved.getCodigo() != null ? saved.getCodigo() : "",
                            RISK_LEVEL, saved.getNivel() != null ? saved.getNivel() : "",
                            PROJECT_NAME, saved.getProyecto().getNombre() != null ? saved.getProyecto().getNombre() : "",
                            RECIPIENTS, updateRecipients
                    )));
        }
        return convertToResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteRisk(String projectId, Integer riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_RIESGO_NO_ENCONTRADO + riesgoId));

        if (!riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }

        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden eliminar riesgos de un proyecto cerrado.");
        }

        String riskCode = riesgo.getCodigo() != null ? riesgo.getCodigo() : String.valueOf(riesgo.getId());
        Proyecto proyectoRiesgo = riesgo.getProyecto();
        riesgoRepository.delete(riesgo);

        var deleteRecipients = ProjectNotificationRecipients.resolve(proyectoRiesgo);
        if (!deleteRecipients.isEmpty()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.RISK_DELETED,
                    projectId,
                    ACTOR_SYSTEM,
                    java.util.Map.of(
                            RISK_CODE, riskCode,
                            PROJECT_NAME, proyectoRiesgo.getNombre() != null ? proyectoRiesgo.getNombre() : "",
                            RECIPIENTS, deleteRecipients
                    )));
        }
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
            String nombreBase = generarNombreBaseSolucion(riesgo.getId(), i);
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

        boolean cambioATratado = false;
        if (riesgo.getEstado() == null || riesgo.getEstado() == EstadoRiesgo.PENDIENTE) {
            riesgo.setEstado(EstadoRiesgo.TRATADO);
            riesgoRepository.save(riesgo);
            cambioATratado = true;
        }

        if (cambioATratado) {
            var treatedRecipients = ProjectNotificationRecipients.resolve(riesgo.getProyecto());
            if (treatedRecipients != null && !treatedRecipients.isEmpty()) {
                notificationPublisher.publish(new NotificationContext(
                        NotificationEventType.RISK_TREATED,
                        projectId,
                        ACTOR_SYSTEM,
                        java.util.Map.of(
                                RISK_CODE, riesgo.getCodigo() != null ? riesgo.getCodigo() : String.valueOf(riesgo.getId()),
                                RISK_LEVEL, riesgo.getNivel() != null ? riesgo.getNivel() : "",
                                PROJECT_NAME, riesgo.getProyecto().getNombre() != null ? riesgo.getProyecto().getNombre() : "",
                                RECIPIENTS, treatedRecipients
                        )));
            }
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

    @Override
    @Transactional(readOnly = true)
    public Resource descargarMatrizExcel(String projectId) {
        List<Riesgo> riesgos = riesgoRepository.findByProyectoId(projectId);
        byte[] bytes = riesgoExcelExporter.buildProjectRiskMatrix(projectId, riesgos);
        return new ByteArrayResource(bytes);
    }

    @Override
    public String construirUrlPublicaSolucion(String projectId, Integer riesgoId, Long solucionId) {
        return urlSigner.appendSignature(
                publicUrlBase + "/api/v1/public/riesgos/" + projectId + "/" + riesgoId + "/soluciones/" + solucionId);
    }

    @Override
    public String construirUrlPublicaSolucionInline(String projectId, Integer riesgoId, Long solucionId) {
        return construirUrlPublicaSolucion(projectId, riesgoId, solucionId) + "?inline=true";
    }

    private void aplicarRequest(Riesgo riesgo, RiesgoRequestDTO requestDto, Proyecto proyecto) {
        riesgo.setProyecto(proyecto);
        riesgo.setDescripcion(requestDto.descripcion());
        riesgo.setProbabilidad(requestDto.probabilidad());
        riesgo.setImpacto(requestDto.impacto());
        if (requestDto.tipoRiesgo() != null) {
            riesgo.setTipoRiesgo(requestDto.tipoRiesgo());
        }
        riesgo.setNivel(parseNivelRiesgo(calcularNivelDesdeMatriz(requestDto.probabilidad(), requestDto.impacto())));
        riesgo.setTratamiento(trimToNull(requestDto.tratamiento()));
        riesgo.setEntidadResponsable(trimToNull(requestDto.entidadResponsable()));
        if (requestDto.estado() == EstadoRiesgo.TRATADO) {
            riesgo.setAccionesMitigacion(trimToNull(requestDto.accionesMitigacion()));
            riesgo.setFechaAccion(requestDto.fechaAccion());
        } else {
            riesgo.setAccionesMitigacion(null);
            riesgo.setFechaAccion(null);
        }
        if (requestDto.estado() != null) {
            riesgo.setEstado(requestDto.estado());
        } else if (riesgo.getEstado() == null) {
            riesgo.setEstado(EstadoRiesgo.PENDIENTE);
        }
    }

    private RiesgoResponseDTO convertToResponseDto(Riesgo riesgo) {
        List<com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO> tratamientosDtos = new ArrayList<>();
        if (riesgo.getTratamientos() != null) {
            for (var tratamiento : riesgo.getTratamientos()) {
                List<com.proyecta.api_gestion.dto.risk.RiesgoTratamientoAdjuntoDTO> adjuntosDtos = new ArrayList<>();
                if (tratamiento.getAdjuntos() != null) {
                    for (var adjunto : tratamiento.getAdjuntos()) {
                        Long tId = tratamiento.getId();
                        Long aId = adjunto.getId();
                        String projectId = riesgo.getProyecto() != null ? riesgo.getProyecto().getId() : null;
                        adjuntosDtos.add(new com.proyecta.api_gestion.dto.risk.RiesgoTratamientoAdjuntoDTO(
                                aId,
                                adjunto.getNombreOriginal(),
                                adjunto.getNombreAlmacenado(),
                                adjunto.getMimeType(),
                                adjunto.getTamanoBytes(),
                                projectId != null
                                        ? "/api/v1/proyectos/" + projectId + "/riesgos/" + riesgo.getId() + "/tratamientos/" + tId + "/adjuntos/" + aId + "/descargar"
                                        : null,
                                adjunto.getFechaCarga()
                        ));
                    }
                }
                tratamientosDtos.add(new com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO(
                        tratamiento.getId(),
                        tratamiento.getIteracion(),
                        tratamiento.getComentario(),
                        tratamiento.getFechaCreacion(),
                        adjuntosDtos
                ));
            }
        }
        return new RiesgoResponseDTO(
                riesgo.getId(),
                riesgo.getCodigo(),
                riesgo.getDescripcion(),
                riesgo.getProbabilidad(),
                riesgo.getImpacto(),
                calcularCalificacionInherente(riesgo.getProbabilidad(), riesgo.getImpacto()),
                riesgo.getNivel(),
                riesgo.getTipoRiesgo(),
                riesgo.getTratamiento(),
                riesgo.getEntidadResponsable(),
                riesgo.getAccionesMitigacion(),
                riesgo.getFechaAccion(),
                riesgo.getEvidenciaIndicador(),
                riesgo.getEstado(),
                riesgo.getFechaActualizacion(),
                riesgo.getSoluciones() == null ? List.of() : riesgo.getSoluciones().stream().map(this::toSolutionDto).toList(),
                tratamientosDtos,
                riesgo.getCreatedBy()
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
            case UNO -> 1;
            case DOS -> 2;
            case TRES -> 3;
            case CUATRO -> 4;
            case CINCO -> 5;
        };
    }

    private Integer escalaImpacto(Impacto impacto) {
        if (impacto == null) {
            return 0;
        }
        return switch (impacto) {
            case UNO -> 1;
            case DOS -> 2;
            case TRES -> 3;
            case CUATRO -> 4;
            case CINCO -> 5;
        };
    }

    private NivelRiesgo parseNivelRiesgo(String nivelStr) {
        try {
            String normalized = nivelStr == null ? null : nivelStr.trim().toUpperCase(Locale.ROOT);
            if (normalized == null || normalized.isBlank()) {
                return NivelRiesgo.BAJO;
            }
            if ("CRITICO".equals(normalized)) {
                normalized = NIVEL_EXTREMO;
            }
            return NivelRiesgo.valueOf(normalized);
        } catch (IllegalArgumentException _) {
            return NivelRiesgo.BAJO;
        }
    }

    private boolean esEstadoCerrado(Proyecto proyecto) {
        if (proyecto.getEstadoConfig() != null) {
            return proyecto.getEstadoConfig().getEsTerminal();
        }
        return EstadoProyecto.CERRADO.equals(proyecto.getEstado()) || EstadoProyecto.CERRADO_FORZOSO.equals(proyecto.getEstado()) || EstadoProyecto.FINALIZADO.equals(proyecto.getEstado());
    }

    private Riesgo cargarRiesgoDelProyecto(String projectId, Integer riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_RIESGO_NO_ENCONTRADO + riesgoId));
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
                throw new BadRequestException("El archivo cargado no es un PDF vǭlido.");
            }
        } catch (IOException _) {
            throw new BadRequestException("No fue posible validar el archivo PDF cargado.");
        }
    }

    private String generarNombreBaseSolucion(Integer riesgoId, int indice) {
        return "riesgo_" + riesgoId + "_solucion_" + System.currentTimeMillis() + "_" + indice + "_" + UUID.randomUUID();
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

    private String resolveUsername(Authentication authentication) {
        if (authentication == null) {
            log.warn("[resolveUsername] authentication es null");
            return USUARIO_DESCONOCIDO;
        }
        Object principal = authentication.getPrincipal();
        log.debug("[resolveUsername] principal class={}, name={}", principal.getClass().getName(), authentication.getName());
        if (principal instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                log.debug("[resolveUsername] resolved from preferred_username: {}", username);
                return username;
            }
            username = jwt.getClaimAsString("email");
            if (username != null && !username.isBlank()) {
                log.debug("[resolveUsername] resolved from email: {}", username);
                return username;
            }
            username = jwt.getSubject();
            if (username != null && !username.isBlank()) {
                log.debug("[resolveUsername] resolved from subject: {}", username);
                return username;
            }
            log.warn("[resolveUsername] JWT no tiene preferred_username, email ni subject");
            return USUARIO_DESCONOCIDO;
        }
        String name = authentication.getName();
        log.debug("[resolveUsername] resolved from authentication.getName(): {}", name);
        return name != null && !name.isBlank() ? name : USUARIO_DESCONOCIDO;
    }

    private record MatrixRule(String probabilidad, String impacto, String nivel, String color) {}
}


