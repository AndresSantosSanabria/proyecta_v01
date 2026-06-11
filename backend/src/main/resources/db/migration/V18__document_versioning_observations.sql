CREATE TABLE IF NOT EXISTS proyecta_db.documento_version (
    documento_version_id BIGSERIAL PRIMARY KEY,
    entregable_id INTEGER NOT NULL REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE,
    numero_version INTEGER NOT NULL,
    nombre_archivo_original VARCHAR(255) NOT NULL,
    archivo_storage VARCHAR(300) NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    fecha_entrega DATE,
    comentario_carga VARCHAR(1000),
    estado VARCHAR(30) NOT NULL,
    subido_por VARCHAR(200),
    subido_rol VARCHAR(80),
    subido_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT documento_version_estado_check
        CHECK (estado IN ('ACTUAL', 'HISTORICA', 'REVERTIDA')),
    CONSTRAINT documento_version_numero_uk
        UNIQUE (entregable_id, numero_version)
);

CREATE INDEX IF NOT EXISTS idx_documento_version_entregable
    ON proyecta_db.documento_version(entregable_id);

CREATE INDEX IF NOT EXISTS idx_documento_version_estado
    ON proyecta_db.documento_version(entregable_id, estado);

CREATE TABLE IF NOT EXISTS proyecta_db.documento_observacion (
    documento_observacion_id BIGSERIAL PRIMARY KEY,
    entregable_id INTEGER NOT NULL REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE,
    documento_version_id BIGINT REFERENCES proyecta_db.documento_version(documento_version_id) ON DELETE SET NULL,
    observacion TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    creada_por VARCHAR(200),
    creada_rol VARCHAR(80),
    creada_en TIMESTAMP NOT NULL DEFAULT NOW(),
    subsanada_por VARCHAR(200),
    subsanada_en TIMESTAMP,
    comentario_subsanacion VARCHAR(1000),
    cerrada_por VARCHAR(200),
    cerrada_en TIMESTAMP,
    CONSTRAINT documento_observacion_estado_check
        CHECK (estado IN ('ABIERTA', 'SUBSANADA', 'CERRADA'))
);

CREATE INDEX IF NOT EXISTS idx_documento_observacion_entregable
    ON proyecta_db.documento_observacion(entregable_id);

CREATE INDEX IF NOT EXISTS idx_documento_observacion_estado
    ON proyecta_db.documento_observacion(entregable_id, estado);

CREATE TABLE IF NOT EXISTS proyecta_db.documento_auditoria (
    documento_auditoria_id BIGSERIAL PRIMARY KEY,
    entregable_id INTEGER NOT NULL REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE,
    documento_version_id BIGINT REFERENCES proyecta_db.documento_version(documento_version_id) ON DELETE SET NULL,
    documento_observacion_id BIGINT REFERENCES proyecta_db.documento_observacion(documento_observacion_id) ON DELETE SET NULL,
    accion VARCHAR(50) NOT NULL,
    actor VARCHAR(200),
    actor_rol VARCHAR(80),
    fecha TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS idx_documento_auditoria_entregable
    ON proyecta_db.documento_auditoria(entregable_id);

CREATE INDEX IF NOT EXISTS idx_documento_auditoria_accion
    ON proyecta_db.documento_auditoria(accion);

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
SELECT 'DOCUMENTO:HISTORIAL', 'Ver historico documental', 'Permite consultar versiones anteriores de evidencias', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM proyecta_db.permisos WHERE codigo = 'DOCUMENTO:HISTORIAL'
);

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
SELECT 'DOCUMENTO:REVERTIR', 'Revertir documento', 'Permite restaurar una version anterior de una evidencia', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM proyecta_db.permisos WHERE codigo = 'DOCUMENTO:REVERTIR'
);
