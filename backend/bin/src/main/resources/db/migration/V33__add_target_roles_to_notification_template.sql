ALTER TABLE proyecta_db.notification_template
    ADD COLUMN target_roles TEXT;

COMMENT ON COLUMN proyecta_db.notification_template.target_roles
    IS 'Roles destinatarios de la notificacion, separados por coma. Si esta vacio, se envia a todos los roles.';
