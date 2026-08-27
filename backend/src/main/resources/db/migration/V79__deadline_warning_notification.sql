INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
('ENTREGABLE_DEADLINE_WARNING', 'Entregable proximo a vencer', 'Alerta cuando un entregable esta proximo a su fecha limite.', 'BUSINESS', TRUE, TRUE, TRUE),
('ENTREGABLE_OVERDUE_REMINDER', 'Entregable vencido recordatorio', 'Recordatorio periodico de entregables vencidos que aun no han sido entregados.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('ENTREGABLE_DEADLINE_WARNING', TRUE, TRUE, 'WARNING', 'GLOBAL',
 'Alerta de vencimiento: {{entregableNombre}} - {{diasRestantes}} dias',
 '<p>El entregable <strong>{{entregableNombre}}</strong> del proyecto <strong>{{projectName}}</strong> vence en <strong>{{diasRestantes}} dias</strong> (fecha limite: {{fechaLimite}}).</p><p>Por favor, asegurese de gestionar la evidencia correspondiente antes de la fecha de corte.</p>',
 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('ENTREGABLE_OVERDUE_REMINDER', TRUE, TRUE, 'ERROR', 'GLOBAL',
 'Recordatorio: {{entregableNombre}} vencido hace {{diasVencido}} dias',
 '<p>El entregable <strong>{{entregableNombre}}</strong> del proyecto <strong>{{projectName}}</strong> se encuentra vencido desde hace <strong>{{diasVencido}} dias</strong> (fecha limite: {{fechaLimite}}).</p><p>Este es un recordatorio periodico. Por favor, regularice la entrega lo antes posible.</p>',
 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;

INSERT INTO proyecta_db.system_parameters (param_key, param_value, descripcion)
VALUES
('notification_deadline_warning_interval_days', '60', 'Intervalo en dias entre notificaciones repetidas de entregables proximos a vencer (default: 60 = 2 meses)'),
('notification_overdue_reminder_interval_days', '8', 'Intervalo en dias entre recordatorios repetidos de entregables vencidos (default: 8)')
ON CONFLICT (param_key) DO NOTHING;
