-- V100: Add {{targetUrl}} to SECURITY_ROLE_UPDATED and SECURITY_USER_UPDATED templates
-- so all notifications include a direct link to the relevant page.

-- SECURITY_ROLE_UPDATED
UPDATE proyecta_db.notification_template
SET body_template = 'Se modifico el rol "{{roleCode}}" en la configuracion de seguridad del sistema. Si tiene permisos afectados, verifique que su acceso siga funcionando correctamente.'
    || chr(10)
    || chr(10)
    || 'Acceda directamente desde este enlace:'
    || chr(10)
    || '{{targetUrl}}'
WHERE event_code = 'SECURITY_ROLE_UPDATED';

-- SECURITY_USER_UPDATED
UPDATE proyecta_db.notification_template
SET body_template = 'Se actualizaron datos de seguridad para {{username}}.'
    || chr(10)
    || chr(10)
    || 'Acceda directamente desde este enlace:'
    || chr(10)
    || '{{targetUrl}}'
WHERE event_code = 'SECURITY_USER_UPDATED';
