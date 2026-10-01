package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.config.ListaParametricaItemDTO;
import com.proyecta.api_gestion.service.config.ListaParametricaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion/listas")
@Tag(name = "Configuración - Listas Paramétricas", description = "Endpoints de administración de listas paramétricas del sistema")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ListaParametricaController {

    private final ListaParametricaService service;

    public ListaParametricaController(ListaParametricaService service) {
        this.service = service;
    }

    @Operation(
        summary = "Listar ítems de una lista paramétrica",
        description = "Retorna los ítems (valor, orden y estado) de la lista indicada, filtrando solo los activos cuando 'soloActivos' es true."
    )
    @GetMapping("/{listaClave}")
    public ResponseEntity<ApiResponse<List<ListaParametricaItemDTO>>> listar(
            @Parameter(description = "Clave de la lista paramétrica") @PathVariable String listaClave,
            @Parameter(description = "Si es true retorna únicamente los ítems activos") @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(ApiResponse.success(
                service.listarPorClave(listaClave, soloActivos),
                "Items listados correctamente"));
    }

    @Operation(summary = "Listar valores activos de una lista paramétrica")
    @GetMapping("/{listaClave}/valores")
    public ResponseEntity<ApiResponse<List<String>>> listarValores(@Parameter(description = "Clave de la lista paramétrica") @PathVariable String listaClave) {
        return ResponseEntity.ok(ApiResponse.success(
                service.listarValoresActivos(listaClave),
                "Valores listados correctamente"));
    }

    @Operation(
        summary = "Reemplazar los valores de una lista paramétrica",
        description = "Guarda el nombre del campo, la descripción y el conjunto completo de valores de la lista. Requiere permiso SISTEMA:CONFIGURAR."
    )
    @PutMapping("/{listaClave}/valores")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> guardarValores(
            @Parameter(description = "Clave de la lista paramétrica") @PathVariable String listaClave,
            @RequestBody Map<String, Object> body) {
        String nombreCampo = (String) body.getOrDefault("nombreCampo", listaClave);
        String descripcion = (String) body.getOrDefault("descripcion", "");
        @SuppressWarnings("unchecked")
        List<String> valores = (List<String>) body.getOrDefault("valores", List.of());
        service.guardarValores(listaClave, nombreCampo, descripcion, valores);
        return ResponseEntity.ok(ApiResponse.success("Valores guardados correctamente"));
    }
}
