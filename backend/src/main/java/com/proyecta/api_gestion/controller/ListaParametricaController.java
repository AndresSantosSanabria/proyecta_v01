package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.config.ListaParametricaItemDTO;
import com.proyecta.api_gestion.service.config.ListaParametricaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion/listas")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ListaParametricaController {

    private final ListaParametricaService service;

    public ListaParametricaController(ListaParametricaService service) {
        this.service = service;
    }

    @GetMapping("/{listaClave}")
    public ResponseEntity<ApiResponse<List<ListaParametricaItemDTO>>> listar(
            @PathVariable String listaClave,
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(ApiResponse.success(
                service.listarPorClave(listaClave, soloActivos),
                "Items listados correctamente"));
    }

    @GetMapping("/{listaClave}/valores")
    public ResponseEntity<ApiResponse<List<String>>> listarValores(@PathVariable String listaClave) {
        return ResponseEntity.ok(ApiResponse.success(
                service.listarValoresActivos(listaClave),
                "Valores listados correctamente"));
    }

    @PutMapping("/{listaClave}/valores")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> guardarValores(
            @PathVariable String listaClave,
            @RequestBody Map<String, Object> body) {
        String nombreCampo = (String) body.getOrDefault("nombreCampo", listaClave);
        String descripcion = (String) body.getOrDefault("descripcion", "");
        @SuppressWarnings("unchecked")
        List<String> valores = (List<String>) body.getOrDefault("valores", List.of());
        service.guardarValores(listaClave, nombreCampo, descripcion, valores);
        return ResponseEntity.ok(ApiResponse.success("Valores guardados correctamente"));
    }
}
