-- V6: Analitica institucional y catalogo dinamico de matriz de riesgos
-- Fecha: 2026-05-28

-- ================================================================
-- Matriz dinamica de riesgos con color institucional
-- ================================================================
ALTER TABLE proyecta_db.matriz_riesgo
    ADD COLUMN IF NOT EXISTS color VARCHAR(20);

UPDATE proyecta_db.matriz_riesgo
SET color = CASE UPPER(COALESCE(nivel_riesgo, ''))
    WHEN 'BAJO' THEN '#22c55e'
    WHEN 'MODERADO' THEN '#f59e0b'
    WHEN 'ALTO' THEN '#ef4444'
    WHEN 'CRITICO' THEN '#dc2626'
    ELSE '#94a3b8'
END,
    puntaje = CASE UPPER(COALESCE(probabilidad, ''))
        WHEN 'BAJA' THEN CASE UPPER(COALESCE(impacto, ''))
            WHEN 'BAJO' THEN 1
            WHEN 'MEDIO' THEN 2
            WHEN 'ALTO' THEN 3
            ELSE 1
        END
        WHEN 'MEDIA' THEN CASE UPPER(COALESCE(impacto, ''))
            WHEN 'BAJO' THEN 2
            WHEN 'MEDIO' THEN 4
            WHEN 'ALTO' THEN 6
            ELSE 2
        END
        WHEN 'ALTA' THEN CASE UPPER(COALESCE(impacto, ''))
            WHEN 'BAJO' THEN 3
            WHEN 'MEDIO' THEN 6
            WHEN 'ALTO' THEN 9
            ELSE 3
        END
        ELSE COALESCE(puntaje, 1)
    END
WHERE color IS NULL;

ALTER TABLE proyecta_db.matriz_riesgo
    ALTER COLUMN color SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_matriz_riesgo_prob_imp
    ON proyecta_db.matriz_riesgo(probabilidad, impacto);

CREATE INDEX IF NOT EXISTS idx_matriz_riesgo_nivel
    ON proyecta_db.matriz_riesgo(nivel_riesgo);

INSERT INTO proyecta_db.matriz_riesgo (probabilidad, impacto, nivel_riesgo, color, puntaje)
VALUES
    ('BAJA', 'BAJO', 'BAJO', '#22c55e', 1),
    ('BAJA', 'MEDIO', 'BAJO', '#22c55e', 2),
    ('BAJA', 'ALTO', 'MODERADO', '#f59e0b', 3),
    ('MEDIA', 'BAJO', 'BAJO', '#22c55e', 2),
    ('MEDIA', 'MEDIO', 'MODERADO', '#f59e0b', 4),
    ('MEDIA', 'ALTO', 'ALTO', '#ef4444', 6),
    ('ALTA', 'BAJO', 'MODERADO', '#f59e0b', 3),
    ('ALTA', 'MEDIO', 'ALTO', '#ef4444', 6),
    ('ALTA', 'ALTO', 'CRITICO', '#dc2626', 9)
ON CONFLICT (probabilidad, impacto) DO UPDATE
SET nivel_riesgo = EXCLUDED.nivel_riesgo,
    color = EXCLUDED.color,
    puntaje = EXCLUDED.puntaje;

COMMENT ON TABLE proyecta_db.matriz_riesgo IS 'Catalogo dinamico para la valoracion de riesgos por probabilidad e impacto.';
COMMENT ON COLUMN proyecta_db.matriz_riesgo.color IS 'Color institucional asociado al nivel resultante.';

-- ================================================================
-- Snapshot analitico del portafolio
-- ================================================================
CREATE TABLE IF NOT EXISTS proyecta_db.analitica_portafolio_snapshot (
    snapshot_id BIGSERIAL PRIMARY KEY,
    corte_calculo TIMESTAMPTZ NOT NULL UNIQUE,
    fuente VARCHAR(80) NOT NULL DEFAULT 'PORTAFOLIO',
    snapshot_json JSONB NOT NULL,
    fecha_generacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analitica_portafolio_snapshot_fecha
    ON proyecta_db.analitica_portafolio_snapshot(fecha_generacion DESC);

COMMENT ON TABLE proyecta_db.analitica_portafolio_snapshot IS 'Snapshot historico de analitica global del portafolio con marca temporal con zona horaria.';
