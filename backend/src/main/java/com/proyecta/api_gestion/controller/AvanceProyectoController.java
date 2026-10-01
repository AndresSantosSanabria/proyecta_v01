package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IAvanceProyectoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.avance.DocumentoObservacionDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoReversionRequest;
import com.proyecta.api_gestion.dto.avance.DocumentoSubsanacionRequest;
import com.proyecta.api_gestion.dto.avance.DocumentoVersionDTO;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableAprobadoResponseDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.DocumentoVersion;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.enums.DocumentoVersionEstado;
import com.proyecta.api_gestion.repository.DocumentoVersionRepository;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class AvanceProyectoController implements IAvanceProyectoController {

    private final ProyectoAvanceService proyectoAvanceService;
    private final EntregableRepository entregableRepository;
    private final DocumentoVersionRepository documentoVersionRepository;
    private final IStorageProvider storageProvider;

    public AvanceProyectoController(ProyectoAvanceService proyectoAvanceService,
                                    EntregableRepository entregableRepository,
                                    DocumentoVersionRepository documentoVersionRepository,
                                    IStorageProvider storageProvider) {
        this.proyectoAvanceService = proyectoAvanceService;
        this.entregableRepository = entregableRepository;
        this.documentoVersionRepository = documentoVersionRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    @GetMapping("/{proyectoId}/avance")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoAvanceResponseDTO>> getAvanceProyecto(
            @PathVariable String proyectoId) {
        ProyectoAvanceResponseDTO detalle = proyectoAvanceService.obtenerAvanceDetallado(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(detalle, "Avance del proyecto obtenido con éxito"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/avance/entregables/{entregableId}/evidencia", consumes = "multipart/form-data")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('EVIDENCIA:CARGAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<EntregableAprobadoResponseDTO>> registrarEvidencia(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaEntrega,
            @RequestPart("evidencia") MultipartFile evidencia,
            Authentication authentication) {

        EntregableAprobadoResponseDTO result = proyectoAvanceService.registrarEvidencia(proyectoId, entregableId, fechaEntrega, evidencia, authentication);
        return ResponseEntity.ok(ApiResponse.success(result, "Evidencia registrada exitosamente. Queda pendiente de aprobacion."));
    }

    @Override
    @RequestMapping(value = "/{proyectoId}/avance/entregables/{entregableId}/aprobar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("@proyectoSecurity.canReviewEvidence(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<EntregableAprobadoResponseDTO>> aprobarEntregable(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam(required = false) String observacion,
            Authentication authentication) {

        EntregableAprobadoResponseDTO result = proyectoAvanceService.aprobarEntregable(proyectoId, entregableId, authentication);
        String message = (observacion == null || observacion.isBlank())
                ? "Entregable aprobado exitosamente."
                : "Entregable aprobado exitosamente. Observacion registrada: " + observacion;
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @Override
    @RequestMapping(value = "/{proyectoId}/avance/entregables/{entregableId}/rechazar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("@proyectoSecurity.canReviewEvidence(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<EntregableAprobadoResponseDTO>> rechazarEntregable(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam String observacion,
            Authentication authentication) {

        EntregableAprobadoResponseDTO result = proyectoAvanceService.rechazarEntregable(proyectoId, entregableId, observacion, authentication);
        String message = "Entregable rechazado. El asignado debe corregir la evidencia.";
        if (observacion != null && !observacion.isBlank()) {
            message += " Observacion: " + observacion;
        }
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @Operation(
        summary = "Listar versiones documentales de un entregable",
        description = "Retorna el historico de versiones del PDF de evidencia de un entregable, de la mas reciente a la mas antigua."
    )
    @GetMapping("/{proyectoId}/avance/entregables/{entregableId}/versiones")
    @PreAuthorize("@proyectoSecurity.canViewDocumentHistory(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<DocumentoVersionDTO>>> listarVersiones(
            @Parameter(description = "Identificador del proyecto", example = "PROY-CUN-2026-001") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del entregable") @PathVariable Integer entregableId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                proyectoAvanceService.listarVersiones(proyectoId, entregableId, authentication),
                "Historico documental obtenido"));
    }

    @Operation(
        summary = "Listar observaciones documentales de un entregable",
        description = "Retorna las observaciones registradas sobre la evidencia cargada para un entregable."
    )
    @GetMapping("/{proyectoId}/avance/entregables/{entregableId}/observaciones")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<DocumentoObservacionDTO>>> listarObservaciones(
            @Parameter(description = "Identificador del proyecto", example = "PROY-CUN-2026-001") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del entregable") @PathVariable Integer entregableId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                proyectoAvanceService.listarObservaciones(proyectoId, entregableId, authentication),
                "Observaciones documentales obtenidas"));
    }

    @Operation(
        summary = "Marcar una observacion de evidencia como subsanada",
        description = "Cierra una observacion sobre la evidencia de un entregable. Admite un comentario opcional de subsanacion."
    )
    @PutMapping("/{proyectoId}/avance/entregables/{entregableId}/observaciones/{observacionId}/subsanar")
    @PreAuthorize("@proyectoSecurity.canMarkEvidenceCorrected(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<DocumentoObservacionDTO>> marcarObservacionSubsanada(
            @Parameter(description = "Identificador del proyecto", example = "PROY-CUN-2026-001") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del entregable") @PathVariable Integer entregableId,
            @Parameter(description = "Identificador de la observacion a subsanar") @PathVariable Long observacionId,
            @RequestBody(required = false) DocumentoSubsanacionRequest request,
            Authentication authentication) {
        String comentario = request != null ? request.comentario() : null;
        return ResponseEntity.ok(ApiResponse.success(
                proyectoAvanceService.marcarObservacionSubsanada(proyectoId, entregableId, observacionId, comentario, authentication),
                "Observacion marcada como subsanada"));
    }

    @Operation(
        summary = "Revertir a una version anterior de la evidencia",
        description = "Restaura una version documental previa como version vigente del entregable. Admite un motivo opcional de reversion."
    )
    @PostMapping("/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/revertir")
    @PreAuthorize("@proyectoSecurity.canRevertDocumentVersion(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<EntregableAprobadoResponseDTO>> revertirVersion(
            @Parameter(description = "Identificador del proyecto", example = "PROY-CUN-2026-001") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del entregable") @PathVariable Integer entregableId,
            @Parameter(description = "Numero de version a restaurar") @PathVariable Long versionId,
            @RequestBody(required = false) DocumentoReversionRequest request,
            Authentication authentication) {
        String motivo = request != null ? request.motivo() : null;
        return ResponseEntity.ok(ApiResponse.success(
                proyectoAvanceService.revertirVersion(proyectoId, entregableId, versionId, motivo, authentication),
                "Version documental restaurada"));
    }

    @Operation(
        summary = "Descargar la evidencia vigente de un entregable",
        description = "Retorna el archivo PDF de la version vigente de la evidencia cargada para el entregable, con disposicion en linea (inline)."
    )
    @GetMapping("/{proyectoId}/avance/entregables/{entregableId}/evidencia")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarEvidencia(
            @Parameter(description = "Identificador del proyecto", example = "PROY-CUN-2026-001") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del entregable") @PathVariable Integer entregableId) {
        Entregable entregable = entregableRepository.findByIdAndProyectoIdWithHierarchy(entregableId, proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado en el proyecto solicitado: " + entregableId));

        if (entregable.getArchivoPdf() == null) {
            throw new ResourceNotFoundException("El entregable no tiene documento de evidencia cargado");
        }

        Resource resource = storageProvider.loadFileAsResource("evidencias", entregable.getArchivoPdf());
        String nombreDescarga = nombreEvidenciaActual(entregable);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, inlinePdfName(nombreDescarga))
                .body(resource);
    }

    @Override
    @PostMapping("/{proyectoId}/avance/alertar-director")
    @PreAuthorize("@proyectoSecurity.canSendDirectorNotification(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<String>> enviarAlertaDirector(
            @PathVariable String proyectoId,
            Authentication authentication) {
        proyectoAvanceService.enviarAlertaDirector(proyectoId, authentication);
        return ResponseEntity.ok(ApiResponse.success("Notificacion enviada exitosamente al director del proyecto.", "Alerta al director enviada"));
    }

    private String inlinePdfName(String fileName) {
        String safeName = com.proyecta.api_gestion.infrastructure.HttpHeaderSanitizer.safeFileName(fileName);
        return ContentDisposition.inline()
                .filename(safeName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private String nombreEvidenciaActual(Entregable entregable) {
        return documentoVersionRepository.findFirstByEntregableIdAndEstadoOrderByNumeroVersionDesc(
                        entregable.getId(),
                        DocumentoVersionEstado.ACTUAL
                )
                .map(DocumentoVersion::getNombreArchivoOriginal)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(nombre -> nombre.replace("\"", ""))
                .orElse(entregable.getArchivoPdf());
    }
}
