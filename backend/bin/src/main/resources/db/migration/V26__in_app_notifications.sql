CREATE TABLE IF NOT EXISTS proyecta_db.notification_in_app (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES proyecta_db.usuarios(id),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    event_code VARCHAR(120) NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    source_entity_id VARCHAR(80),
    severity VARCHAR(30)
);

CREATE INDEX IF NOT EXISTS idx_notification_in_app_recipient_created
    ON proyecta_db.notification_in_app (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_in_app_unread
    ON proyecta_db.notification_in_app (recipient_user_id, read_status);
