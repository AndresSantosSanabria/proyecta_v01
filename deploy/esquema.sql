-- =============================================================================
-- esquema.sql - Plantilla de inicializacion de esquema (PostgreSQL 16)
-- PROYECTA

-- El esquema lo crea 01-create-schema.sql (se repite aqui por idempotencia):
CREATE SCHEMA IF NOT EXISTS proyecta_db;

-- ---------------------------------------------------------------------------
-- Tabla de EJEMPLO (ilustrativa). No forma parte de la aplicacion; puede
-- eliminarse o renombrarse sin afectar el esquema de Flyway.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS proyecta_db.ejemplo_categoria (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ
);

COMMENT ON TABLE proyecta_db.ejemplo_categoria
    IS 'Tabla de ejemplo - plantilla de inicializacion manual (deploy/esquema.sql)';

-- Datos semilla idempotentes:
INSERT INTO proyecta_db.ejemplo_categoria (nombre, descripcion)
VALUES
    ('GENERAL', 'Categoria general de ejemplo'),
    ('ACTIVO',  'Registros activos por defecto')
ON CONFLICT (nombre) DO NOTHING;
