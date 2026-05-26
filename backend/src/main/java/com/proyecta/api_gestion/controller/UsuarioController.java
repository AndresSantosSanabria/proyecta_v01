package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IUsuarioController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class UsuarioController implements IUsuarioController {

    private final LocalUserAuthorizationService localUserAuthorizationService;

    public UsuarioController(LocalUserAuthorizationService localUserAuthorizationService) {
        this.localUserAuthorizationService = localUserAuthorizationService;
    }

    @Override
    public ResponseEntity<ApiResponse<UsuarioDTO>> getMe(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        Usuario usuario = localUserAuthorizationService.validateAndTouch(authentication);

        String nombre = firstNonBlank(
                usuario.getNombre(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                "Usuario Keycloak");

        String correo = firstNonBlank(
                usuario.getCorreo(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                "");

        String rolCodigo = firstNonBlank(usuario.getRolCodigo(), "SIN_ROL");
        String rolNombre = usuario.getRolConfig() != null ? usuario.getRolConfig().getNombre() : rolCodigo;
        Integer nivelAcceso = usuario.getRolConfig() != null ? usuario.getRolConfig().getNivelAcceso() : null;

        String dependencia = firstNonBlank(
                jwt.getClaimAsString("department"),
                jwt.getClaimAsString("organizational_unit"),
                jwt.getClaimAsString("preferred_username"),
                "No definida");

        UsuarioDTO user = new UsuarioDTO(
                usuario.getId(),
                nombre,
                correo,
                rolCodigo,
                rolNombre,
                nivelAcceso,
                dependencia,
                usuario.getActivo(),
                usuario.getUltimoAcceso());

        return ResponseEntity.ok(ApiResponse.success(user, "Perfil de usuario recuperado"));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
