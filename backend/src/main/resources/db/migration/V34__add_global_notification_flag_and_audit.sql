-- Agregar columna a la tabla usuarios
ALTER TABLE proyecta_db.usuarios
ADD COLUMN recibir_notificaciones_globales BOOLEAN DEFAULT FALSE;

-- Crear tabla de auditoría
CREATE TABLE proyecta_db.notification_audit (
    id UUID PRIMARY KEY,
    event_code VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Índices para mejorar la búsqueda en la auditoría
CREATE INDEX idx_notification_audit_recipient ON proyecta_db.notification_audit(recipient);
CREATE INDEX idx_notification_audit_event_code ON proyecta_db.notification_audit(event_code);
CREATE INDEX idx_notification_audit_status ON proyecta_db.notification_audit(status);
