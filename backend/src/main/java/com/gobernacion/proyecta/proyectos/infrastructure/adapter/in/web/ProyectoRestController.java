package com.gobernacion.proyecta.proyectos.infrastructure.adapter.in.web;

import com.gobernacion.proyecta.proyectos.domain.port.in.ProyectoUseCase;
import com.gobernacion.proyecta.proyectos.domain.model.Proyecto;
import com.gobernacion.proyecta.proyectos.application.dto.ProyectoResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hex/proyectos") // Usando /hex/ para distinguir mientras conviven
@CrossOrigin(origins = "*")
public class ProyectoRestController {

    private final ProyectoUseCase proyectoUseCase;

    public ProyectoRestController(ProyectoUseCase proyectoUseCase) {
        this.proyectoUseCase = proyectoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ProyectoResponseDTO>> listar() {
        return ResponseEntity.ok(proyectoUseCase.listarTodos().stream()
                .map(p -> new ProyectoResponseDTO(p.getId(), p.getNombre(), p.getDependencia(), p.getDirector(), p.getEstado(), p.getAvanceTotal(), p.getFechaInicio()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponseDTO> obtener(@PathVariable String id) {
        return proyectoUseCase.obtenerPorId(id)
                .map(p -> new ProyectoResponseDTO(p.getId(), p.getNombre(), p.getDependencia(), p.getDirector(), p.getEstado(), p.getAvanceTotal(), p.getFechaInicio()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<Void> cerrar(@PathVariable String id) {
        proyectoUseCase.cerrarProyecto(id);
        return ResponseEntity.ok().build();
    }
}
