package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectClosureController;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.enums.Rol;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
public class ProjectClosureController implements IProjectClosureController {

    private static final Set<Rol> ROLES_AUTORIZADOS = Set.of(Rol.ADMINISTRADOR, Rol.GESTOR_PROYECTOS_TI);

    private final ProjectClosureService closureService;
    private final UsuarioRepository usuarioRepository;

    public ProjectClosureController(ProjectClosureService closureService,
                                    UsuarioRepository usuarioRepository) {
        this.closureService = closureService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @PostMapping("/{id}/cierre")
    public ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @PathVariable String id,
            @Valid @RequestBody CierreProyectoRequest request) {

        CierreProyectoResponse response = closureService.cerrarProyecto(id, request);
        return ResponseEntity.ok(response);
    }

}
