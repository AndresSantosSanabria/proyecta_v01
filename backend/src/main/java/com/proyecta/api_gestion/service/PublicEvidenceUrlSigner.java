package com.proyecta.api_gestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Firma los enlaces publicos de evidencia con HMAC-SHA256 (?exp=&sig=) para que
 * sigan siendo accesibles sin inicio de sesión (requisito de negocio) pero no sean
 * enumerables por IDs secuenciales (CWE-639 / OWASP A01).
 *
 * El filtro {@code PublicEvidenceSecurityFilter} valida la firma en cada petición a
 * /api/v1/public/** excepto /evidencia/{token}, que ya usa un token opaco de 256 bits.
 *
 * El secreto proviene de PUBLIC_EVIDENCE_HMAC_SECRET. Si no está definido (dev local)
 * se genera una clave efímera: la app arranca, pero los enlaces firmados dejarán de
 * validarse al reiniciar. En producción DEBE configurarse la variable de entorno.
 */
@Component
public class PublicEvidenceUrlSigner {

    private static final Logger log = LoggerFactory.getLogger(PublicEvidenceUrlSigner.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final long expiryEpochSeconds;

    public PublicEvidenceUrlSigner(
            @Value("${gob.security.public-evidence.hmac-secret:}") String configuredSecret,
            @Value("${gob.security.public-evidence.expiry-days:30}") long expiryDays) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] ephemeral = new byte[32];
            new SecureRandom().nextBytes(ephemeral);
            this.secret = ephemeral;
            log.warn("PUBLIC_EVIDENCE_HMAC_SECRET no esta configurado: se genero una clave efimera. "
                    + "Los enlaces publicos firmados dejaran de validar tras reiniciar el servicio. "
                    + "Configure la variable de entorno para despliegues persistentes.");
        } else {
            this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
            log.info("Firma HMAC de enlaces publicos activa con secreto configurado ({} bytes).", this.secret.length);
        }
        long days = Math.max(1L, expiryDays);
        this.expiryEpochSeconds = days * 24L * 60L * 60L;
    }

    /**
     * Añade {@code exp} (fecha de expiración) y {@code sig} (HMAC del path) a un enlace público.
     * La firma cubre únicamente el path (sin query) para que parámetros como {@code inline}
     * puedan variar sin romper la validación.
     *
     * @param url enlace absoluto o path (puede incluir query existente)
     * @return el mismo enlace con {@code exp} y {@code sig} añadidos
     */
    public String appendSignature(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String path = extractPath(url);
        int queryIndex = path.indexOf('?');
        String canonicalPath = queryIndex >= 0 ? path.substring(0, queryIndex) : path;
        String existingQuery = queryIndex >= 0 ? path.substring(queryIndex + 1) : "";

        long exp = Instant.now().getEpochSecond() + expiryEpochSeconds;
        String sig = computeMacBase64(canonicalPath, exp);

        String separator = existingQuery.isEmpty() ? "?" : "&";
        return url + separator + "exp=" + exp + "&sig=" + sig;
    }

    /**
     * Valida la firma de un path público recibido por el filtro.
     * Comparación en tiempo constante para evitar ataques de timing (CWE-208).
     */
    public boolean isValid(String rawPath, String expParam, String sigParam) {
        if (rawPath == null || expParam == null || sigParam == null) {
            return false;
        }
        long exp;
        try {
            exp = Long.parseLong(expParam);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (Instant.now().getEpochSecond() > exp) {
            return false;
        }
        byte[] expected = computeMac(rawPath, exp);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(sigParam);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    private String computeMacBase64(String path, long exp) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(computeMac(path, exp));
    }

    private byte[] computeMac(String path, long exp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal((path + "|" + exp).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC del enlace publico.", ex);
        }
    }

    /**
     * Extrae solo el path (sin autoridad ni query) de una URL completa o de un path relativo.
     */
    private static String extractPath(String url) {
        int schemeIndex = url.indexOf("://");
        String candidate;
        if (schemeIndex >= 0) {
            int pathStart = url.indexOf('/', schemeIndex + 3);
            candidate = pathStart >= 0 ? url.substring(pathStart) : "/";
        } else {
            candidate = url;
        }
        int queryIndex = candidate.indexOf('?');
        return queryIndex >= 0 ? candidate.substring(0, queryIndex) : candidate;
    }
}
