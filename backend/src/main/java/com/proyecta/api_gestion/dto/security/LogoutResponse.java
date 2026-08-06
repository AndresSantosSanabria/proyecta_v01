package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de logout con URL para cerrar la sesión en Keycloak")
public class LogoutResponse {

    @Schema(description = "URL completa del end_session_endpoint de Keycloak con id_token_hint y post_logout_redirect_uri")
    private String logoutUrl;

    @Schema(description = "Indica si la sesión local de Spring fue invalidada")
    private boolean localSessionInvalidated;

    @Schema(description = "Indica si el refresh token fue revocado correctamente en Keycloak")
    private boolean refreshTokenRevoked;

    public LogoutResponse() {
    }

    public LogoutResponse(String logoutUrl, boolean localSessionInvalidated, boolean refreshTokenRevoked) {
        this.logoutUrl = logoutUrl;
        this.localSessionInvalidated = localSessionInvalidated;
        this.refreshTokenRevoked = refreshTokenRevoked;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public boolean isLocalSessionInvalidated() {
        return localSessionInvalidated;
    }

    public void setLocalSessionInvalidated(boolean localSessionInvalidated) {
        this.localSessionInvalidated = localSessionInvalidated;
    }

    public boolean isRefreshTokenRevoked() {
        return refreshTokenRevoked;
    }

    public void setRefreshTokenRevoked(boolean refreshTokenRevoked) {
        this.refreshTokenRevoked = refreshTokenRevoked;
    }
}
