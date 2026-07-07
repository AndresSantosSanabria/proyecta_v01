package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.avance.IndicadoresEficienciaDTO;
import com.proyecta.api_gestion.service.interfaces.IIndicadorEficienciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class IndicadorEficienciaController {

    private final IIndicadorEficienciaService indicadorService;

    public IndicadorEficienciaController(IIndicadorEficienciaService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping("/{proyectoId}/indicadores-eficiencia")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<IndicadoresEficienciaDTO>> calcularIndicadores(
            @PathVariable String proyectoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte) {

        IndicadoresEficienciaDTO resultado = indicadorService.calcular(proyectoId, fechaCorte);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Indicadores de eficiencia calculados exitosamente"));
    }
}
