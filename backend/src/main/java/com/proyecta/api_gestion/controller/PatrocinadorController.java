package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.proyecto.PatrocinadorDTO;
import com.proyecta.api_gestion.model.Patrocinador;
import com.proyecta.api_gestion.repository.PatrocinadorRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/patrocinadores")
@Tag(name = "Catálogos — Patrocinadores", description = "Endpoints para listar patrocinadores")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class PatrocinadorController {

    private final PatrocinadorRepository patrocinadorRepository;

    public PatrocinadorController(PatrocinadorRepository patrocinadorRepository) {
        this.patrocinadorRepository = patrocinadorRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatrocinadorDTO>>> listarPatrocinadoresUnicos() {
        List<Patrocinador> patrocinadores = patrocinadorRepository.findUniquePatrocinadores();
        List<PatrocinadorDTO> dtos = patrocinadores.stream()
                .map(p -> new PatrocinadorDTO(
                        p.getNombre(),
                        p.getEntidad(),
                        p.getCargo(),
                        p.getProcesoSigc(),
                        p.getProcedimiento()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos, "Patrocinadores obtenidos con éxito"));
    }
}
