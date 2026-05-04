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

/**
 * Controller para el Cierre Formal de Proyectos.
 *
 * Expone POST /api/proyectos/{id}/cierre
 * Seguridad: Solo roles ADMINISTRADOR (admin) o GESTOR_TIC (gestor) pueden ejecutar el cierre.
 *
 * Nota: El header X-User-Id se usa como mecanismo de autenticación ligero.
 * En producción debe integrarse con el JWT del SecurityConfig.
 */
@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProjectClosureController implements IProjectClosureController {

    // Roles autorizados para ejecutar el cierre del proyecto
    private static final Set<Rol> ROLES_AUTORIZADOS = Set.of(Rol.admin, Rol.gestor);

    private final ProjectClosureService closureService;
    private final UsuarioRepository usuarioRepository;

    public ProjectClosureController(ProjectClosureService closureService,
                                    UsuarioRepository usuarioRepository) {
        this.closureService = closureService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Ejecuta el cierre formal del proyecto.
     *
     * @param id      ID del proyecto (path variable)
     * @param userId  ID del usuario autenticado (header X-User-Id)
     * @param request DTO con el resumen ejecutivo del cierre
     * @return CierreProyectoResponse con resultado de la operación
     */
    @Override
    @PostMapping("/{id}/cierre")
    public ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @PathVariable String id,
            @RequestHeader("X-User-Id") Integer userId,
            @Valid @RequestBody CierreProyectoRequest request) {

        verificarAutorizacion(userId);

        CierreProyectoResponse response = closureService.cerrarProyecto(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica que el usuario tenga un rol autorizado para cerrar proyectos.
     * Principio SOLID – Single Responsibility: extrae la lógica de autorización.
     *
     * @param userId ID del usuario a verificar
     * @throws ForbiddenException si el usuario no existe o su rol no está autorizado
     */
    private void verificarAutorizacion(Integer userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuario no encontrado o no autorizado."));

        if (!ROLES_AUTORIZADOS.contains(usuario.getRol())) {
            throw new ForbiddenException(
                    "Acceso denegado. Solo los roles ADMINISTRADOR o GESTOR_TIC pueden cerrar proyectos. " +
                    "Rol actual: " + usuario.getRol()
            );
        }
    }
}
