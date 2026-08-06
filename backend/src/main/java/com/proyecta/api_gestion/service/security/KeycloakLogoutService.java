package com.proyecta.api_gestion.service.security;

import com.proyecta.api_gestion.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KeycloakLogoutService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakLogoutService.class);

    private final String issuerUri;
    private final String clientId;
    private final String clientSecret;
    private final String postLogoutRedirectUri;

    public KeycloakLogoutService(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${gob.security.resource-client-ids}") String clientId,
            @Value("${gob.security.keycloak.client-secret:}") String clientSecret,
            @Value("${gob.security.logout.post-redirect-uri:http://localhost:5173/}") String postLogoutRedirectUri) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public LogoutResult prepareLogout(String idTokenHint, String refreshToken) {
        if (idTokenHint == null || idTokenHint.isBlank()) {
            throw new BadRequestException("El id_token es obligatorio para cerrar la sesión SSO en Keycloak.");
        }

        boolean refreshTokenRevoked = false;
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRevoked = revokeRefreshToken(refreshToken);
        }

        String logoutUrl = UriComponentsBuilder
                .fromUriString(issuerUri + "/protocol/openid-connect/logout")
                .queryParam("id_token_hint", idTokenHint)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                .build()
                .encode()
                .toUriString();

        return new LogoutResult(logoutUrl, refreshTokenRevoked);
    }

    private boolean revokeRefreshToken(String refreshToken) {
        try {
            String revokeEndpoint = issuerUri + "/protocol/openid-connect/revoke";

            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            if (clientSecret != null && !clientSecret.isBlank()) {
                body.add("client_secret", clientSecret);
            }
            body.add("token", refreshToken);
            body.add("token_type_hint", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = rest.postForEntity(revokeEndpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Refresh token revocado correctamente en Keycloak");
                return true;
            }

            logger.warn("Keycloak devolvió {} al revocar refresh token", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error al revocar refresh token en Keycloak: {}", e.getMessage());
        }
        return false;
    }

    public record LogoutResult(String logoutUrl, boolean refreshTokenRevoked) {}
}
