package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationPreference;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationPreferenceRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationPreferenceService {
    private final NotificationPreferenceRepositoryPort preferenceRepositoryPort;
    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;

    public NotificationPreferenceService(NotificationPreferenceRepositoryPort preferenceRepositoryPort, SeguridadUsuarioRepositoryPort usuarioRepositoryPort) {
        this.preferenceRepositoryPort = preferenceRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreference> listAll() {
        return preferenceRepositoryPort.findAll();
    }

    @Transactional
    public NotificationPreference upsert(String username, String eventCode, String projectId, boolean enabled, boolean emailEnabled) {
        SeguridadUsuario user = usuarioRepositoryPort.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        NotificationPreference preference = preferenceRepositoryPort
                .findByUserUsernameIgnoreCaseAndEventCodeAndProjectId(username, eventCode, projectId)
                .orElseGet(NotificationPreference::new);
        preference.setUser(user);
        preference.setEventCode(eventCode);
        preference.setProjectId(projectId);
        preference.setEnabled(enabled);
        preference.setEmailEnabled(emailEnabled);
        return preferenceRepositoryPort.save(preference);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String username, String eventCode, String projectId) {
        return preferenceRepositoryPort.findByUserUsernameIgnoreCaseAndEventCodeAndProjectId(username, eventCode, projectId)
                .map(pref -> Boolean.TRUE.equals(pref.getEnabled()) && Boolean.TRUE.equals(pref.getEmailEnabled()))
                .orElse(true);
    }
}
