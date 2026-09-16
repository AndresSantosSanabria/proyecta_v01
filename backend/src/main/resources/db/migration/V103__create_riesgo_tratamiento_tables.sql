-- V99: Crear tablas para historial iterativo de tratamientos de riesgos
-- Cada riesgo puede tener múltiples tratamientos, y cada tratamiento tiene sus propios archivos adjuntos.

CREATE TABLE riesgo_tratamiento (
    riesgo_tratamiento_id BIGSERIAL PRIMARY KEY,
    riesgo_id INTEGER NOT NULL REFERENCES riesgos(riesgo_id) ON DELETE CASCADE,
    iteracion INTEGER NOT NULL,
    comentario TEXT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_riesgo_tratamiento_iteracion UNIQUE (riesgo_id, iteracion)
);

CREATE INDEX idx_riesgo_tratamiento_riesgo ON riesgo_tratamiento(riesgo_id);

CREATE TABLE riesgo_tratamiento_adjunto (
    riesgo_tratamiento_adjunto_id BIGSERIAL PRIMARY KEY,
    riesgo_tratamiento_id BIGINT NOT NULL REFERENCES riesgo_tratamiento(riesgo_tratamiento_id) ON DELETE CASCADE,
    nombre_original VARCHAR(300) NOT NULL,
    nombre_almacenado VARCHAR(300) NOT NULL,
    ruta_almacenamiento VARCHAR(120) NOT NULL DEFAULT 'riesgos-tratamientos',
    mime_type VARCHAR(120) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_riesgo_tratamiento_adjunto_trat ON riesgo_tratamiento_adjunto(riesgo_tratamiento_id);
