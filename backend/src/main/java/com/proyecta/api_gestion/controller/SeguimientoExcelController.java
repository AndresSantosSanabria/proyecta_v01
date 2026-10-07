package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.service.report.SeguimientoExcelGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Seguimiento Excel", description = "Endpoints de carga y consulta del seguimiento de proyectos vía Excel")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class SeguimientoExcelController {

    private final SeguimientoExcelGenerator seguimientoExcelGenerator;

    public SeguimientoExcelController(SeguimientoExcelGenerator seguimientoExcelGenerator) {
        this.seguimientoExcelGenerator = seguimientoExcelGenerator;
    }

    @Operation(
        summary = "Descargar el reporte de seguimiento del proyecto en Excel",
        description = "Genera y devuelve un archivo XLSX con el seguimiento del proyecto. Requiere permiso PROYECTO:VER y pertenencia al proyecto."
    )
    // CWE-862: el reporte expone datos del proyecto; requiere PROYECTO:VER y
    // pertenencia al proyecto (canAccessOperational valida asignacion).
    @GetMapping("/{projectId}/reportes/seguimiento-excel")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<byte[]> downloadSeguimientoExcel(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "sistema";
            byte[] excelData = seguimientoExcelGenerator.generate(projectId, username);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    com.proyecta.api_gestion.infrastructure.HttpHeaderSanitizer.contentDisposition(
                            "attachment", "Seguimiento_Proyecto_" + projectId + ".xlsx"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
                    
        } catch (IOException _) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
