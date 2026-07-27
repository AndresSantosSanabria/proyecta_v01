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
CREATE SCHEMA IF NOT EXISTS proyecta_db;;

-- Establecer el schema por defecto para el resto del script
SET search_path TO proyecta_db;;

-- ============================================================
-- 1. ESTRUCTURA DE TABLAS (ESTADO DESEADO)
-- ============================================================

-- 1.1 usuario
CREATE TABLE IF NOT EXISTS usuario (
    usuario_id      SERIAL          PRIMARY KEY,
    keycloak_sub    VARCHAR(120)    UNIQUE,
    nombre          VARCHAR(120)    NOT NULL,
    correo          VARCHAR(200)    NOT NULL UNIQUE,
    contrasena_hash TEXT,
    rol             VARCHAR(30),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP       NOT NULL DEFAULT NOW(),
    ultimo_acceso   TIMESTAMP
);;

-- 1.2 patrocinador
CREATE TABLE IF NOT EXISTS patrocinador (
    patrocinador_id SERIAL          PRIMARY KEY,
    nombre          VARCHAR(120)    NOT NULL,
    cargo           VARCHAR(100),
    dependencia     VARCHAR(100),
    entidad         VARCHAR(150),
    proceso_sigc    VARCHAR(100),
    procedimiento   VARCHAR(150)
);;

-- 1.25 rol_config
CREATE TABLE IF NOT EXISTS rol_config (
    rol_id          SERIAL          PRIMARY KEY,
    codigo          VARCHAR(30)     NOT NULL UNIQUE,
    nombre          VARCHAR(100)    NOT NULL,
    descripcion     VARCHAR(300),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE
);;

-- 1.26 estado_proyecto_config
CREATE TABLE IF NOT EXISTS estado_proyecto_config (
    estado_proyecto_id  SERIAL      PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    color               VARCHAR(20),
    es_terminal         BOOLEAN     NOT NULL DEFAULT FALSE,
    activo              BOOLEAN     NOT NULL DEFAULT TRUE
);;

-- 1.27 estado_entregable_config
CREATE TABLE IF NOT EXISTS estado_entregable_config (
    estado_entregable_id    SERIAL  PRIMARY KEY,
    codigo                  VARCHAR(30) NOT NULL UNIQUE,
    nombre                  VARCHAR(100) NOT NULL,
    descripcion             VARCHAR(300),
    color                   VARCHAR(20),
    es_conforme             BOOLEAN     NOT NULL DEFAULT FALSE,
    es_terminal             BOOLEAN     NOT NULL DEFAULT FALSE,
    activo                  BOOLEAN     NOT NULL DEFAULT TRUE
);;

-- 1.28 tipo_documento_config
CREATE TABLE IF NOT EXISTS tipo_documento_config (
    tipo_documento_id   SERIAL      PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    require_pdf         BOOLEAN     NOT NULL DEFAULT TRUE,
    orden               INTEGER     NOT NULL DEFAULT 0,
    activo              BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP   NOT NULL DEFAULT NOW()
);;

-- 1.29 estrategia_peti_config
CREATE TABLE IF NOT EXISTS estrategia_peti_config (
    estrategia_peti_id  SERIAL      PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    activo              BOOLEAN     NOT NULL DEFAULT TRUE
);;

-- 1.30 matriz_riesgo
CREATE TABLE IF NOT EXISTS matriz_riesgo (
    matriz_riesgo_id    SERIAL      PRIMARY KEY,
    probabilidad        VARCHAR(30) NOT NULL,
    impacto             VARCHAR(30) NOT NULL,
    nivel_riesgo        VARCHAR(30) NOT NULL,
    color               VARCHAR(20)  NOT NULL,
    puntaje             INTEGER     NOT NULL,
    UNIQUE(probabilidad, impacto)
);;

-- 1.31 estado_riesgo_config
CREATE TABLE IF NOT EXISTS estado_riesgo_config (
    estado_riesgo_id    SERIAL      PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    activo              BOOLEAN     NOT NULL DEFAULT TRUE
);;

-- 1.3 system_parameters
CREATE TABLE IF NOT EXISTS system_parameters (
    param_key       VARCHAR(50)     PRIMARY KEY,
    param_value     VARCHAR(1000)   NOT NULL,
    descripcion     VARCHAR(255)
);;

-- 1.4 reporte_config
CREATE TABLE IF NOT EXISTS reporte_config (
    id              VARCHAR(50)     PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL,
    descripcion     VARCHAR(300),
    orden           INTEGER         NOT NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE
);;

-- 2.1 proyecto
CREATE TABLE IF NOT EXISTS proyecto (
    proyecto_id             VARCHAR(40)     PRIMARY KEY,
    nombre                  VARCHAR(300)    NOT NULL,
    dependencia             VARCHAR(200),
    director_nombre         VARCHAR(120),
    director_correo          VARCHAR(200),
    objetivo_general        TEXT,
    es_peti                 BOOLEAN         NOT NULL DEFAULT FALSE,
    estrategia_peti         VARCHAR(80),
    estrategia_peti_config_id INTEGER       REFERENCES estrategia_peti_config(estrategia_peti_id),
    vigencia_peti           VARCHAR(20),
    fecha_inicio            DATE,
    fecha_cierre            DATE,
    tiene_plan_comunicaciones BOOLEAN       NOT NULL DEFAULT FALSE,
    plan_comunicaciones_pdf VARCHAR(300),
    acta_constitucion_pdf   VARCHAR(300),
    cronograma_pdf          VARCHAR(300),
    viabilizacion_pdf       VARCHAR(300),
    estado                  VARCHAR(30),
    estado_config_id        INTEGER         REFERENCES estado_proyecto_config(estado_proyecto_id),
    avance_total            NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    fecha_registro          TIMESTAMP       NOT NULL DEFAULT NOW(),
    patrocinador_id         INTEGER         REFERENCES patrocinador(patrocinador_id) ON DELETE SET NULL,
    
    furag_infraestructura_datos         VARCHAR(5),
    furag_interoperabilidad             VARCHAR(5),
    furag_digitalizacion_automatizacion VARCHAR(5),
    furag_contratacion_publica           VARCHAR(5),
    furag_servicios_nube                VARCHAR(5),
    furag_sandbox                       VARCHAR(5),
    furag_tecnologias_emergentes        VARCHAR(5)
);;

-- 3.1 fase
CREATE TABLE IF NOT EXISTS fase (
    fase_id             SERIAL          PRIMARY KEY,
    nombre              VARCHAR(150),
    descripcion         VARCHAR(300),
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    avance_calculado    NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    proyecto_id         VARCHAR(30)     NOT NULL
                            REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);;

-- 3.2 hito
CREATE TABLE IF NOT EXISTS hito (
    hito_id             SERIAL          PRIMARY KEY,
    nombre              VARCHAR(150),
    descripcion         VARCHAR(300),
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    avance_calculado    NUMERIC(5, 2)   NOT NULL DEFAULT 0.00,
    estado_revision     VARCHAR(30)     DEFAULT 'PENDIENTE'
                            CHECK (estado_revision IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fase_id             INTEGER         NOT NULL
                            REFERENCES fase(fase_id) ON DELETE CASCADE
);;

-- 3.3 entregable
CREATE TABLE IF NOT EXISTS entregable (
    entregable_id       SERIAL          PRIMARY KEY,
    nombre              VARCHAR(300)    NOT NULL,
    ponderacion         NUMERIC(5, 2)   NOT NULL,
    estado              VARCHAR(30),
    estado_config_id    INTEGER         REFERENCES estado_entregable_config(estado_entregable_id),
    conforme            BOOLEAN         NOT NULL DEFAULT FALSE,
    archivo_pdf         VARCHAR(300),
    observacion_revision VARCHAR(1000),
    fecha_inicio        DATE,
    fecha_limite        DATE,
    fecha_entrega_real  DATE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    hito_id             INTEGER         NOT NULL
                            REFERENCES hito(hito_id) ON DELETE CASCADE
);;

-- 4. riesgos
CREATE TABLE IF NOT EXISTS riesgos (
    riesgo_id           SERIAL          PRIMARY KEY,
    codigo              VARCHAR(10),
    descripcion         TEXT            NOT NULL,
    probabilidad        VARCHAR(30),
    impacto             VARCHAR(30),
    nivel               VARCHAR(30),
    puntaje             INTEGER,
    tratamiento         TEXT,
    estado              VARCHAR(30),
    estado_config_id    INTEGER         REFERENCES estado_riesgo_config(estado_riesgo_id),
    fecha_actualizacion TIMESTAMP,
    proyecto_id         VARCHAR(30)     NOT NULL
                            REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);;

-- 5. objetivos_especificos
CREATE TABLE IF NOT EXISTS objetivos_especificos (
    obj_id      SERIAL      PRIMARY KEY,
    descripcion TEXT        NOT NULL,
    orden       SMALLINT,
    proyecto_id VARCHAR(30) REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);;

-- 6. proyecto_equipo
CREATE TABLE IF NOT EXISTS proyecto_equipo (
    proyecto_id     VARCHAR(30)     NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    miembro_nombre  VARCHAR(120),
    miembro_rol     VARCHAR(100),
    miembro_cargo   VARCHAR(100)
);;

-- 7. actas_cierre
CREATE TABLE IF NOT EXISTS actas_cierre (
    acta_id             BIGSERIAL       PRIMARY KEY,
    proyecto_id         VARCHAR(30)     NOT NULL UNIQUE,
    resumen_ejecutivo   TEXT            NOT NULL,
    fecha_cierre        TIMESTAMPTZ     NOT NULL,
    avance_final        NUMERIC(5, 2)   NOT NULL,
    progreso_programado_final NUMERIC(5, 2),
    progreso_ejecutado_final NUMERIC(5, 2),
    diferencia_final    NUMERIC(5, 2),
    eficacia_final      NUMERIC(6, 4),
    estado_final        VARCHAR(30),
    corte_calculo       DATE,
    snapshot_json       TEXT,
    archivo_pdf         VARCHAR(255),
    ruta_archivo_pdf    VARCHAR(255),
    archivo_docx        VARCHAR(255),
    ruta_archivo_docx   VARCHAR(255),

    CONSTRAINT fk_actas_cierre_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyecto(proyecto_id)
        ON DELETE RESTRICT
);;

-- ============================================================
-- 2. MIGRACIONES (ASEGURAR COLUMNAS PARA ENTORNOS EXISTENTES)
-- ============================================================

-- Proyecto
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS director_nombre VARCHAR(120);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS director_correo VARCHAR(200);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS tiene_plan_comunicaciones BOOLEAN NOT NULL DEFAULT FALSE;;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS viabilizacion_pdf VARCHAR(300);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS acta_constitucion_pdf VARCHAR(300);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS cronograma_pdf VARCHAR(300);;

DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='proyecto' AND column_name='avance_calculado') THEN
        ALTER TABLE proyecto RENAME COLUMN avance_calculado TO avance_total;
    END IF;
END $$;;

ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_infraestructura_datos VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_interoperabilidad VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_digitalizacion_automatizacion VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_contratacion_publica VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_servicios_nube VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_sandbox VARCHAR(5);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS furag_tecnologias_emergentes VARCHAR(5);;

-- Fase
ALTER TABLE fase ADD COLUMN IF NOT EXISTS nombre VARCHAR(150);;

-- Hito
ALTER TABLE hito ADD COLUMN IF NOT EXISTS nombre VARCHAR(150);;
ALTER TABLE hito ADD COLUMN IF NOT EXISTS estado_revision VARCHAR(30) DEFAULT 'PENDIENTE';;

-- Entregable
ALTER TABLE entregable ADD COLUMN IF NOT EXISTS estado VARCHAR(30);;
ALTER TABLE entregable ADD COLUMN IF NOT EXISTS observacion_revision VARCHAR(1000);;
ALTER TABLE entregable ADD COLUMN IF NOT EXISTS fecha_inicio DATE;;
ALTER TABLE entregable ADD COLUMN IF NOT EXISTS fecha_entrega_real DATE;;
ALTER TABLE entregable ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES estado_entregable_config(estado_entregable_id);;

-- Riesgos
ALTER TABLE riesgos ADD COLUMN IF NOT EXISTS probabilidad VARCHAR(30);;
ALTER TABLE riesgos ADD COLUMN IF NOT EXISTS impacto VARCHAR(30);;
ALTER TABLE riesgos ADD COLUMN IF NOT EXISTS puntaje INTEGER;;
ALTER TABLE riesgos ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES estado_riesgo_config(estado_riesgo_id);;

-- Proyecto
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES estado_proyecto_config(estado_proyecto_id);;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS estrategia_peti_config_id INTEGER REFERENCES estrategia_peti_config(estrategia_peti_id);;

-- Usuario
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS keycloak_sub VARCHAR(120) UNIQUE;;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS rol_config_id INTEGER REFERENCES rol_config(rol_id);;

-- Documento
ALTER TABLE documento ADD COLUMN IF NOT EXISTS tipo_documento_config_id INTEGER REFERENCES tipo_documento_config(tipo_documento_id);;

DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='entregable' AND column_name='fecha_entrega') THEN
        ALTER TABLE entregable RENAME COLUMN fecha_entrega TO fecha_limite;
    END IF;
END $$;;

-- ============================================================
-- 3. COMENTARIOS Y METADATOS
-- ============================================================

COMMENT ON TABLE  usuario                   IS 'Usuarios del sistema con autenticación y control de roles.';;
COMMENT ON COLUMN usuario.rol               IS 'Rol del usuario: ADMINISTRADOR, DIRECTOR_PROYECTO, GESTOR_PROYECTOS_TI, OPERADOR';;

COMMENT ON TABLE patrocinador IS 'Patrocinadores institucionales de los proyectos.';;

COMMENT ON TABLE  system_parameters             IS 'Parámetros de configuración global del sistema.';;
COMMENT ON COLUMN system_parameters.param_key   IS 'Clave única del parámetro (ej: ventana_vencimiento_dias).';;

COMMENT ON TABLE reporte_config IS 'Configuración dinámica de los tipos de reportes disponibles en el sistema.';;

COMMENT ON TABLE  proyecto                      IS 'Proyectos de gestión tecnológica. PK manual con formato IS-PROY-CUN-YYYY-NNN.';;
COMMENT ON COLUMN proyecto.avance_total         IS 'Avance ponderado calculado automáticamente a partir de los entregables.';;
COMMENT ON COLUMN proyecto.estado               IS 'Estado operativo del proyecto: ACTIVO | CON_RETRASOS | CERRADO';;

COMMENT ON TABLE  fase                      IS 'Fases de ejecución de un proyecto. Contienen uno o más hitos.';;
COMMENT ON COLUMN fase.ponderacion          IS 'Peso relativo de la fase en el avance total del proyecto.';;
COMMENT ON COLUMN fase.avance_calculado     IS 'Avance ponderado de la fase, calculado desde los hitos.';;

COMMENT ON TABLE  hito                          IS 'Hitos de control dentro de una fase. Representan entregables clave.';;
COMMENT ON COLUMN hito.ponderacion              IS 'Peso relativo del hito en el avance de su fase.';;
COMMENT ON COLUMN hito.avance_calculado         IS 'Porcentaje de avance del hito calculado desde sus entregables.';;
COMMENT ON COLUMN hito.estado_revision          IS 'Estado de revisión por el Gestor de Proyectos. Requerido APROBADO para cerrar el proyecto.';;

COMMENT ON TABLE  entregable                IS 'Entregables específicos que componen un hito.';;
COMMENT ON COLUMN entregable.ponderacion    IS 'Peso del entregable para el cálculo de avance del hito.';;
COMMENT ON COLUMN entregable.conforme       IS 'TRUE cuando el entregable ha sido validado y aceptado formalmente.';;

COMMENT ON TABLE  riesgos               IS 'Registro de riesgos identificados para cada proyecto.';;
COMMENT ON COLUMN riesgos.nivel         IS 'Calculado automáticamente: Crítico, Alto, Moderado, Bajo según probabilidad × impacto.';;

COMMENT ON TABLE  actas_cierre                      IS 'Actas de cierre formal. Un proyecto solo puede tener un acta (UNIQUE en proyecto_id).';;

-- ============================================================
-- 4. ÍNDICES DE RENDIMIENTO
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_proyecto_estado ON proyecto(estado);;
CREATE INDEX IF NOT EXISTS idx_fase_proyecto   ON fase(proyecto_id);;
CREATE INDEX IF NOT EXISTS idx_hito_fase       ON hito(fase_id);;
CREATE INDEX IF NOT EXISTS idx_entregable_hito ON entregable(hito_id);;

-- ============================================================
-- 5. DATOS INICIALES DE CONFIGURACIÓN
-- ============================================================
INSERT INTO proyecta_db.reporte_config (id, nombre, descripcion, orden, activo) 
VALUES 
('ESTADO_PROYECTO', 'Estado de Proyecto', 'Resumen ejecutivo del avance y hitos principales.', 1, true),
('TODOS_LOS_PROYECTOS', 'Estado de todos los proyectos', 'Lista resumida de todos los proyectos con su avance.', 2, true),
('PROYECTOS_CON_RETRASOS', 'Proyectos con retrasos en entrega', 'Lista de proyectos que tienen entregables atrasados.', 3, true),
('PLAN_COMUNICACIONES', 'Plan de comunicaciones', 'Detalles del plan de comunicaciones de un proyecto.', 4, true),
('FURAG', 'Preguntas FURAG', 'Reporte de cumplimiento de metas y objetivos institucionales.', 5, true),
('RIESGOS', 'Verificación de tratamiento a riesgos', 'Visualización de amenazas y planes de mitigación.', 6, true)
ON CONFLICT (id) DO NOTHING;;
