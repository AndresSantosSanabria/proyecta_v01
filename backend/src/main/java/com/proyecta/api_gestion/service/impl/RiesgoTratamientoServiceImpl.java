package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoAdjuntoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoRequest;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Riesgo;
import com.proyecta.api_gestion.model.RiesgoTratamiento;
import com.proyecta.api_gestion.model.RiesgoTratamientoAdjunto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.repository.RiesgoRepository;
import com.proyecta.api_gestion.repository.RiesgoTratamientoAdjuntoRepository;
import com.proyecta.api_gestion.repository.RiesgoTratamientoRepository;
import com.proyecta.api_gestion.service.IRiesgoTratamientoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RiesgoTratamientoServiceImpl implements IRiesgoTratamientoService {

    private static final Logger log = LoggerFactory.getLogger(RiesgoTratamientoServiceImpl.class);
    private static final int MAX_ARCHIVOS_POR_TRATAMIENTO = 10;

    private final RiesgoTratamientoRepository tratamientoRepository;
    private final RiesgoTratamientoAdjuntoRepository adjuntoRepository;
    private final RiesgoRepository riesgoRepository;
    private final IStorageProvider storageProvider;

    public RiesgoTratamientoServiceImpl(RiesgoTratamientoRepository tratamientoRepository,
                                         RiesgoTratamientoAdjuntoRepository adjuntoRepository,
                                         RiesgoRepository riesgoRepository,
                                         IStorageProvider storageProvider) {
        this.tratamientoRepository = tratamientoRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.riesgoRepository = riesgoRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoTratamientoDTO> listarTratamientos(String projectId, Integer riesgoId) {
        Riesgo riesgo = cargarRiesgoDelProyecto(projectId, riesgoId);
        return tratamientoRepository.findByRiesgo_IdOrderByIteracionDesc(riesgo.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RiesgoTratamientoDTO crearTratamiento(String projectId, Integer riesgoId, RiesgoTratamientoRequest request, MultipartFile[] archivos) {
        Riesgo riesgo = cargarRiesgoDelProyecto(projectId, riesgoId);
        if (esEstadoCerrado(riesgo.getProyecto())) {
            throw new ForbiddenException("No se pueden agregar tratamientos a un proyecto cerrado.");
        }

        long totalExistente = tratamientoRepository.countByRiesgo_Id(riesgo.getId());
        int siguienteIteracion = (int) totalExistente + 1;

        RiesgoTratamiento tratamiento = new RiesgoTratamiento();
        tratamiento.setRiesgo(riesgo);
        tratamiento.setIteracion(siguienteIteracion);
        tratamiento.setComentario(request.comentario());
        RiesgoTratamiento guardado = tratamientoRepository.save(tratamiento);

        if (archivos != null && archivos.length > 0) {
            if (archivos.length > MAX_ARCHIVOS_POR_TRATAMIENTO) {
                throw new BadRequestException("No se pueden adjuntar más de " + MAX_ARCHIVOS_POR_TRATAMIENTO + " archivos por tratamiento.");
            }
            for (int i = 0; i < archivos.length; i++) {
                MultipartFile archivo = archivos[i];
                validarPdf(archivo);
                String nombreOriginal = storageProvider.sanitizeFileName(archivo.getOriginalFilename());
                String nombreBase = "riesgo_" + riesgo.getId() + "_trat_" + siguienteIteracion + "_" + System.currentTimeMillis() + "_" + i + "_" + UUID.randomUUID();
                String nombreAlmacenado = storageProvider.storeFile(archivo, "riesgos-tratamientos", nombreBase);

                RiesgoTratamientoAdjunto adjunto = new RiesgoTratamientoAdjunto();
                adjunto.setTratamiento(guardado);
                adjunto.setNombreOriginal(nombreOriginal);
                adjunto.setNombreAlmacenado(nombreAlmacenado);
                adjunto.setRutaAlmacenamiento("riesgos-tratamientos");
                adjunto.setMimeType(detectMimeType(archivo));
                adjunto.setTamanoBytes(archivo.getSize());
                adjuntoRepository.save(adjunto);
            }
        }

        if (riesgo.getEstado() == null || riesgo.getEstado() == EstadoRiesgo.PENDIENTE) {
            riesgo.setEstado(EstadoRiesgo.TRATADO);
            riesgoRepository.save(riesgo);
        }

        return toDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarAdjunto(String projectId, Integer riesgoId, Long tratamientoId, Long adjuntoId) {
        RiesgoTratamientoAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto de tratamiento no encontrado: " + adjuntoId));

        if (adjunto.getTratamiento() == null || !adjunto.getTratamiento().getId().equals(tratamientoId)) {
            throw new ForbiddenException("El adjunto no pertenece al tratamiento especificado.");
        }
        if (adjunto.getTratamiento().getRiesgo() == null || !adjunto.getTratamiento().getRiesgo().getId().equals(riesgoId)) {
            throw new ForbiddenException("El adjunto no pertenece al riesgo especificado.");
        }
        if (adjunto.getTratamiento().getRiesgo().getProyecto() == null || !adjunto.getTratamiento().getRiesgo().getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El adjunto no pertenece al proyecto especificado.");
        }

        return storageProvider.loadFileAsResource(adjunto.getRutaAlmacenamiento(), adjunto.getNombreAlmacenado());
    }

    private RiesgoTratamientoDTO toDto(RiesgoTratamiento tratamiento) {
        List<RiesgoTratamientoAdjuntoDTO> adjuntosDtos = new ArrayList<>();
        if (tratamiento.getAdjuntos() != null) {
            for (RiesgoTratamientoAdjunto adjunto : tratamiento.getAdjuntos()) {
                Long riesgoId = tratamiento.getRiesgo() != null ? tratamiento.getRiesgo().getId().longValue() : null;
                Long tratamientoId = tratamiento.getId();
                Long adjuntoId = adjunto.getId();
                String projectId = tratamiento.getRiesgo() != null && tratamiento.getRiesgo().getProyecto() != null
                        ? tratamiento.getRiesgo().getProyecto().getId() : null;

                adjuntosDtos.add(new RiesgoTratamientoAdjuntoDTO(
                        adjuntoId,
                        adjunto.getNombreOriginal(),
                        adjunto.getNombreAlmacenado(),
                        adjunto.getMimeType(),
                        adjunto.getTamanoBytes(),
                        projectId != null && riesgoId != null && tratamientoId != null
                                ? "/api/v1/proyectos/" + projectId + "/riesgos/" + riesgoId + "/tratamientos/" + tratamientoId + "/adjuntos/" + adjuntoId + "/descargar"
                                : null,
                        adjunto.getFechaCarga()
                ));
            }
        }
        return new RiesgoTratamientoDTO(
                tratamiento.getId(),
                tratamiento.getIteracion(),
                tratamiento.getComentario(),
                tratamiento.getFechaCreacion(),
                adjuntosDtos
        );
    }

    private Riesgo cargarRiesgoDelProyecto(String projectId, Integer riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));
        if (riesgo.getProyecto() == null || !riesgo.getProyecto().getId().equals(projectId)) {
            throw new ForbiddenException("El riesgo no pertenece al proyecto especificado.");
        }
        return riesgo;
    }

    private boolean esEstadoCerrado(com.proyecta.api_gestion.model.Proyecto proyecto) {
        if (proyecto.getEstadoConfig() != null) {
            return proyecto.getEstadoConfig().getEsTerminal();
        }
        return EstadoProyecto.CERRADO.equals(proyecto.getEstado()) || EstadoProyecto.CERRADO_FORZOSO.equals(proyecto.getEstado()) || EstadoProyecto.FINALIZADO.equals(proyecto.getEstado());
    }

    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Cada archivo adjunto debe ser un PDF válido.");
        }
        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException("Solo se permiten archivos PDF como adjuntos de tratamiento.");
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

    private String detectMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        return "application/pdf";
    }
}
