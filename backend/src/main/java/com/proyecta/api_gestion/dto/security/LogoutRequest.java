package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de logout")
public class LogoutRequest {

    @Schema(description = "ID token (id_token) del usuario, necesario como id_token_hint para cerrar sesión en Keycloak", example = "eyJhbGciOi...")
    private String idToken;

    @Schema(description = "Refresh token del usuario para revocarlo en Keycloak", example = "eyJhbGciOi...")
    private String refreshToken;

    public LogoutRequest() {}

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
