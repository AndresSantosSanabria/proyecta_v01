-- Tabla de versiones para documentos de proyecto (VIABILIZACION, ACTA_CONSTITUCION, CRONOGRAMA, PLAN_COMUNICACIONES)
-- Permite control de versiones: al subir un documento que ya existe, la version anterior se marca como HISTORICA
-- y la nueva se crea como ACTUAL. Se requiere observacion al reemplazar.

CREATE TABLE proyecta_db.documento_proyecto_version (
    documento_proyecto_version_id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    tipo_documento VARCHAR(30) NOT NULL,
    numero_version INTEGER NOT NULL,
    nombre_archivo_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    observacion VARCHAR(1000),
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('ACTUAL', 'HISTORICA')),
    subido_por VARCHAR(200),
    subido_rol VARCHAR(80),
    subido_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT documento_proyecto_version_uk UNIQUE (proyecto_id, tipo_documento, numero_version),
    CONSTRAINT fk_doc_proj_version_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE
);

CREATE INDEX idx_doc_proj_version_proyecto_tipo ON proyecta_db.documento_proyecto_version(proyecto_id, tipo_documento);

COMMENT ON TABLE proyecta_db.documento_proyecto_version IS 'Historico de versiones de documentos del proyecto (no se eliminan, se crean nuevas versiones)';
COMMENT ON COLUMN proyecta_db.documento_proyecto_version.estado IS 'ACTUAL = version vigente, HISTORICA = version reemplazada';
COMMENT ON COLUMN proyecta_db.documento_proyecto_version.observacion IS 'Motivo del cambio (obligatorio al reemplazar)';
