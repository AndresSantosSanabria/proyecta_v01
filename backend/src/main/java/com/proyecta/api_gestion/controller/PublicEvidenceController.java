package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.PublicEvidenceAccessService;
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

    public PublicEvidenceController(PublicEvidenceAccessService evidenceAccessService,
                                     IStorageProvider storageProvider) {
        this.evidenceAccessService = evidenceAccessService;
        this.storageProvider = storageProvider;
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
}
