package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.SeguridadAutorizacionMeDTO;
import com.proyecta.api_gestion.service.security.dynamic.SecurityAdministrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authz")
public class AuthorizationController {

    private final SecurityAdministrationService securityAdministrationService;

    public AuthorizationController(SecurityAdministrationService securityAdministrationService) {
        this.securityAdministrationService = securityAdministrationService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeguridadAutorizacionMeDTO>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.getAuthorizationFor(authentication),
                "Autorizacion resuelta correctamente"));
    }
}
