package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProyectoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoUpdateDTO;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProyectoController implements IProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @Override
    @GetMapping("/activos-con-avance")
    public ResponseEntity<ApiResponse<List<Proyecto>>> getProyectosActivos(
            @RequestParam(defaultValue = "0") BigDecimal minimo) {
        List<Proyecto> proyectos = proyectoService.obtenerProyectosActivosConAvance(minimo);

        if (proyectos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ApiResponse.success(proyectos, "Proyectos activos obtenidos con éxito"));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Proyecto>> getProyecto(@PathVariable String id) {
        Proyecto proyecto = proyectoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success(proyecto, "Proyecto encontrado con éxito"));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Proyecto>> crearProyecto(@RequestBody ProyectoCreateDTO dto) {
        Proyecto nuevo = proyectoService.crearProyecto(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(nuevo, "Proyecto creado exitosamente"));
    }

    @Override
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Proyecto>> actualizarProyecto(
            @PathVariable String id, @RequestBody ProyectoUpdateDTO dto) {
        Proyecto actualizado = proyectoService.actualizarProyecto(id, dto);
        return ResponseEntity.ok(ApiResponse.success(actualizado, "Proyecto actualizado correctamente"));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarProyecto(@PathVariable String id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.ok(ApiResponse.success("Proyecto eliminado con éxito"));
    }
}