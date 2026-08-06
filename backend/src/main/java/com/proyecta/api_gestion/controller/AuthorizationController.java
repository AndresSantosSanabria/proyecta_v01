package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.LogoutRequest;
import com.proyecta.api_gestion.dto.security.LogoutResponse;
import com.proyecta.api_gestion.dto.security.SeguridadAutorizacionMeDTO;
import com.proyecta.api_gestion.service.security.KeycloakLogoutService;
import com.proyecta.api_gestion.service.security.dynamic.RoleAliasService;
import com.proyecta.api_gestion.service.security.dynamic.SecurityAdministrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authz")
public class AuthorizationController {

    private final SecurityAdministrationService securityAdministrationService;
    private final RoleAliasService roleAliasService;
    private final KeycloakLogoutService keycloakLogoutService;

    public AuthorizationController(SecurityAdministrationService securityAdministrationService,
                                   RoleAliasService roleAliasService,
                                   KeycloakLogoutService keycloakLogoutService) {
        this.securityAdministrationService = securityAdministrationService;
        this.roleAliasService = roleAliasService;
        this.keycloakLogoutService = keycloakLogoutService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeguridadAutorizacionMeDTO>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.getAuthorizationFor(authentication),
                "Autorizacion resuelta correctamente"));
    }

    @GetMapping("/role-aliases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> roleAliases() {
        return ResponseEntity.ok(ApiResponse.success(
                roleAliasService.roleAliasesForClient(),
                "Alias de roles resueltos correctamente"));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            HttpServletRequest servletRequest,
            @RequestBody(required = false) LogoutRequest request) {
        String idToken = (request != null) ? request.getIdToken() : null;
        String refreshToken = (request != null) ? request.getRefreshToken() : null;

        KeycloakLogoutService.LogoutResult result = keycloakLogoutService.prepareLogout(idToken, refreshToken);

        HttpSession session = servletRequest.getSession(false);
        boolean localSessionInvalidated = false;
        if (session != null) {
            session.invalidate();
            localSessionInvalidated = true;
        }
        SecurityContextHolder.clearContext();

        LogoutResponse response = new LogoutResponse(
                result.logoutUrl(),
                localSessionInvalidated,
                result.refreshTokenRevoked());
        return ResponseEntity.ok(ApiResponse.success(response, "Logout preparado correctamente"));
    }
}
