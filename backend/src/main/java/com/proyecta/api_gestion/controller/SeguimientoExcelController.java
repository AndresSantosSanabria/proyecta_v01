package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.service.report.SeguimientoExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/proyectos")
public class SeguimientoExcelController {

    private final SeguimientoExcelGenerator seguimientoExcelGenerator;

    public SeguimientoExcelController(SeguimientoExcelGenerator seguimientoExcelGenerator) {
        this.seguimientoExcelGenerator = seguimientoExcelGenerator;
    }

    @GetMapping("/{projectId}/reportes/seguimiento-excel")
    public ResponseEntity<byte[]> downloadSeguimientoExcel(@PathVariable String projectId,
                                                           Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "sistema";
            byte[] excelData = seguimientoExcelGenerator.generate(projectId, username);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Seguimiento_Proyecto_" + projectId + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
