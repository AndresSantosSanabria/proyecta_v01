package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.config.openapi.PublicEndpoint;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import com.proyecta.api_gestion.service.IRiesgoService;
import com.proyecta.api_gestion.service.IRiesgoTratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints publicos de evidencia (acceso anonimo por requisito de negocio).
 * Todas las rutas quedan protegidas por {@code PublicEvidenceSecurityFilter}
 * (rate limit por IP + firma HMAC ?exp=&sig=) salvo /evidencia/{token}, que usa
 * un token opaco de 256 bits.
 */
@PublicEndpoint
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Evidencias Públicas", description = "Endpoints anónimos de consulta de evidencia protegidos con firma HMAC o token opaco (sin autenticación)")
public class PublicEvidenceController {

    private static final Logger log = LoggerFactory.getLogger(PublicEvidenceController.class);

    private final PublicEvidenceAccessService evidenceAccessService;
    private final IStorageProvider storageProvider;
    private final IRiesgoService riesgoService;
    private final IRiesgoTratamientoService tratamientoService;

    public PublicEvidenceController(PublicEvidenceAccessService evidenceAccessService,
                                     IStorageProvider storageProvider,
                                     IRiesgoService riesgoService,
                                     IRiesgoTratamientoService tratamientoService) {
        this.evidenceAccessService = evidenceAccessService;
        this.storageProvider = storageProvider;
        this.riesgoService = riesgoService;
        this.tratamientoService = tratamientoService;
    }

    @Operation(
        summary = "Consultar una evidencia mediante token opaco",
        description = "Devuelve el PDF de la evidencia asociada al token de 256 bits, sin autenticación. El token es de un solo uso según la vigencia configurada."
    )
    @GetMapping(value = "/evidencia/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verEvidencia(@Parameter(description = "Token opaco de acceso a la evidencia") @PathVariable String token) {
        Entregable entregable = evidenceAccessService.resolveByToken(token);

        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageProvider.loadFileAsResource("evidencias", entregable.getArchivoPdf());

        String safeName = entregable.getArchivoPdf().replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Operation(summary = "Descargar el PDF de cierre y transferencia de evidencia")
    @GetMapping(value = "/cierre-evidencia/{fileName}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verEvidenciaCierre(@Parameter(description = "Nombre del archivo de cierre") @PathVariable String fileName) {
        try {
            Resource resource = storageProvider.loadFileAsResource("cierre-transferencia", fileName);
            // CWE-113: nunca ecoar el nombre del archivo sin sanear en la cabecera.
            String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException ex) {
            // CWE-209: no exponer mensajes internos del storage al cliente anonimo.
            log.warn("Fallo publico sirviendo cierre-evidencia: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Descargar la solución de un riesgo en PDF",
        description = "Devuelve el PDF de la solución de riesgo; con 'inline=true' se muestra en el navegador, con 'false' se descarga como archivo adjunto."
    )
    @GetMapping(value = "/riesgos/{proyectoId}/{riesgoId}/soluciones/{solucionId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verSolucionRiesgo(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del riesgo") @PathVariable Integer riesgoId,
            @Parameter(description = "Identificador de la solución") @PathVariable Long solucionId,
            @Parameter(description = "Si es true el PDF se muestra en línea, si es false se descarga") @RequestParam(value = "inline", defaultValue = "true") boolean inline) {
        Resource resource = riesgoService.descargarSolucion(proyectoId, riesgoId, solucionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment")
                        + "; filename=\"solucion-riesgo-" + riesgoId + "-" + solucionId + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Operation(
        summary = "Descargar un adjunto de tratamiento de riesgo en PDF",
        description = "Devuelve el PDF adjunto al tratamiento; con 'inline=true' se muestra en el navegador, con 'false' se descarga como archivo adjunto."
    )
    @GetMapping(value = "/riesgos/{proyectoId}/{riesgoId}/tratamientos/{tratamientoId}/adjuntos/{adjuntoId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verAdjuntoTratamiento(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Identificador del riesgo") @PathVariable Integer riesgoId,
            @Parameter(description = "Identificador del tratamiento") @PathVariable Long tratamientoId,
            @Parameter(description = "Identificador del adjunto") @PathVariable Long adjuntoId,
            @Parameter(description = "Si es true el PDF se muestra en línea, si es false se descarga") @RequestParam(value = "inline", defaultValue = "true") boolean inline) {
        Resource resource = tratamientoService.descargarAdjunto(proyectoId, riesgoId, tratamientoId, adjuntoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment")
                        + "; filename=\"tratamiento-" + riesgoId + "-" + tratamientoId + "-" + adjuntoId + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
