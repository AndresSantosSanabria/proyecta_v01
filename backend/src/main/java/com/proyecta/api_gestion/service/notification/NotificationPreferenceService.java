package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationPreference;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.notification.NotificationPreferenceRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository preferenceRepository;
    private final SeguridadUsuarioRepository usuarioRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository, SeguridadUsuarioRepository usuarioRepository) {
        this.preferenceRepository = preferenceRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreference> listAll() {
        return preferenceRepository.findAll();
    }

    @Transactional
    public NotificationPreference upsert(String username, String eventCode, String projectId, boolean enabled, boolean emailEnabled) {
        SeguridadUsuario user = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        NotificationPreference preference = preferenceRepository
                .findByUser_UsernameIgnoreCaseAndEventCodeAndProjectId(username, eventCode, projectId)
                .orElseGet(NotificationPreference::new);
        preference.setUser(user);
        preference.setEventCode(eventCode);
        preference.setProjectId(projectId);
        preference.setEnabled(enabled);
        preference.setEmailEnabled(emailEnabled);
        return preferenceRepository.save(preference);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String username, String eventCode, String projectId) {
        return preferenceRepository.findByUser_UsernameIgnoreCaseAndEventCodeAndProjectId(username, eventCode, projectId)
                .map(pref -> Boolean.TRUE.equals(pref.getEnabled()) && Boolean.TRUE.equals(pref.getEmailEnabled()))
                .orElse(true);
    }
}
