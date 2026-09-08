package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.model.enums.DocumentoVersionEstado;
import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.interfaces.ProjectEvidenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ProjectEvidenceServiceImpl implements ProjectEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ProjectEvidenceServiceImpl.class);

    private static final Map<String, String> DOC_TYPE_NAMES = Map.of(
            "VIABILIZACION", "Documento de viabilidad",
            "ACTA_CONSTITUCION", "Acta de constitucion",
            "CRONOGRAMA", "Cronograma del proyecto",
            "PLAN_COMUNICACIONES", "Plan de comunicaciones"
    );

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final DocumentoProyectoVersionRepository documentoProyectoVersionRepository;
    private final RiesgoRepository riesgoRepository;
    private final RiesgoSolucionAdjuntoRepository riesgoSolucionAdjuntoRepository;
    private final EntregableCambioFechaRepository entregableCambioFechaRepository;
    private final EntregableCambioDescripcionRepository entregableCambioDescripcionRepository;
    private final DocumentoDinamicoRepository documentoDinamicoRepository;
    private final ActaCierreRepository actaCierreRepository;
    private final DocumentoVersionRepository documentoVersionRepository;

    public ProjectEvidenceServiceImpl(ProyectoRepository proyectoRepository,
                                      EntregableRepository entregableRepository,
                                      DocumentoProyectoVersionRepository documentoProyectoVersionRepository,
                                      RiesgoRepository riesgoRepository,
                                      RiesgoSolucionAdjuntoRepository riesgoSolucionAdjuntoRepository,
                                      EntregableCambioFechaRepository entregableCambioFechaRepository,
                                      EntregableCambioDescripcionRepository entregableCambioDescripcionRepository,
                                      DocumentoDinamicoRepository documentoDinamicoRepository,
                                      ActaCierreRepository actaCierreRepository,
                                      DocumentoVersionRepository documentoVersionRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.documentoProyectoVersionRepository = documentoProyectoVersionRepository;
        this.riesgoRepository = riesgoRepository;
        this.riesgoSolucionAdjuntoRepository = riesgoSolucionAdjuntoRepository;
        this.entregableCambioFechaRepository = entregableCambioFechaRepository;
        this.entregableCambioDescripcionRepository = entregableCambioDescripcionRepository;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.actaCierreRepository = actaCierreRepository;
        this.documentoVersionRepository = documentoVersionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectEvidenceDTO> listarEvidencias(String proyectoId, String categoria) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        boolean shouldCollectAll = (categoria == null || categoria.isBlank() || "TODOS".equals(categoria));

        if (shouldCollectAll || "DOCUMENTO_PROYECTO".equals(categoria)) {
            safeAddAll(evidencias, () -> colDocumentosProyecto(proyectoId), "DOCUMENTO_PROYECTO");
        }
        if (shouldCollectAll || "DOCUMENTO_PROYECTO_AVANZADO".equals(categoria)) {
            safeAddAll(evidencias, () -> colDocumentosProyectoAvanzado(proyecto), "DOCUMENTO_PROYECTO_AVANZADO");
        }
        if (shouldCollectAll || "DOCUMENTO_DINAMICO".equals(categoria)) {
            safeAddAll(evidencias, () -> colDocumentosDinamicos(proyectoId), "DOCUMENTO_DINAMICO");
        }
        if (shouldCollectAll || "EVIDENCIA_ENTREGABLE".equals(categoria)) {
            safeAddAll(evidencias, () -> colEvidenciasEntregables(proyectoId), "EVIDENCIA_ENTREGABLE");
        }
        if (shouldCollectAll || "CRONOGRAMA".equals(categoria)) {
            safeAddAll(evidencias, () -> colCronograma(proyecto), "CRONOGRAMA");
        }
        if (shouldCollectAll || "RIESGO".equals(categoria)) {
            safeAddAll(evidencias, () -> colSolucionesRiesgos(proyectoId), "RIESGO");
        }
        if (shouldCollectAll || "MATRIZ_RIESGOS".equals(categoria)) {
            safeAddAll(evidencias, () -> colMatrizRiesgos(proyectoId), "MATRIZ_RIESGOS");
        }
        if (shouldCollectAll || "CAMBIO_FECHA".equals(categoria)) {
            safeAddAll(evidencias, () -> colCambiosFecha(proyectoId), "CAMBIO_FECHA");
        }
        if (shouldCollectAll || "CAMBIO_DESCRIPCION".equals(categoria)) {
            safeAddAll(evidencias, () -> colCambiosDescripcion(proyectoId), "CAMBIO_DESCRIPCION");
        }
        if (shouldCollectAll || "ACTA_CIERRE".equals(categoria)) {
            safeAddAll(evidencias, () -> colActaCierre(proyectoId), "ACTA_CIERRE");
        }

        return evidencias;
    }

    private void safeAddAll(List<ProjectEvidenceDTO> target, Supplier<List<ProjectEvidenceDTO>> source, String categoria) {
        try {
            List<ProjectEvidenceDTO> result = source.get();
            if (result != null) {
                target.addAll(result);
            }
        } catch (Exception e) {
            log.warn("Error al colectar evidencias de categoria {}: {}", categoria, e.getMessage(), e);
        }
    }

    private List<ProjectEvidenceDTO> colDocumentosProyecto(String proyectoId) {
        List<DocumentoProyectoVersion> versiones = documentoProyectoVersionRepository
                .findByProyectoIdOrderBySubidoEnDesc(proyectoId);

        return versiones.stream()
                .filter(v -> DocumentoProyectoVersionEstado.ACTUAL.equals(v.getEstado()))
                .map(v -> {
                    String codigo = v.getTipoDocumento();
                    String nombreDisplay = DOC_TYPE_NAMES.getOrDefault(codigo, codigo);
                    String urlDescarga = "/api/v1/proyectos/" + proyectoId + "/documentos/" + codigo + "/descargar";
                    return new ProjectEvidenceDTO(
                            "doc-" + codigo,
                            "DOCUMENTO_PROYECTO",
                            nombreDisplay,
                            v.getNombreArchivoOriginal(),
                            urlDescarga,
                            v.getSubidoEn() != null ? v.getSubidoEn().toLocalDate() : null,
                            null,
                            null,
                            "CARGADO",
                            "CARGADO",
                            v.getSubidoPor(),
                            nombreDisplay,
                            codigo,
                            null,
                            null,
                            null,
                            v.getObservacion(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            v.getTamanoBytes(),
                            v.getMimeType(),
                            null,
                            null,
                            null
                    );
                })
                .toList();
    }

    private List<ProjectEvidenceDTO> colDocumentosProyectoAvanzado(Proyecto proyecto) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();
        String proyectoId = proyecto.getId();

        List<Map.Entry<String, String>> proyectoPdfFields = List.of(
                Map.entry("VIABILIZACION", proyecto.getViabilizacionPdf()),
                Map.entry("ACTA_CONSTITUCION", proyecto.getActaConstitucionPdf()),
                Map.entry("PLAN_COMUNICACIONES", proyecto.getPlanComunicacionesPdf())
        );

        for (Map.Entry<String, String> entry : proyectoPdfFields) {
            String codigo = entry.getKey();
            String pdfPath = entry.getValue();

            if (pdfPath == null || pdfPath.isBlank()) {
                continue;
            }

            String nombreDisplay = DOC_TYPE_NAMES.getOrDefault(codigo, codigo);
            String nombreArchivo = extractFileName(pdfPath);
            String urlDescarga = "/api/v1/proyectos/" + proyectoId + "/documentos/" + codigo + "/descargar";

            evidencias.add(new ProjectEvidenceDTO(
                    "doc-proy-" + codigo,
                    "DOCUMENTO_PROYECTO_AVANZADO",
                    nombreDisplay,
                    nombreArchivo,
                    urlDescarga,
                    null,
                    null,
                    null,
                    "CARGADO",
                    "CARGADO",
                    null,
                    nombreDisplay,
                    codigo,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colDocumentosDinamicos(String proyectoId) {
        List<DocumentoDinamico> documentos = documentoDinamicoRepository
                .findByProyectoIdOrderByFechaCargaDesc(proyectoId);

        return documentos.stream()
                .map(doc -> {
                    String nombreDisplay = doc.getTipoDocumento() != null ? doc.getTipoDocumento() : "Documento dinamico";
                    String urlDescarga = doc.getUrlDescarga() != null
                            ? doc.getUrlDescarga()
                            : "/api/v1/proyectos/" + proyectoId + "/documentos-dinamicos/" + doc.getId() + "/descargar";

                    return new ProjectEvidenceDTO(
                            "doc-din-" + doc.getId(),
                            "DOCUMENTO_DINAMICO",
                            nombreDisplay,
                            doc.getNombreOriginal(),
                            urlDescarga,
                            doc.getFechaCarga() != null ? doc.getFechaCarga().toLocalDate() : null,
                            null,
                            null,
                            "CARGADO",
                            "CARGADO",
                            null,
                            nombreDisplay,
                            doc.getTipoDocumento(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            doc.getTamanoBytes(),
                            doc.getMimeType(),
                            null,
                            null,
                            null
                    );
                })
                .toList();
    }

    private List<ProjectEvidenceDTO> colEvidenciasEntregables(String proyectoId) {
        List<Entregable> entregables = entregableRepository.findByProyectoId(proyectoId);
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Entregable entregable : entregables) {
            if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
                continue;
            }

            String faseNombre = null;
            String hitoNombre = null;
            if (entregable.getHito() != null) {
                hitoNombre = entregable.getHito().getNombre();
                if (entregable.getHito().getFase() != null) {
                    faseNombre = entregable.getHito().getFase().getNombre();
                }
            }

            String urlEvidencia = "/api/v1/proyectos/" + proyectoId
                    + "/avance/entregables/" + entregable.getId() + "/evidencia";

            String estadoCodigo = mapEstado(entregable);
            String estadoDisplay = mapEstadoDisplay(estadoCodigo);

            evidencias.add(new ProjectEvidenceDTO(
                    "ev-" + entregable.getId(),
                    "EVIDENCIA_ENTREGABLE",
                    entregable.getArchivoPdf(),
                    entregable.getArchivoPdf(),
                    urlEvidencia,
                    entregable.getFechaEntregaReal(),
                    entregable.getFechaEntregaReal(),
                    entregable.getFechaLimite(),
                    estadoDisplay,
                    estadoCodigo,
                    null,
                    "Evidencia de avance",
                    null,
                    faseNombre,
                    hitoNombre,
                    entregable.getNombre(),
                    entregable.getDescripcion(),
                    entregable.getObservacionRevision(),
                    entregable.getId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colCronograma(Proyecto proyecto) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        if (proyecto.getCronogramaPdf() != null && !proyecto.getCronogramaPdf().isBlank()) {
            String nombreArchivo = extractFileName(proyecto.getCronogramaPdf());
            String urlDescarga = "/api/v1/proyectos/" + proyecto.getId() + "/cronograma/descargar";

            evidencias.add(new ProjectEvidenceDTO(
                    "crono-" + proyecto.getId(),
                    "CRONOGRAMA",
                    "Cronograma del proyecto",
                    nombreArchivo,
                    urlDescarga,
                    null,
                    null,
                    null,
                    "CARGADO",
                    "CARGADO",
                    null,
                    "Cronograma PDF",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colSolucionesRiesgos(String proyectoId) {
        List<Riesgo> riesgos = riesgoRepository.findByProyectoId(proyectoId);
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Riesgo riesgo : riesgos) {
            if (riesgo.getSoluciones() == null) {
                continue;
            }
            for (RiesgoSolucionAdjunto solucion : riesgo.getSoluciones()) {
                String urlDescarga = "/api/v1/proyectos/" + proyectoId
                        + "/riesgos/" + riesgo.getId()
                        + "/soluciones/" + solucion.getId() + "/descargar";

                evidencias.add(new ProjectEvidenceDTO(
                        "riesgo-sol-" + solucion.getId(),
                        "RIESGO",
                        solucion.getNombreOriginal() != null ? solucion.getNombreOriginal() : "Solucion " + solucion.getId(),
                        solucion.getNombreOriginal(),
                        urlDescarga,
                        solucion.getFechaCarga() != null ? solucion.getFechaCarga().toLocalDate() : null,
                        null,
                        null,
                        "CARGADO",
                        "CARGADO",
                        null,
                        "Solucion de riesgo",
                        null,
                        null,
                        null,
                        null,
                        "Riesgo: " + (riesgo.getCodigo() != null ? riesgo.getCodigo() : riesgo.getId()) + " - " + truncate(riesgo.getDescripcion(), 80),
                        null,
                        null,
                        riesgo.getId().longValue(),
                        solucion.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        solucion.getTamanoBytes(),
                        solucion.getMimeType(),
                        null,
                        null,
                        null
                ));
            }
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colMatrizRiesgos(String proyectoId) {
        List<Riesgo> riesgos = riesgoRepository.findByProyectoId(proyectoId);
        log.info("colMatrizRiesgos: proyectoId={}, riesgos encontrados={}", proyectoId, riesgos.size());
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Riesgo riesgo : riesgos) {
            String nivelDisplay = riesgo.getNivel() != null ? riesgo.getNivel().name() : "SIN_NIVEL";
            String estadoDisplay = riesgo.getEstado() != null ? riesgo.getEstado().name() : "PENDIENTE";
            String descripcion = "Riesgo: "
                    + (riesgo.getCodigo() != null ? riesgo.getCodigo() : riesgo.getId())
                    + " - " + truncate(riesgo.getDescripcion(), 80);

            int solucionesCount = (riesgo.getSoluciones() != null) ? riesgo.getSoluciones().size() : 0;

            String usuarioResponsable = riesgo.getCreatedBy() != null && !riesgo.getCreatedBy().isBlank()
                    ? riesgo.getCreatedBy() : "Sin usuario";

            evidencias.add(new ProjectEvidenceDTO(
                    "riesgo-matriz-" + riesgo.getId(),
                    "MATRIZ_RIESGOS",
                    truncate(riesgo.getDescripcion(), 100),
                    riesgo.getCodigo(),
                    null,
                    riesgo.getFechaActualizacion() != null ? riesgo.getFechaActualizacion().toLocalDate() : null,
                    null,
                    null,
                    nivelDisplay,
                    nivelDisplay,
                    usuarioResponsable,
                    "Matriz de riesgos",
                    null,
                    null,
                    null,
                    null,
                    descripcion,
                    null,
                    null,
                    null,
                    riesgo.getId().longValue(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    solucionesCount
            ));
        }

        log.info("colMatrizRiesgos: evidencias generadas={}", evidencias.size());
        return evidencias;
    }

    private List<ProjectEvidenceDTO> colCambiosFecha(String proyectoId) {
        List<Entregable> entregables = entregableRepository.findByProyectoId(proyectoId);
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Entregable entregable : entregables) {
            List<EntregableCambioFecha> cambios = entregableCambioFechaRepository
                    .findByEntregableIdOrderByCreadoEnDesc(entregable.getId());

            String faseNombre = null;
            String hitoNombre = null;
            if (entregable.getHito() != null) {
                hitoNombre = entregable.getHito().getNombre();
                if (entregable.getHito().getFase() != null) {
                    faseNombre = entregable.getHito().getFase().getNombre();
                }
            }

            for (EntregableCambioFecha cambio : cambios) {
                String urlDescarga = null;
                if (cambio.getArchivoPdf() != null && !cambio.getArchivoPdf().isBlank()) {
                    urlDescarga = "/api/v1/proyectos/" + proyectoId
                            + "/entregables/cambios-fecha/" + cambio.getId() + "/descargar";
                }

                evidencias.add(new ProjectEvidenceDTO(
                        "cf-" + cambio.getId() + "-" + proyectoId,
                        "CAMBIO_FECHA",
                        "Cambio de fecha - " + (entregable.getNombre() != null ? entregable.getNombre() : ""),
                        cambio.getNombreOriginal(),
                        urlDescarga,
                        cambio.getCreadoEn() != null ? cambio.getCreadoEn().toLocalDate() : null,
                        null,
                        null,
                        "COMPLETADO",
                        "COMPLETADO",
                        cambio.getUsuario(),
                        "Cambio de fecha",
                        null,
                        faseNombre,
                        hitoNombre,
                        entregable.getNombre(),
                        cambio.getJustificacion(),
                        null,
                        entregable.getId(),
                        cambio.getId(),
                        null,
                        null,
                        cambio.getArchivoPdf(),
                        cambio.getFechaAnterior() != null ? cambio.getFechaAnterior().toString() : null,
                        cambio.getFechaNueva() != null ? cambio.getFechaNueva().toString() : null,
                        cambio.getJustificacion(),
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        return evidencias;
    }

    private String mapEstado(Entregable entregable) {
        if (entregable.getEstado() == null) return "PENDIENTE";
        return switch (entregable.getEstado()) {
            case APROBADO -> "APROBADO";
            case COMPLETADO -> "COMPLETADO";
            case EN_PROCESO -> "EN_PROCESO";
            case RECHAZADO -> "RECHAZADO";
            case PENDIENTE -> "PENDIENTE";
            case ATRASADO -> "ATRASADO";
        };
    }

    private String mapEstadoDisplay(String estadoCodigo) {
        if (estadoCodigo == null) return "Pendiente";
        return switch (estadoCodigo) {
            case "APROBADO" -> "Aprobado";
            case "COMPLETADO" -> "Completado";
            case "EN_PROCESO" -> "En Proceso";
            case "RECHAZADO" -> "Rechazado";
            default -> "Pendiente";
        };
    }

    private String extractFileName(String path) {
        if (path == null) return null;
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private List<ProjectEvidenceDTO> colCambiosDescripcion(String proyectoId) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return evidencias;

        List<Entregable> entregables = entregableRepository.findByProyectoId(proyectoId);

        for (Entregable entregable : entregables) {
            List<EntregableCambioDescripcion> cambios = entregableCambioDescripcionRepository
                    .findByEntregableIdOrderByCreadoEnDesc(entregable.getId());

            if (cambios.isEmpty()) continue;

            String hitoNombre = entregable.getHito() != null ? entregable.getHito().getNombre() : null;
            String faseNombre = null;
            if (entregable.getHito() != null && entregable.getHito().getFase() != null) {
                faseNombre = entregable.getHito().getFase().getNombre();
            }

            for (EntregableCambioDescripcion cambio : cambios) {
                String urlDescarga = null;
                if (cambio.getArchivoPdf() != null && !cambio.getArchivoPdf().isBlank()) {
                    urlDescarga = "/api/v1/proyectos/" + proyectoId
                            + "/entregables/cambios-descripcion/" + cambio.getId() + "/descargar";
                }

                evidencias.add(new ProjectEvidenceDTO(
                        "cd-" + cambio.getId() + "-" + proyectoId,
                        "CAMBIO_DESCRIPCION",
                        "Cambio de descripción - " + (entregable.getNombre() != null ? entregable.getNombre() : ""),
                        cambio.getNombreOriginal(),
                        urlDescarga,
                        cambio.getCreadoEn() != null ? cambio.getCreadoEn().toLocalDate() : null,
                        null,
                        null,
                        "COMPLETADO",
                        "COMPLETADO",
                        cambio.getUsuario(),
                        "Cambio de descripción",
                        null,
                        faseNombre,
                        hitoNombre,
                        entregable.getNombre(),
                        cambio.getJustificacion(),
                        null,
                        entregable.getId(),
                        cambio.getId(),
                        null,
                        null,
                        cambio.getArchivoPdf(),
                        null,
                        null,
                        cambio.getJustificacion(),
                        null,
                        null,
                        cambio.getDescripcionAnterior(),
                        cambio.getDescripcionNueva(),
                        null
                ));
            }
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colActaCierre(String proyectoId) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        actaCierreRepository.findByProyectoId(proyectoId).ifPresent(acta -> {
            if (acta.getArchivoPdf() != null && !acta.getArchivoPdf().isBlank()) {
                String urlPdf = "/api/v1/proyectos/" + proyectoId + "/cierre/descargar";
                evidencias.add(new ProjectEvidenceDTO(
                        "cierre-pdf-" + acta.getId(),
                        "ACTA_CIERRE",
                        "Acta de cierre (PDF)",
                        acta.getArchivoPdf(),
                        urlPdf,
                        acta.getFechaCierre() != null ? acta.getFechaCierre().toLocalDate() : null,
                        null,
                        null,
                        "COMPLETADO",
                        "COMPLETADO",
                        null,
                        "Acta de cierre",
                        null,
                        null,
                        null,
                        null,
                        acta.getResumenEjecutivo(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }

            if (acta.getArchivoDocx() != null && !acta.getArchivoDocx().isBlank()) {
                String urlDocx = "/api/v1/proyectos/" + proyectoId + "/cierre/descargar?formato=docx";
                evidencias.add(new ProjectEvidenceDTO(
                        "cierre-docx-" + acta.getId(),
                        "ACTA_CIERRE",
                        "Acta de cierre (DOCX)",
                        acta.getArchivoDocx(),
                        urlDocx,
                        acta.getFechaCierre() != null ? acta.getFechaCierre().toLocalDate() : null,
                        null,
                        null,
                        "COMPLETADO",
                        "COMPLETADO",
                        null,
                        "Acta de cierre",
                        null,
                        null,
                        null,
                        null,
                        acta.getResumenEjecutivo(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }
        });

        return evidencias;
    }
}