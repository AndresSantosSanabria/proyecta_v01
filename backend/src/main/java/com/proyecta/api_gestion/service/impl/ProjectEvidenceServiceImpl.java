package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.*;
import com.proyecta.api_gestion.domain.model.advance.AdvanceReportUpload;
import com.proyecta.api_gestion.domain.model.advance.AdvanceReportVersion;
import com.proyecta.api_gestion.domain.model.enums.DocumentoProyectoVersionEstado;
import com.proyecta.api_gestion.application.port.out.persistence.*;
import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportUploadRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportVersionRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.ProjectEvidenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ProjectEvidenceServiceImpl implements ProjectEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ProjectEvidenceServiceImpl.class);

    private static final String CAT_DOCUMENTO_PROYECTO = "DOCUMENTO_PROYECTO";
    private static final String CAT_DOCUMENTO_PROYECTO_AVANZADO = "DOCUMENTO_PROYECTO_AVANZADO";
    private static final String CAT_DOCUMENTO_DINAMICO = "DOCUMENTO_DINAMICO";
    private static final String CAT_EVIDENCIA_ENTREGABLE = "EVIDENCIA_ENTREGABLE";
    private static final String CAT_CRONOGRAMA = "CRONOGRAMA";
    private static final String CAT_RIESGO = "RIESGO";
    private static final String CAT_MATRIZ_RIESGOS = "MATRIZ_RIESGOS";
    private static final String CAT_CAMBIO_FECHA = "CAMBIO_FECHA";
    private static final String CAT_CAMBIO_DESCRIPCION = "CAMBIO_DESCRIPCION";
    private static final String CAT_ACTA_CIERRE = "ACTA_CIERRE";
    private static final String CAT_INFORME_AVANCE = "INFORME_AVANCE";
    private static final String URL_PROYECTOS_BASE = "/api/v1/proyectos/";
    private static final String URL_DESCARGAR = "/descargar";
    private static final String ESTADO_CARGADO = "CARGADO";
    private static final String ESTADO_COMPLETADO = "COMPLETADO";

    private static final Map<String, String> DOC_TYPE_NAMES = Map.of(
            "VIABILIZACION", "Documento de viabilidad",
            "ACTA_CONSTITUCION", "Acta de constitucion",
            CAT_CRONOGRAMA, "Cronograma del proyecto",
            "PLAN_COMUNICACIONES", "Plan de comunicaciones",
            "MATRIZ_RIESGOS_VIABILIDAD", "Matriz de Riesgos de Viabilidad"
    );

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final EntregableRepositoryPort entregableRepositoryPort;
    private final DocumentoProyectoVersionRepositoryPort documentoProyectoVersionRepositoryPort;
    private final RiesgoRepositoryPort riesgoRepositoryPort;
    private final EntregableCambioFechaRepositoryPort entregableCambioFechaRepositoryPort;
    private final EntregableCambioDescripcionRepositoryPort entregableCambioDescripcionRepositoryPort;
    private final DocumentoDinamicoRepositoryPort documentoDinamicoRepositoryPort;
    private final ActaCierreRepositoryPort actaCierreRepositoryPort;
    private final AdvanceReportUploadRepositoryPort advanceReportUploadRepositoryPort;
    private final AdvanceReportVersionRepositoryPort advanceReportVersionRepositoryPort;

    public ProjectEvidenceServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                                      EntregableRepositoryPort entregableRepositoryPort,
                                      DocumentoProyectoVersionRepositoryPort documentoProyectoVersionRepositoryPort,
                                      RiesgoRepositoryPort riesgoRepositoryPort,
                                      EntregableCambioFechaRepositoryPort entregableCambioFechaRepositoryPort,
                                      EntregableCambioDescripcionRepositoryPort entregableCambioDescripcionRepositoryPort,
                                      DocumentoDinamicoRepositoryPort documentoDinamicoRepositoryPort,
                                      ActaCierreRepositoryPort actaCierreRepositoryPort,
                                      AdvanceReportUploadRepositoryPort advanceReportUploadRepositoryPort,
                                      AdvanceReportVersionRepositoryPort advanceReportVersionRepositoryPort) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.documentoProyectoVersionRepositoryPort = documentoProyectoVersionRepositoryPort;
        this.riesgoRepositoryPort = riesgoRepositoryPort;
        this.entregableCambioFechaRepositoryPort = entregableCambioFechaRepositoryPort;
        this.entregableCambioDescripcionRepositoryPort = entregableCambioDescripcionRepositoryPort;
        this.documentoDinamicoRepositoryPort = documentoDinamicoRepositoryPort;
        this.actaCierreRepositoryPort = actaCierreRepositoryPort;
        this.advanceReportUploadRepositoryPort = advanceReportUploadRepositoryPort;
        this.advanceReportVersionRepositoryPort = advanceReportVersionRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectEvidenceDTO> listarEvidencias(String proyectoId, String categoria) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        Set<String> tiposConVersionActual = documentoProyectoVersionRepositoryPort
                .findByProyectoIdOrderBySubidoEnDesc(proyectoId).stream()
                .filter(v -> DocumentoProyectoVersionEstado.ACTUAL.equals(v.getEstado()))
                .map(DocumentoProyectoVersion::getTipoDocumento)
                .collect(Collectors.toSet());

        if (categoriaCoincide(categoria, CAT_DOCUMENTO_PROYECTO)) {
            safeAddAll(evidencias, () -> colDocumentosProyecto(proyectoId), CAT_DOCUMENTO_PROYECTO);
        }
        if (categoriaCoincide(categoria, CAT_DOCUMENTO_PROYECTO_AVANZADO)) {
            safeAddAll(evidencias, () -> colDocumentosProyectoAvanzado(proyecto, tiposConVersionActual), CAT_DOCUMENTO_PROYECTO_AVANZADO);
        }
        if (categoriaCoincide(categoria, CAT_DOCUMENTO_DINAMICO)) {
            safeAddAll(evidencias, () -> colDocumentosDinamicos(proyectoId), CAT_DOCUMENTO_DINAMICO);
        }
        if (categoriaCoincide(categoria, CAT_EVIDENCIA_ENTREGABLE)) {
            safeAddAll(evidencias, () -> colEvidenciasEntregables(proyectoId), CAT_EVIDENCIA_ENTREGABLE);
        }
        if (categoriaCoincide(categoria, CAT_CRONOGRAMA)) {
            safeAddAll(evidencias, () -> colCronograma(proyecto, tiposConVersionActual), CAT_CRONOGRAMA);
        }
        if (categoriaCoincide(categoria, CAT_RIESGO)) {
            safeAddAll(evidencias, () -> colSolucionesRiesgos(proyectoId), CAT_RIESGO);
        }
        if (categoriaCoincide(categoria, CAT_MATRIZ_RIESGOS)) {
            safeAddAll(evidencias, () -> colMatrizRiesgos(proyectoId), CAT_MATRIZ_RIESGOS);
        }
        if (categoriaCoincide(categoria, CAT_CAMBIO_FECHA)) {
            safeAddAll(evidencias, () -> colCambiosFecha(proyectoId), CAT_CAMBIO_FECHA);
        }
        if (categoriaCoincide(categoria, CAT_CAMBIO_DESCRIPCION)) {
            safeAddAll(evidencias, () -> colCambiosDescripcion(proyectoId), CAT_CAMBIO_DESCRIPCION);
        }
        if (categoriaCoincide(categoria, CAT_ACTA_CIERRE)) {
            safeAddAll(evidencias, () -> colActaCierre(proyectoId), CAT_ACTA_CIERRE);
        }
        if (categoriaCoincide(categoria, CAT_INFORME_AVANCE)) {
            safeAddAll(evidencias, () -> colInformesAvance(proyectoId), CAT_INFORME_AVANCE);
        }

        return evidencias;
    }

    private boolean categoriaCoincide(String categoria, String codigo) {
        return categoria == null || categoria.isBlank() || "TODOS".equals(categoria) || codigo.equals(categoria);
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
        List<DocumentoProyectoVersion> versiones = documentoProyectoVersionRepositoryPort
                .findByProyectoIdOrderBySubidoEnDesc(proyectoId);

        return versiones.stream()
                .filter(v -> DocumentoProyectoVersionEstado.ACTUAL.equals(v.getEstado()))
                .map(v -> {
                    String codigo = v.getTipoDocumento();
                    String nombreDisplay = DOC_TYPE_NAMES.getOrDefault(codigo, codigo);
                    String urlDescarga = URL_PROYECTOS_BASE + proyectoId + "/documentos/" + codigo + URL_DESCARGAR;
                    return new ProjectEvidenceDTO(
                            "doc-" + codigo,
                            CAT_DOCUMENTO_PROYECTO,
                            nombreDisplay,
                            v.getNombreArchivoOriginal(),
                            urlDescarga,
                            v.getSubidoEn() != null ? v.getSubidoEn().toLocalDate() : null,
                            null,
                            null,
                            ESTADO_CARGADO,
                            ESTADO_CARGADO,
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

    private List<ProjectEvidenceDTO> colDocumentosProyectoAvanzado(Proyecto proyecto, Set<String> tiposConVersionActual) {
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

            if (pdfPath == null || pdfPath.isBlank() || tiposConVersionActual.contains(codigo)) {
                continue;
            }

            String nombreDisplay = DOC_TYPE_NAMES.getOrDefault(codigo, codigo);
            String nombreArchivo = extractFileName(pdfPath);
            String urlDescarga = URL_PROYECTOS_BASE + proyectoId + "/documentos/" + codigo + URL_DESCARGAR;

            evidencias.add(new ProjectEvidenceDTO(
                    "doc-proy-" + codigo,
                    CAT_DOCUMENTO_PROYECTO_AVANZADO,
                    nombreDisplay,
                    nombreArchivo,
                    urlDescarga,
                    null,
                    null,
                    null,
                    ESTADO_CARGADO,
                    ESTADO_CARGADO,
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
        List<DocumentoDinamico> documentos = documentoDinamicoRepositoryPort
                .findByProyectoIdOrderByFechaCargaDesc(proyectoId);

        return documentos.stream()
                .map(doc -> {
                    String nombreDisplay = doc.getTipoDocumento() != null ? doc.getTipoDocumento() : "Documento dinamico";
                    String urlDescarga = doc.getUrlDescarga() != null
                            ? doc.getUrlDescarga()
                            : URL_PROYECTOS_BASE + proyectoId + "/documentos-dinamicos/" + doc.getId() + URL_DESCARGAR;

                    return new ProjectEvidenceDTO(
                            "doc-din-" + doc.getId(),
                            CAT_DOCUMENTO_DINAMICO,
                            nombreDisplay,
                            doc.getNombreOriginal(),
                            urlDescarga,
                            doc.getFechaCarga() != null ? doc.getFechaCarga().toLocalDate() : null,
                            null,
                            null,
                            ESTADO_CARGADO,
                            ESTADO_CARGADO,
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
        List<Entregable> entregables = entregableRepositoryPort.findByProyectoId(proyectoId);
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

            String urlEvidencia = URL_PROYECTOS_BASE + proyectoId
                    + "/avance/entregables/" + entregable.getId() + "/evidencia";

            String estadoCodigo = mapEstado(entregable);
            String estadoDisplay = mapEstadoDisplay(estadoCodigo);

            evidencias.add(new ProjectEvidenceDTO(
                    "ev-" + entregable.getId(),
                    CAT_EVIDENCIA_ENTREGABLE,
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

    private List<ProjectEvidenceDTO> colCronograma(Proyecto proyecto, Set<String> tiposConVersionActual) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        if (tiposConVersionActual.contains(CAT_CRONOGRAMA)) {
            return evidencias;
        }

        if (proyecto.getCronogramaPdf() != null && !proyecto.getCronogramaPdf().isBlank()) {
            String nombreArchivo = extractFileName(proyecto.getCronogramaPdf());
            String urlDescarga = URL_PROYECTOS_BASE + proyecto.getId() + "/cronograma/descargar";

            evidencias.add(new ProjectEvidenceDTO(
                    "crono-" + proyecto.getId(),
                    CAT_CRONOGRAMA,
                    "Cronograma del proyecto",
                    nombreArchivo,
                    urlDescarga,
                    null,
                    null,
                    null,
                    ESTADO_CARGADO,
                    ESTADO_CARGADO,
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
        List<Riesgo> riesgos = riesgoRepositoryPort.findByProyectoId(proyectoId);
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Riesgo riesgo : riesgos) {
            if (riesgo.getSoluciones() == null) {
                continue;
            }
            for (RiesgoSolucionAdjunto solucion : riesgo.getSoluciones()) {
                String urlDescarga = URL_PROYECTOS_BASE + proyectoId
                        + "/riesgos/" + riesgo.getId()
                        + "/soluciones/" + solucion.getId() + URL_DESCARGAR;

                evidencias.add(new ProjectEvidenceDTO(
                        "riesgo-sol-" + solucion.getId(),
                        CAT_RIESGO,
                        solucion.getNombreOriginal() != null ? solucion.getNombreOriginal() : "Solucion " + solucion.getId(),
                        solucion.getNombreOriginal(),
                        urlDescarga,
                        solucion.getFechaCarga() != null ? solucion.getFechaCarga().toLocalDate() : null,
                        null,
                        null,
                        ESTADO_CARGADO,
                        ESTADO_CARGADO,
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
        List<Riesgo> riesgos = riesgoRepositoryPort.findByProyectoId(proyectoId);
        log.info("colMatrizRiesgos: proyectoId={}, riesgos encontrados={}", proyectoId, riesgos.size());
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (Riesgo riesgo : riesgos) {
            String nivelDisplay = riesgo.getNivel() != null ? riesgo.getNivel().name() : "SIN_NIVEL";
            String descripcion = "Riesgo: "
                    + (riesgo.getCodigo() != null ? riesgo.getCodigo() : riesgo.getId())
                    + " - " + truncate(riesgo.getDescripcion(), 80);

            int solucionesCount = (riesgo.getSoluciones() != null) ? riesgo.getSoluciones().size() : 0;

            String usuarioResponsable = riesgo.getCreatedBy() != null && !riesgo.getCreatedBy().isBlank()
                    ? riesgo.getCreatedBy() : "Sin usuario";

            evidencias.add(new ProjectEvidenceDTO(
                    "riesgo-matriz-" + riesgo.getId(),
                    CAT_MATRIZ_RIESGOS,
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
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();
        List<Entregable> entregables = entregableRepositoryPort.findByProyectoId(proyectoId);

        for (Entregable entregable : entregables) {
            List<EntregableCambioFecha> cambios = entregableCambioFechaRepositoryPort
                    .findByEntregableIdOrderByCreadoEnDesc(entregable.getId());

            HitoFaseNombres nombres = resolverNombresHitoFase(entregable);

            for (EntregableCambioFecha cambio : cambios) {
                evidencias.add(buildCambioFechaDTO(entregable, cambio, nombres, proyectoId));
            }
        }

        return evidencias;
    }

    private HitoFaseNombres resolverNombresHitoFase(Entregable entregable) {
        Hito hito = entregable.getHito();
        if (hito == null) {
            return new HitoFaseNombres(null, null);
        }
        String hitoNombre = hito.getNombre();
        String faseNombre = null;
        if (hito.getFase() != null) {
            faseNombre = hito.getFase().getNombre();
        }
        return new HitoFaseNombres(hitoNombre, faseNombre);
    }

    private ProjectEvidenceDTO buildCambioFechaDTO(Entregable entregable, EntregableCambioFecha cambio,
            HitoFaseNombres nombres, String proyectoId) {
        String urlDescarga = null;
        if (cambio.getArchivoPdf() != null && !cambio.getArchivoPdf().isBlank()) {
            urlDescarga = URL_PROYECTOS_BASE + proyectoId
                    + "/entregables/cambios-fecha/" + cambio.getId() + URL_DESCARGAR;
        }

        return new ProjectEvidenceDTO(
                "cf-" + cambio.getId() + "-" + proyectoId,
                CAT_CAMBIO_FECHA,
                "Cambio de fecha - " + (entregable.getNombre() != null ? entregable.getNombre() : ""),
                cambio.getNombreOriginal(),
                urlDescarga,
                cambio.getCreadoEn() != null ? cambio.getCreadoEn().toLocalDate() : null,
                null,
                null,
                ESTADO_COMPLETADO,
                ESTADO_COMPLETADO,
                cambio.getUsuario(),
                "Cambio de fecha",
                null,
                nombres.faseNombre(),
                nombres.hitoNombre(),
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
        );
    }

    private ProjectEvidenceDTO buildCambioDescripcionDTO(Entregable entregable, EntregableCambioDescripcion cambio,
            HitoFaseNombres nombres, String proyectoId) {
        String urlDescarga = null;
        if (cambio.getArchivoPdf() != null && !cambio.getArchivoPdf().isBlank()) {
            urlDescarga = URL_PROYECTOS_BASE + proyectoId
                    + "/entregables/cambios-descripcion/" + cambio.getId() + URL_DESCARGAR;
        }

        return new ProjectEvidenceDTO(
                "cd-" + cambio.getId() + "-" + proyectoId,
                CAT_CAMBIO_DESCRIPCION,
                "Cambio de descripción - " + (entregable.getNombre() != null ? entregable.getNombre() : ""),
                cambio.getNombreOriginal(),
                urlDescarga,
                cambio.getCreadoEn() != null ? cambio.getCreadoEn().toLocalDate() : null,
                null,
                null,
                ESTADO_COMPLETADO,
                ESTADO_COMPLETADO,
                cambio.getUsuario(),
                "Cambio de descripción",
                null,
                nombres.faseNombre(),
                nombres.hitoNombre(),
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
        );
    }

    private record HitoFaseNombres(String hitoNombre, String faseNombre) {
    }

    private String mapEstado(Entregable entregable) {
        if (entregable.getEstado() == null) return "PENDIENTE";
        return switch (entregable.getEstado()) {
            case APROBADO -> "APROBADO";
            case COMPLETADO -> ESTADO_COMPLETADO;
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
            case ESTADO_COMPLETADO -> "Completado";
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
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId).orElse(null);
        if (proyecto == null) return evidencias;

        List<Entregable> entregables = entregableRepositoryPort.findByProyectoId(proyectoId);

        for (Entregable entregable : entregables) {
            List<EntregableCambioDescripcion> cambios = entregableCambioDescripcionRepositoryPort
                    .findByEntregableIdOrderByCreadoEnDesc(entregable.getId());

            if (cambios.isEmpty()) continue;

            HitoFaseNombres nombres = resolverNombresHitoFase(entregable);

            for (EntregableCambioDescripcion cambio : cambios) {
                evidencias.add(buildCambioDescripcionDTO(entregable, cambio, nombres, proyectoId));
            }
        }

        return evidencias;
    }

    private List<ProjectEvidenceDTO> colActaCierre(String proyectoId) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        actaCierreRepositoryPort.findByProyectoId(proyectoId).ifPresent(acta -> {
            if (acta.getArchivoPdf() != null && !acta.getArchivoPdf().isBlank()) {
                String urlPdf = URL_PROYECTOS_BASE + proyectoId + "/cierre/descargar";
                evidencias.add(new ProjectEvidenceDTO(
                        "cierre-pdf-" + acta.getId(),
                        CAT_ACTA_CIERRE,
                        "Acta de cierre (PDF)",
                        acta.getArchivoPdf(),
                        urlPdf,
                        acta.getFechaCierre() != null ? acta.getFechaCierre().toLocalDate() : null,
                        null,
                        null,
                        ESTADO_COMPLETADO,
                        ESTADO_COMPLETADO,
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
                String urlDocx = URL_PROYECTOS_BASE + proyectoId + "/cierre/descargar?formato=docx";
                evidencias.add(new ProjectEvidenceDTO(
                        "cierre-docx-" + acta.getId(),
                        CAT_ACTA_CIERRE,
                        "Acta de cierre (DOCX)",
                        acta.getArchivoDocx(),
                        urlDocx,
                        acta.getFechaCierre() != null ? acta.getFechaCierre().toLocalDate() : null,
                        null,
                        null,
                        ESTADO_COMPLETADO,
                        ESTADO_COMPLETADO,
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

    private List<ProjectEvidenceDTO> colInformesAvance(String proyectoId) {
        List<ProjectEvidenceDTO> evidencias = new ArrayList<>();

        for (AdvanceReportUpload upload : advanceReportUploadRepositoryPort.findByProjectIdOrderByUploadedAtDesc(proyectoId)) {
            String url = "/api/v1/advance-report/download/" + proyectoId + "?periodo=" + upload.getPeriodo();
            AdvanceReportVersion actual = advanceReportVersionRepositoryPort
                    .findByUploadIdAndEstado(upload.getId(), AdvanceReportVersion.ESTADO_ACTUAL)
                    .orElse(null);

            evidencias.add(new ProjectEvidenceDTO(
                    "informe-avance-" + upload.getId(),
                    CAT_INFORME_AVANCE,
                    "Informe de avance " + upload.getPeriodo(),
                    upload.getFileName(),
                    url,
                    upload.getUploadedAt() != null ? upload.getUploadedAt().toLocalDate() : null,
                    null,
                    null,
                    upload.getEstado(),
                    upload.getEstado(),
                    upload.getUploadedBy(),
                    "Informe de avance",
                    null,
                    null,
                    null,
                    null,
                    "Periodo " + upload.getPeriodo(),
                    upload.getObservaciones(),
                    null,
                    null,
                    null,
                    null,
                    upload.getFileName(),
                    null,
                    null,
                    null,
                    upload.getFileSize(),
                    actual != null ? actual.getMimeType() : null,
                    null,
                    null,
                    null
            ));
        }

        return evidencias;
    }
}