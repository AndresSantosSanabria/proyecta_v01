CREATE TABLE documento_dinamico (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    proyecto_id VARCHAR(30) NOT NULL,
    tipo_documento VARCHAR(100) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    url_descarga VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT idx_proyecto_tipo UNIQUE (proyecto_id, tipo_documento),
    FOREIGN KEY (proyecto_id) REFERENCES proyecto(proyecto_id)
);

CREATE INDEX idx_documento_dinamico_proyecto_tipo ON documento_dinamico(proyecto_id, tipo_documento);

ALTER TABLE entregable DROP CONSTRAINT IF EXISTS entregable_estado_check;
ALTER TABLE entregable ADD CONSTRAINT entregable_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'A_CONFORMIDAD', 'COMPLETADO', 'ATRASADO'));
