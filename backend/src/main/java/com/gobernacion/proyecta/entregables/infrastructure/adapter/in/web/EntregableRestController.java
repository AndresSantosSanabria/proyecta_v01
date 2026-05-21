package com.gobernacion.proyecta.entregables.infrastructure.adapter.in.web;

import com.gobernacion.proyecta.entregables.domain.port.in.EntregableUseCase;
import com.gobernacion.proyecta.entregables.domain.model.Entregable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hex/entregables")
@CrossOrigin(origins = "*")
public class EntregableRestController {

    private final EntregableUseCase entregableUseCase;

    public EntregableRestController(EntregableUseCase entregableUseCase) {
        this.entregableUseCase = entregableUseCase;
    }

    @GetMapping("/hito/{hitoId}")
    public ResponseEntity<List<Entregable>> listarPorHito(@PathVariable Integer hitoId) {
        return ResponseEntity.ok(entregableUseCase.listarPorHito(hitoId));
    }

    @PostMapping("/{id}/completar")
    public ResponseEntity<Void> darConformidad(
            @PathVariable Integer id,
            @RequestParam LocalDate fechaEntrega,
            @RequestParam String archivoPdf) {
        entregableUseCase.darConformidad(id, fechaEntrega, archivoPdf);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        entregableUseCase.eliminarEntregable(id);
        return ResponseEntity.noContent().build();
    }
}
