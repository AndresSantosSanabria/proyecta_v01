package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.PublicEvidenceAccess;
import com.proyecta.api_gestion.repository.PublicEvidenceAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class PublicEvidenceAccessService {

    private static final Logger log = LoggerFactory.getLogger(PublicEvidenceAccessService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PublicEvidenceAccessRepository repository;

    public PublicEvidenceAccessService(PublicEvidenceAccessRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String getOrCreateToken(Entregable entregable, String username) {
        var existing = repository.findByEntregable_IdAndActivoTrue(entregable.getId());
        if (existing.isPresent()) {
            return existing.get().getToken();
        }
        String token = generateToken();
        PublicEvidenceAccess access = new PublicEvidenceAccess();
        access.setEntregable(entregable);
        access.setToken(token);
        access.setActivo(true);
        access.setCreatedBy(username);
        repository.save(access);
        log.info("Token de evidencia publica creado para entregable {}: {}", entregable.getId(), token);
        return token;
    }

    @Transactional(readOnly = true)
    public Entregable resolveByToken(String token) {
        var access = repository.findByTokenAndActivoTrue(token)
                .orElseThrow(() -> new com.proyecta.api_gestion.exception.ResourceNotFoundException(
                        "Evidencia no encontrada o enlace expirado."));
        return access.getEntregable();
    }

    @Transactional
    public List<String> getTokensForProject(String proyectoId) {
        return repository.findAll().stream()
                .filter(a -> a.getActivo() && a.getEntregable() != null
                        && a.getEntregable().getHito() != null
                        && a.getEntregable().getHito().getFase() != null
                        && proyectoId.equals(a.getEntregable().getHito().getFase().getProyecto().getId()))
                .map(PublicEvidenceAccess::getToken)
                .toList();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
