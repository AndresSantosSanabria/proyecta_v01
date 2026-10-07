package com.proyecta.api_gestion.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    private final NotificationOrchestratorService orchestratorService;

    public NotificationDispatchListener(NotificationOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationContext context) {
        try {
            orchestratorService.dispatch(context);
        } catch (Exception ex) {
            log.error("[Notification] Dispatch failed for event {} — {}", context.eventType(), ex.getMessage(), ex);
        }
    }
}
