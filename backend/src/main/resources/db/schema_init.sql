-- ============================================================
-- SCRIPT DE INICIALIZACIÓN COMPLETA — Sistema Proyecta
-- Base de datos: PostgreSQL
-- Schema:        proyecta_db
-- Generado:      2026-05-04
-- Uso:           Idempotente — seguro de ejecutar N veces.
--                Usa CREATE/ALTER ... IF NOT EXISTS para no
--                romper entornos que ya tienen datos.
-- ============================================================

-- ============================================================
-- 0. SCHEMA
-- ============================================================
CREATE SCHEMA IF NOT EXISTS proyecta_db;

-- Establecer el schema por defecto para el resto del script
SET search_path TO proyecta_db;

-- ============================================================
-- 1. TABLAS INDEPENDIENTES (sin FK entrante)
-- ============================================================

-- 1.1 usuario
CREATE TABLE IF NOT EXISTS usuario (
    usuario_id      SERIAL          PRIMARY KEY,
    nombre          VARCHAR(120)    NOT NULL,
    correo          VARCHAR(200)    NOT NULL UNIQUE,
    contrasena_hash TEXT,
    rol             VARCHAR(20)     CHECK (rol IN ('admin', 'director', 'gestor', 'operador')),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP       NOT NULL DEFAULT NOW(),
    ultimo_acceso   TIMESTAMP
);

COMMENT ON TABLE  usuario                   IS 'Usuarios del sistema con autenticación y control de roles.';
COMMENT ON COLUMN usuario.rol               IS 'Rol del usuario: admin, director, gestor, operador';
COMMENT ON COLUMN usuario.contrasena_hash   IS 'Hash bcrypt de la contraseña. Nunca texto plano.';

-- 1.2 patrocinador
CREATE TABLE IF NOT EXISTS patrocinador (
    patrocinador_id SERIAL          PRIMARY KEY,
    nombre          VARCHAR(120)    NOT NULL,
    cargo           VARCHAR(100),
    dependencia     VARCHAR(100),
    entidad         VARCHAR(150),
    proceso_sigc    VARCHAR(100),
    procedimiento   VARCHAR(150)
);

COMMENT ON TABLE patrocinador IS 'Patrocinadores institucionales de los proyectos.';

-- 1.3 system_parameters
CREATE TABLE IF NOT EXISTS system_parameters (
    param_key       VARCHAR(50)     PRIMARY KEY,
    param_value     VARCHAR(255)    NOT NULL,
    descripcion     VARCHAR(255)
);

COMMENT ON TABLE  system_parameters             IS 'Parámetros de configuración global del sistema.';
COMMENT ON COLUMN system_parameters.param_key   IS 'Clave única del parámetro (ej: ventana_vencimiento_dias).';

-- ============================================================
-- 2. TABLA PRINCIPAL — proyecto
-- ============================================================
CREATE TABLE IF NOT EXISTS proyecto (
    proyecto_id             VARCHAR(30)     PRIMARY KEY,   -- Formato: IS-PROY-CUN-NNN (PK manual)
    nombre                  VARCHAR(300)    NOT NULL,
    dependencia             VARCHAR(200),
    objetivo_general        TEXT,
    es_peti                 BOOLEAN         NOT NULL DEFAULT FALSE,
    estrategia_peti         VARCHAR(80),
    vigencia_peti           VARCHAR(20),
    fecha_inicio            DATE,
    fecha_cierre            DATE,
    plan_comunicaciones_pdf VARCHAR(300),
    estado                  VARCHAR(20)     NOT NULL DEFAULT 'activo'
                                CHECK (estado IN ('activo', 'cerrado')),
    cerrado                 BOOLEAN         NOT NULL DEFAULT FALSE,
    viabilizacion_pdf       VARCHAR(300),
    acta_constitucion_pdf   VARCHAR(300),
    cronograma_pdf          VARCHAR(300),
    avance_calculado        NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    fecha_registro          TIMESTAMP       NOT NULL DEFAULT NOW(),
    gestor_id               INTEGER         REFERENCES usuario(usuario_id)      ON DELETE SET NULL,
    director_id             INTEGER         REFERENCES usuario(usuario_id)      ON DELETE SET NULL,
    patrocinador_id         INTEGER         REFERENCES patrocinador(patrocinador_id) ON DELETE SET NULL
);

COMMENT ON TABLE  proyecto                      IS 'Proyectos de gestión tecnológica. PK manual con formato IS-PROY-CUN-NNN.';
COMMENT ON COLUMN proyecto.proyecto_id          IS 'Identificador único manual. Ejemplo: IS-PROY-CUN-001';
COMMENT ON COLUMN proyecto.cerrado              IS 'TRUE cuando el proyecto ha sido formalmente cerrado con Acta de Cierre.';
COMMENT ON COLUMN proyecto.avance_calculado     IS 'Avance ponderado calculado automáticamente a partir de los entregables.';
COMMENT ON COLUMN proyecto.estado               IS 'Estado operativo del proyecto: activo | cerrado';

-- ============================================================
-- 3. JERARQUÍA DE EJECUCIÓN — Fase → Hito → Entregable
-- ============================================================

-- 3.1 fase
CREATE TABLE IF NOT EXISTS fase (
    fase_id             SERIAL          PRIMARY KEY,
    numero              SMALLINT        NOT NULL,
    descripcion         VARCHAR(300),
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    avance_calculado    NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    proyecto_id         VARCHAR(30)     NOT NULL
                            REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);

COMMENT ON TABLE  fase                      IS 'Fases de ejecución de un proyecto. Contienen uno o más hitos.';
COMMENT ON COLUMN fase.ponderacion          IS 'Peso relativo de la fase en el avance total del proyecto.';
COMMENT ON COLUMN fase.avance_calculado     IS 'Avance ponderado de la fase, calculado desde los hitos.';

-- 3.2 hito
CREATE TABLE IF NOT EXISTS hito (
    hito_id             SERIAL          PRIMARY KEY,
    numero              SMALLINT        NOT NULL,
    descripcion         VARCHAR(300),
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    avance_calculado    NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    estado_revision     VARCHAR(30)     DEFAULT 'PENDIENTE'
                            CHECK (estado_revision IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fase_id             INTEGER         NOT NULL
                            REFERENCES fase(fase_id) ON DELETE CASCADE
);

COMMENT ON TABLE  hito                          IS 'Hitos de control dentro de una fase. Representan entregables clave.';
COMMENT ON COLUMN hito.ponderacion              IS 'Peso relativo del hito en el avance de su fase.';
COMMENT ON COLUMN hito.avance_calculado         IS 'Porcentaje de avance del hito calculado desde sus entregables.';
COMMENT ON COLUMN hito.estado_revision          IS 'Estado de revisión por el Gestor de Proyectos. Requerido APROBADO para cerrar el proyecto.';

-- 3.3 entregable
CREATE TABLE IF NOT EXISTS entregable (
    entregable_id       SERIAL          PRIMARY KEY,
    numero              SMALLINT        NOT NULL,
    nombre              VARCHAR(300)    NOT NULL,
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    conforme            BOOLEAN         NOT NULL DEFAULT FALSE,
    archivo_pdf         VARCHAR(300),
    fecha_entrega       DATE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    hito_id             INTEGER         NOT NULL
                            REFERENCES hito(hito_id) ON DELETE CASCADE
);

COMMENT ON TABLE  entregable                IS 'Entregables específicos que componen un hito.';
COMMENT ON COLUMN entregable.ponderacion    IS 'Peso del entregable para el cálculo de avance del hito.';
COMMENT ON COLUMN entregable.conforme       IS 'TRUE cuando el entregable ha sido validado y aceptado formalmente.';

-- ============================================================
-- 4. MÓDULO DE RIESGOS
-- ============================================================
CREATE TABLE IF NOT EXISTS riesgos (
    riesgo_id           SERIAL          PRIMARY KEY,
    codigo              VARCHAR(10),
    descripcion         TEXT            NOT NULL,
    probabilidad        INTEGER         NOT NULL CHECK (probabilidad BETWEEN 1 AND 5),
    impacto             INTEGER         NOT NULL CHECK (impacto BETWEEN 1 AND 5),
    nivel               VARCHAR(20),    -- Calculado: Crítico, Alto, Moderado, Bajo
    tratamiento         TEXT,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'Pendiente'
                            CHECK (estado IN ('Pendiente', 'Tratado')),
    fecha_actualizacion TIMESTAMP,
    proyecto_id         VARCHAR(30)     NOT NULL
                            REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);

COMMENT ON TABLE  riesgos               IS 'Registro de riesgos identificados para cada proyecto.';
COMMENT ON COLUMN riesgos.nivel         IS 'Calculado automáticamente: Crítico, Alto, Moderado, Bajo según probabilidad × impacto.';
COMMENT ON COLUMN riesgos.probabilidad  IS 'Escala 1-5. 1=Muy Baja, 5=Muy Alta.';
COMMENT ON COLUMN riesgos.impacto       IS 'Escala 1-5. 1=Muy Bajo, 5=Muy Alto.';

-- ============================================================
-- 5. OBJETIVOS ESPECÍFICOS
-- ============================================================
CREATE TABLE IF NOT EXISTS objetivos_especificos (
    obj_id      SERIAL      PRIMARY KEY,
    descripcion TEXT        NOT NULL,
    orden       SMALLINT,
    proyecto_id VARCHAR(30) REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);

COMMENT ON TABLE objetivos_especificos IS 'Objetivos específicos asociados a un proyecto.';

-- ============================================================
-- 6. CIERRE DE PROYECTO — actas_cierre
-- ============================================================
CREATE TABLE IF NOT EXISTS actas_cierre (
    acta_id             BIGSERIAL       PRIMARY KEY,
    proyecto_id         VARCHAR(30)     NOT NULL UNIQUE,
    resumen_ejecutivo   TEXT            NOT NULL,
    fecha_cierre        TIMESTAMP       NOT NULL,
    avance_final        NUMERIC(5, 2)   NOT NULL,

    CONSTRAINT fk_actas_cierre_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyecto(proyecto_id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE  actas_cierre                      IS 'Actas de cierre formal. Un proyecto solo puede tener un acta (UNIQUE en proyecto_id).';
COMMENT ON COLUMN actas_cierre.resumen_ejecutivo    IS 'Resumen ejecutivo del cierre. Mínimo 100 caracteres.';
COMMENT ON COLUMN actas_cierre.avance_final         IS 'Porcentaje de avance calculado como promedio ponderado de entregables conformes.';

-- ============================================================
-- 7. ÍNDICES DE RENDIMIENTO
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_proyecto_estado
    ON proyecto(estado);

CREATE INDEX IF NOT EXISTS idx_proyecto_cerrado
    ON proyecto(cerrado);

CREATE INDEX IF NOT EXISTS idx_proyecto_gestor
    ON proyecto(gestor_id);

CREATE INDEX IF NOT EXISTS idx_fase_proyecto
    ON fase(proyecto_id);

CREATE INDEX IF NOT EXISTS idx_hito_fase
    ON hito(fase_id);

CREATE INDEX IF NOT EXISTS idx_hito_estado_revision
    ON hito(estado_revision);

CREATE INDEX IF NOT EXISTS idx_entregable_hito
    ON entregable(hito_id);

CREATE INDEX IF NOT EXISTS idx_entregable_conforme
    ON entregable(conforme);

CREATE INDEX IF NOT EXISTS idx_riesgos_proyecto
    ON riesgos(proyecto_id);

CREATE INDEX IF NOT EXISTS idx_objetivos_proyecto
    ON objetivos_especificos(proyecto_id);

CREATE INDEX IF NOT EXISTS idx_actas_cierre_proyecto
    ON actas_cierre(proyecto_id);

-- ============================================================
-- 8. MIGRACIONES / ACTUALIZACIONES PARA ENTORNOS EXISTENTES
-- ============================================================
-- Si la tabla hito ya existía antes de implementar el Cierre de Proyecto,
-- nos aseguramos de agregar la columna faltante y asignarle un valor por defecto.
ALTER TABLE hito
    ADD COLUMN IF NOT EXISTS estado_revision VARCHAR(30) NULL;

UPDATE hito
SET estado_revision = 'PENDIENTE'
WHERE estado_revision IS NULL;

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
