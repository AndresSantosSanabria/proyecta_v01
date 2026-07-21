package com.proyecta.api_gestion.service.notification;

public record NotificationSendResult(
        boolean success,
        NotificationDeliveryStatus status,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationSendResult sent(String providerMessageId) {
        return new NotificationSendResult(true, NotificationDeliveryStatus.SENT, providerMessageId, null);
    }

    public static NotificationSendResult skipped(String reason) {
        return new NotificationSendResult(true, NotificationDeliveryStatus.SKIPPED, null, reason);
    }

    public static NotificationSendResult failed(String errorMessage) {
        return new NotificationSendResult(false, NotificationDeliveryStatus.FAILED, null, errorMessage);
    }
}
