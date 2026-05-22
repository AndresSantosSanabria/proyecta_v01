package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IUsuarioController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('app_access')")
public class UsuarioController implements IUsuarioController {

    @Override
    public ResponseEntity<ApiResponse<UsuarioDTO>> getMe(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String nombre = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                "Usuario Keycloak");

        String correo = firstNonBlank(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                "");

        String rol = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));

        String dependencia = firstNonBlank(
                jwt.getClaimAsString("department"),
                jwt.getClaimAsString("organizational_unit"),
                jwt.getClaimAsString("preferred_username"),
                "No definida");

        UsuarioDTO user = new UsuarioDTO(nombre, correo, rol, dependencia);
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
