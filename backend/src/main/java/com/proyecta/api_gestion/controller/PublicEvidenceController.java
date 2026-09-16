package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
import com.proyecta.api_gestion.service.IRiesgoService;
import com.proyecta.api_gestion.service.IRiesgoTratamientoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "*")
public class PublicEvidenceController {

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

    @GetMapping(value = "/evidencia/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verEvidencia(@PathVariable String token) {
        Entregable entregable = evidenceAccessService.resolveByToken(token);

        if (entregable.getArchivoPdf() == null || entregable.getArchivoPdf().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageProvider.loadFileAsResource("evidencias", entregable.getArchivoPdf());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entregable.getArchivoPdf() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping(value = "/cierre-evidencia/{fileName}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verEvidenciaCierre(@PathVariable String fileName) {
        try {
            Resource resource = storageProvider.loadFileAsResource("cierre-transferencia", fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/riesgos/{proyectoId}/{riesgoId}/soluciones/{solucionId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verSolucionRiesgo(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @PathVariable Long solucionId,
            @RequestParam(value = "inline", defaultValue = "true") boolean inline) {
        Resource resource = riesgoService.descargarSolucion(proyectoId, riesgoId, solucionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment")
                        + "; filename=\"solucion-riesgo-" + riesgoId + "-" + solucionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping(value = "/riesgos/{proyectoId}/{riesgoId}/tratamientos/{tratamientoId}/adjuntos/{adjuntoId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> verAdjuntoTratamiento(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @PathVariable Long tratamientoId,
            @PathVariable Long adjuntoId,
            @RequestParam(value = "inline", defaultValue = "true") boolean inline) {
        Resource resource = tratamientoService.descargarAdjunto(proyectoId, riesgoId, tratamientoId, adjuntoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment")
                        + "; filename=\"tratamiento-" + riesgoId + "-" + tratamientoId + "-" + adjuntoId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
