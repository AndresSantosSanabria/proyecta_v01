package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":6101,"username":"maria.gomez","eventCode":"PROJECT_DELIVERABLE_DUE","projectId":"PROY-CUN-2026-008","enabled":true,"emailEnabled":false}
    """)
public record NotificationPreferenceDTO(
        Long id,
        String username,
        String eventCode,
        String projectId,
        Boolean enabled,
        Boolean emailEnabled
) {
}
