-- Migration: Create documento table
-- Date: 2026-05-20
-- Description: Tabla para registrar metadatos de documentos cargados al sistema

CREATE TABLE IF NOT EXISTS proyecta_db.documento (
    id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    tipo_documento VARCHAR(30) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    url_descarga VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_documento_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_documento CHECK (tipo_documento IN ('VIABILIZACION', 'ACTA_CONSTITUCION', 'CRONOGRAMA', 'PLAN_COMUNICACIONES')),
    CONSTRAINT chk_tamano_positivo CHECK (tamano_bytes > 0)
);

CREATE INDEX IF NOT EXISTS idx_documento_proyecto ON proyecta_db.documento(proyecto_id);
CREATE INDEX IF NOT EXISTS idx_documento_tipo ON proyecta_db.documento(tipo_documento);
CREATE INDEX IF NOT EXISTS idx_documento_proyecto_tipo ON proyecta_db.documento(proyecto_id, tipo_documento);

COMMENT ON TABLE proyecta_db.documento IS 'Registro de metadatos de documentos cargados por proyecto';
COMMENT ON COLUMN proyecta_db.documento.nombre_original IS 'Nombre original del archivo subido por el usuario';
COMMENT ON COLUMN proyecta_db.documento.nombre_almacenado IS 'Nombre único generado con UUID para evitar colisiones';
COMMENT ON COLUMN proyecta_db.documento.ruta_almacenamiento IS 'Subdirectorio relativo dentro del storage (ej: documentos)';
COMMENT ON COLUMN proyecta_db.documento.mime_type IS 'Tipo MIME real detectado en el backend';
COMMENT ON COLUMN proyecta_db.documento.tamano_bytes IS 'Tamaño del archivo en bytes';
