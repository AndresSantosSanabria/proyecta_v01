-- Adjunto multiple de soluciones para la matriz de riesgos.

CREATE TABLE IF NOT EXISTS proyecta_db.riesgo_solucion_adjunto (
    riesgo_solucion_adjunto_id BIGSERIAL PRIMARY KEY,
    riesgo_id INTEGER NOT NULL,
    nombre_original VARCHAR(300) NOT NULL,
    nombre_almacenado VARCHAR(300) NOT NULL,
    ruta_almacenamiento VARCHAR(120) NOT NULL DEFAULT 'riesgos-soluciones',
    mime_type VARCHAR(120) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_riesgo_solucion_adjunto_riesgo
        FOREIGN KEY (riesgo_id) REFERENCES proyecta_db.riesgos(riesgo_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_riesgo_solucion_adjunto_riesgo
    ON proyecta_db.riesgo_solucion_adjunto(riesgo_id);
