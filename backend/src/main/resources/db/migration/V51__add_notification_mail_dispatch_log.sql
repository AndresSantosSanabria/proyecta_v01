CREATE TABLE IF NOT EXISTS proyecta_db.notification_mail_dispatch_log (
    id UUID PRIMARY KEY,
    recipient VARCHAR(200) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_mail_dispatch_log_recipient
    ON proyecta_db.notification_mail_dispatch_log(recipient);

CREATE INDEX IF NOT EXISTS idx_notification_mail_dispatch_log_status
    ON proyecta_db.notification_mail_dispatch_log(status);

CREATE INDEX IF NOT EXISTS idx_notification_mail_dispatch_log_created_at
    ON proyecta_db.notification_mail_dispatch_log(created_at);
