package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IUsuarioController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.GlobalNotificationUpdateRequest;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.validation.Valid;
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
    private final SeguridadUsuarioRepository seguridadUsuarioRepository;
    private final KeycloakIdentityExtractor identityExtractor;

    public UsuarioController(LocalUserAuthorizationService localUserAuthorizationService,
                             SeguridadUsuarioRepository seguridadUsuarioRepository,
                             KeycloakIdentityExtractor identityExtractor) {
        this.localUserAuthorizationService = localUserAuthorizationService;
        this.seguridadUsuarioRepository = seguridadUsuarioRepository;
        this.identityExtractor = identityExtractor;
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

        String username = identityExtractor.resolveUsername(authentication);
        Boolean recibirNotificacionesGlobales = resolveGlobalNotificationsFlag(username);

        UsuarioDTO user = new UsuarioDTO(
                usuario.getId(),
                nombre,
                correo,
                rolCodigo,
                rolNombre,
                nivelAcceso,
                dependencia,
                usuario.getActivo(),
                usuario.getUltimoAcceso(),
                recibirNotificacionesGlobales);

        return ResponseEntity.ok(ApiResponse.success(user, "Perfil de usuario recuperado"));
    }

    @Override
    public ResponseEntity<ApiResponse<Boolean>> updateGlobalNotifications(
            @Valid GlobalNotificationUpdateRequest request,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        SeguridadUsuario segUsuario = seguridadUsuarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> seguridadUsuarioRepository.findByCorreoIgnoreCase(username))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        Boolean newValue = Boolean.TRUE.equals(request.recibirNotificacionesGlobales());
        segUsuario.setRecibirNotificacionesGlobales(newValue);
        seguridadUsuarioRepository.save(segUsuario);

        return ResponseEntity.ok(ApiResponse.success(newValue, newValue
                ? "Notificaciones globales activadas"
                : "Notificaciones globales desactivadas"));
    }

    private Boolean resolveGlobalNotificationsFlag(String username) {
        if (username == null || username.isBlank()) return false;
        return seguridadUsuarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> seguridadUsuarioRepository.findByCorreoIgnoreCase(username))
                .map(SeguridadUsuario::getRecibirNotificacionesGlobales)
                .orElse(false);
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
