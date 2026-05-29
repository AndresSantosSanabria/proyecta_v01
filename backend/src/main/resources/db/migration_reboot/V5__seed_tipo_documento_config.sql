INSERT INTO proyecta_db.tipo_documento_config (codigo, nombre, descripcion, require_pdf, orden, activo)
SELECT v.codigo, v.nombre, v.descripcion, v.require_pdf, v.orden, v.activo
FROM (
    VALUES
        ('VIABILIZACION', 'Viabilizacion', 'Documento de viabilidad del proyecto', TRUE, 1, TRUE),
        ('ACTA_CONSTITUCION', 'Acta de Constitucion', 'Acta formal de constitucion del proyecto', TRUE, 2, TRUE),
        ('CRONOGRAMA', 'Cronograma', 'Plan de cronograma del proyecto', TRUE, 3, TRUE),
        ('PLAN_COMUNICACIONES', 'Plan de Comunicaciones', 'Plan de comunicaciones del proyecto', TRUE, 4, TRUE)
) AS v(codigo, nombre, descripcion, require_pdf, orden, activo)
WHERE NOT EXISTS (
    SELECT 1
    FROM proyecta_db.tipo_documento_config t
    WHERE t.codigo = v.codigo
);
