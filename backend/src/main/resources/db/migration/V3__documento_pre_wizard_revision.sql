-- =============================================================================
-- PROYECTA — Revisión individual de documentos pre-wizard
-- Fecha: 2026-09-22
-- Base de datos: PostgreSQL 15+
-- Esquema: proyecta_db
--
-- Una fila por (proyecto_id, tipo_documento) de los 3 tipos pre-wizard:
-- VIABILIZACION, PLAN_COMUNICACIONES, MATRIZ_RIESGOS_VIABILIDAD.
-- Estados: PENDIENTE | APROBADO | DEVUELTO
-- =============================================================================

CREATE TABLE documento_pre_wizard_revision (
    documento_pre_wizard_revision_id BIGSERIAL   PRIMARY KEY,
    proyecto_id                      VARCHAR(30) NOT NULL,
    tipo_documento                   VARCHAR(30) NOT NULL,
    estado                           VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    observacion                      VARCHAR(1000),
    revisado_por                     VARCHAR(200),
    revisado_en                      TIMESTAMP,
    creado_en                        TIMESTAMP    NOT NULL DEFAULT NOW(),
    actualizado_en                   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pre_wizard_revision UNIQUE (proyecto_id, tipo_documento),
    CONSTRAINT chk_pre_wizard_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'DEVUELTO')),
    CONSTRAINT chk_pre_wizard_tipo CHECK (tipo_documento IN (
        'VIABILIZACION',
        'PLAN_COMUNICACIONES',
        'MATRIZ_RIESGOS_VIABILIDAD'
    ))
);

CREATE INDEX idx_pre_wizard_revision_proyecto
    ON documento_pre_wizard_revision (proyecto_id);
