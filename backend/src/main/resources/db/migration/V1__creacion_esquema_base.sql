-- =============================================================================
-- PROYECTA — Esquema Base Consolidado
-- Fecha: 2026-09-21
-- Base de datos: PostgreSQL 15+
-- Esquema: proyecta_db
--
-- CAMBIOS DE NORMALIZACIÓN:
-- - Eliminado `documento` (fusionado con `documento_proyecto_version`)
-- - Eliminado `riesgo_solucion_adjunto` y `riesgo_tratamiento_adjunto` → `riesgo_adjunto`
-- - Eliminado `entregable.archivo_pdf` y `entregable.conforme` (derivados de estado)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. TABLAS DE CONFIGURACIÓN / CATÁLOGOS
-- =============================================================================

CREATE TABLE estado_proyecto_config (
    estado_proyecto_id   SERIAL       PRIMARY KEY,
    codigo               VARCHAR(30)  NOT NULL UNIQUE,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          VARCHAR(300),
    color_hex            VARCHAR(7),
    es_terminal          BOOLEAN      NOT NULL DEFAULT FALSE,
    orden                INTEGER      NOT NULL DEFAULT 0,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE estado_entregable_config (
    estado_entregable_id SERIAL       PRIMARY KEY,
    codigo               VARCHAR(30)  NOT NULL UNIQUE,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          VARCHAR(300),
    es_conforme          BOOLEAN      NOT NULL DEFAULT FALSE,
    es_terminal          BOOLEAN      NOT NULL DEFAULT FALSE,
    cuenta_avance        NUMERIC(5,2) NOT NULL DEFAULT 0,
    color_hex            VARCHAR(7),
    orden                INTEGER      NOT NULL DEFAULT 0,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE tipo_documento_config (
    tipo_documento_id    SERIAL       PRIMARY KEY,
    codigo               VARCHAR(30)  NOT NULL UNIQUE,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          VARCHAR(300),
    require_pdf         BOOLEAN      NOT NULL DEFAULT TRUE,
    orden                INTEGER      NOT NULL DEFAULT 0,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE estrategia_peti_config (
    estrategia_peti_id   SERIAL       PRIMARY KEY,
    codigo               VARCHAR(80)  NOT NULL UNIQUE,
    nombre               VARCHAR(150) NOT NULL,
    descripcion          TEXT,
    vigencia_desde       VARCHAR(20),
    vigencia_hasta       VARCHAR(20),
    orden                INTEGER      NOT NULL DEFAULT 0,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE estado_riesgo_config (
    estado_riesgo_id     SERIAL       PRIMARY KEY,
    codigo               VARCHAR(30)  NOT NULL UNIQUE,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          VARCHAR(300),
    activo               BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE matriz_riesgo (
    matriz_riesgo_id     SERIAL       PRIMARY KEY,
    probabilidad         VARCHAR(30)  NOT NULL,
    impacto              VARCHAR(30)  NOT NULL,
    nivel_riesgo         VARCHAR(30)  NOT NULL,
    color                VARCHAR(20)  NOT NULL,
    puntaje              INTEGER      NOT NULL,
    UNIQUE (probabilidad, impacto)
);

CREATE TABLE lista_parametrica_config (
    id                   BIGSERIAL    PRIMARY KEY,
    lista_clave          VARCHAR(60)  NOT NULL,
    item_codigo          VARCHAR(200) NOT NULL,
    item_nombre          VARCHAR(200) NOT NULL,
    orden                INTEGER      NOT NULL DEFAULT 0,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    lista_nombre_campo   VARCHAR(200),
    lista_descripcion    VARCHAR(500),
    lista_tipo           VARCHAR(30)  DEFAULT 'Lista',
    UNIQUE (lista_clave, item_codigo)
);

CREATE TABLE system_parameters (
    param_key            VARCHAR(50)  PRIMARY KEY,
    param_value          VARCHAR(255) NOT NULL,
    descripcion          VARCHAR(255)
);

CREATE TABLE reporte_config (
    id                   VARCHAR(50)  PRIMARY KEY,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          VARCHAR(300),
    orden                INTEGER      NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- 2. TABLAS DE SEGURIDAD / ACCESO
-- =============================================================================

CREATE TABLE roles (
    id                   BIGSERIAL    PRIMARY KEY,
    codigo               VARCHAR(60)  NOT NULL UNIQUE,
    nombre               VARCHAR(120) NOT NULL,
    descripcion          VARCHAR(300),
    transversal          BOOLEAN      NOT NULL DEFAULT FALSE,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permisos (
    id                   BIGSERIAL    PRIMARY KEY,
    codigo               VARCHAR(120) NOT NULL UNIQUE,
    nombre               VARCHAR(180) NOT NULL,
    descripcion          VARCHAR(400),
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol_permiso (
    id                   BIGSERIAL    PRIMARY KEY,
    rol_id               BIGINT       NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id           BIGINT       NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (rol_id, permiso_id)
);

CREATE TABLE usuarios (
    id                   BIGSERIAL    PRIMARY KEY,
    keycloak_sub         VARCHAR(120) NOT NULL UNIQUE,
    username             VARCHAR(120) NOT NULL UNIQUE,
    nombre               VARCHAR(180) NOT NULL,
    correo               VARCHAR(200) NOT NULL UNIQUE,
    dependencia          VARCHAR(180),
    rol_codigo           VARCHAR(120),
    rol_nombre           VARCHAR(180),
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso        TIMESTAMP,
    recibir_notificaciones_globales BOOLEAN DEFAULT FALSE
);

CREATE TABLE usuario_proyecto (
    id                   BIGSERIAL    PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    proyecto_id          VARCHAR(40)  NOT NULL,
    cargo                VARCHAR(80)  NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_asignacion     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (usuario_id, proyecto_id, cargo)
);

CREATE TABLE usuario_permiso (
    id                   BIGSERIAL    PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    permiso_id           BIGINT       NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    concedido            BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_modificacion   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, permiso_id)
);

-- =============================================================================
-- 3. TABLAS DE PROYECTO (CORE)
-- =============================================================================

CREATE TABLE patrocinador (
    patrocinador_id      SERIAL       PRIMARY KEY,
    nombre               VARCHAR(120) NOT NULL,
    cargo                VARCHAR(100),
    dependencia          VARCHAR(100),
    entidad              VARCHAR(150),
    proceso_sigc         VARCHAR(100),
    procedimiento        VARCHAR(150)
);

CREATE TABLE proyecto (
    proyecto_id                   VARCHAR(40)  PRIMARY KEY,
    nombre                        VARCHAR(300) NOT NULL,
    dependencia                   VARCHAR(200),
    director_nombre               VARCHAR(120),
    director_correo               VARCHAR(200),
    director_usuario_id           BIGINT       REFERENCES usuarios(id) ON DELETE SET NULL,
    objetivo_general              TEXT,
    es_peti                       BOOLEAN      NOT NULL DEFAULT FALSE,
    estrategia_peti               VARCHAR(80),
    estrategia_peti_config_id     INTEGER      REFERENCES estrategia_peti_config(estrategia_peti_id),
    vigencia_peti                 VARCHAR(20),
    fecha_inicio                  DATE,
    fecha_cierre                  DATE,
    tiene_plan_comunicaciones     BOOLEAN      NOT NULL DEFAULT FALSE,
    cronograma_pdf                VARCHAR(255),
    acta_constitucion_pdf         VARCHAR(255),
    plan_comunicaciones_pdf       VARCHAR(255),
    viabilizacion_pdf             VARCHAR(255),
    viabilidad_estado             VARCHAR(20)  DEFAULT 'PENDIENTE',
    viabilidad_observaciones      TEXT,
    viabilidad_revisado_por       VARCHAR(120),
    viabilidad_revisado_en        TIMESTAMP,
    documentos_cargados           BOOLEAN      NOT NULL DEFAULT FALSE,
    documentos_verificados        BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_verificacion_documentos TIMESTAMP,
    fecha_limite_completar        DATE,
    cierre_forzoso                BOOLEAN      NOT NULL DEFAULT FALSE,
    cierre_forzoso_por            VARCHAR(120),
    cierre_forzoso_en             TIMESTAMP,
    acta_constitucion_cargada     BOOLEAN      NOT NULL DEFAULT FALSE,
    estado                        VARCHAR(20),
    estado_config_id              INTEGER      REFERENCES estado_proyecto_config(estado_proyecto_id),
    avance_total                  NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    cierre_solicitado             BOOLEAN      NOT NULL DEFAULT FALSE,
    cierre_solicitado_en          TIMESTAMP,
    cierre_solicitado_por         VARCHAR(120),
    cierre_estado                 VARCHAR(30),
    cierre_observaciones          TEXT,
    cierre_borrador_json          TEXT,
    completitud_borrador_json     TEXT,
    completitud_fases_completadas TEXT,
    furag_infraestructura_datos   VARCHAR(10),
    furag_interoperabilidad       VARCHAR(10),
    furag_digitalizacion_automatizacion VARCHAR(10),
    furag_contratacion_publica    VARCHAR(10),
    furag_servicios_nube          VARCHAR(10),
    furag_sandbox                 VARCHAR(10),
    furag_tecnologias_emergentes  VARCHAR(10),
    patrocinador_id               INTEGER      REFERENCES patrocinador(patrocinador_id) ON DELETE SET NULL,
    presupuesto_estimado          NUMERIC(24,2),
    alcance_detallado             TEXT,
    fecha_registro                TIMESTAMP    NOT NULL DEFAULT NOW(),
    requiere_completitud_director BOOLEAN      NOT NULL DEFAULT FALSE,
    primer_ingreso_director_at    TIMESTAMP,
     completado_por_director_at    TIMESTAMP,
     registrado_inicial_por        VARCHAR(120),
     email_message_id              VARCHAR(255)
);

-- FK diferida
ALTER TABLE usuario_proyecto
    ADD CONSTRAINT fk_usuario_proyecto_proyecto
    FOREIGN KEY (proyecto_id) REFERENCES proyecto(proyecto_id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uk_usuario_proyecto_director_activo
    ON usuario_proyecto (proyecto_id)
    WHERE UPPER(cargo) = 'DIRECTOR_PROYECTO' AND activo = TRUE;

-- 3.1 Fases del proyecto
CREATE TABLE fase (
    fase_id              SERIAL       PRIMARY KEY,
    nombre               VARCHAR(150),
    descripcion          VARCHAR(300),
    ponderacion          NUMERIC(5,2) NOT NULL,
    avance_calculado     NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    proyecto_id          VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);

-- 3.6 Hitos por fase
CREATE TABLE hito (
    hito_id              SERIAL       PRIMARY KEY,
    nombre               VARCHAR(150),
    descripcion          VARCHAR(300),
    ponderacion          NUMERIC(5,2) NOT NULL,
    avance_calculado     NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    estado_revision      VARCHAR(30)  CHECK (estado_revision IN ('PENDIENTE','APROBADO','RECHAZADO')),
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    fase_id              INTEGER      NOT NULL REFERENCES fase(fase_id) ON DELETE CASCADE
);

-- 3.7 Entregables por hito
CREATE TABLE entregable (
    entregable_id        SERIAL       PRIMARY KEY,
    nombre               VARCHAR(300) NOT NULL,
    descripcion          VARCHAR(300),
    ponderacion          NUMERIC(5,2) NOT NULL,
    estado               VARCHAR(20),
    estado_config_id     INTEGER      REFERENCES estado_entregable_config(estado_entregable_id),
    conforme             BOOLEAN      NOT NULL DEFAULT FALSE,
    archivo_pdf          VARCHAR(300),
    fecha_limite         DATE,
    fecha_entrega_real   DATE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    hito_id              INTEGER      NOT NULL REFERENCES hito(hito_id) ON DELETE CASCADE,
    observacion_revision VARCHAR(1000),
    fecha_inicio         DATE,
    retroactivo          BOOLEAN      NOT NULL DEFAULT FALSE
);

-- 3.8 Objetivos específicos del proyecto
CREATE TABLE objetivos_especificos (
    obj_id               SERIAL       PRIMARY KEY,
    descripcion          TEXT         NOT NULL,
    orden                SMALLINT,
    proyecto_id          VARCHAR(40)  REFERENCES proyecto(proyecto_id) ON DELETE CASCADE
);

-- 3.9 Equipo del proyecto
CREATE TABLE proyecto_equipo (
    id                   BIGSERIAL    PRIMARY KEY,
    proyecto_id          VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    miembro_nombre       VARCHAR(120),
    miembro_rol          VARCHAR(100),
    miembro_cargo        VARCHAR(100),
    miembro_dependencia  VARCHAR(150),
    miembro_telefono     VARCHAR(30),
    miembro_correo       VARCHAR(150)
);

-- 3.10 Stakeholders del proyecto
CREATE TABLE proyecto_stakeholder (
    id                   BIGSERIAL    PRIMARY KEY,
    proyecto_id          VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    stakeholder_rol      VARCHAR(150),
    stakeholder_descripcion TEXT,
    stakeholder_interes  TEXT,
    stakeholder_impacto  TEXT
);

-- =============================================================================
-- 4. GESTIÓN DE RIESGOS
-- =============================================================================

CREATE TABLE riesgos (
    riesgo_id                SERIAL       PRIMARY KEY,
    codigo                   VARCHAR(10),
    descripcion              TEXT         NOT NULL,
    probabilidad             VARCHAR(10)  CHECK (probabilidad IN ('UNO','DOS','TRES','CUATRO','CINCO')),
    impacto                  VARCHAR(10)  CHECK (impacto IN ('UNO','DOS','TRES','CUATRO','CINCO')),
    nivel                    VARCHAR(30),
    puntaje                  INTEGER,
    tratamiento              TEXT,
    estado                   VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    estado_config_id         INTEGER      REFERENCES estado_riesgo_config(estado_riesgo_id),
    fecha_actualizacion      TIMESTAMP,
    proyecto_id              VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    categoria_riesgo         VARCHAR(120),
    causa                    TEXT,
    consecuencia             TEXT,
    controles_existentes     TEXT,
    tipo_control             VARCHAR(80),
    valoracion_control       VARCHAR(80),
    probabilidad_residual    VARCHAR(10)  CHECK (probabilidad_residual IN ('UNO','DOS','TRES','CUATRO','CINCO')),
    impacto_residual         VARCHAR(10)  CHECK (impacto_residual IN ('UNO','DOS','TRES','CUATRO','CINCO')),
    nivel_residual           VARCHAR(20),
    acciones_mitigacion      TEXT,
    entidad_responsable      VARCHAR(150),
    rol_responsable          VARCHAR(150),
    fecha_accion             DATE,
    evidencia_indicador      TEXT,
    tipo_riesgo              VARCHAR(20)  NOT NULL DEFAULT 'GENERAL',
    created_by               VARCHAR(150)
);

-- 4.1 Archivos adjuntos de soluciones de riesgo
CREATE TABLE riesgo_solucion_adjunto (
    riesgo_solucion_adjunto_id BIGSERIAL  PRIMARY KEY,
    riesgo_id               INTEGER      NOT NULL REFERENCES riesgos(riesgo_id) ON DELETE CASCADE,
    nombre_original         VARCHAR(300) NOT NULL,
    nombre_almacenado       VARCHAR(300) NOT NULL,
    ruta_almacenamiento     VARCHAR(120) NOT NULL,
    mime_type               VARCHAR(120) NOT NULL,
    tamano_bytes            BIGINT       NOT NULL,
    fecha_carga             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4.2 Iteraciones de tratamiento de riesgo
CREATE TABLE riesgo_tratamiento (
    riesgo_tratamiento_id  BIGSERIAL    PRIMARY KEY,
    riesgo_id              INTEGER      NOT NULL REFERENCES riesgos(riesgo_id) ON DELETE CASCADE,
    iteracion              INTEGER      NOT NULL,
    comentario             TEXT         NOT NULL,
    fecha_creacion         TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (riesgo_id, iteracion)
);

-- 4.3 Archivos adjuntos de tratamientos de riesgo
CREATE TABLE riesgo_tratamiento_adjunto (
    riesgo_tratamiento_adjunto_id BIGSERIAL PRIMARY KEY,
    riesgo_tratamiento_id   BIGINT       NOT NULL REFERENCES riesgo_tratamiento(riesgo_tratamiento_id) ON DELETE CASCADE,
    nombre_original         VARCHAR(300) NOT NULL,
    nombre_almacenado       VARCHAR(300) NOT NULL,
    ruta_almacenamiento     VARCHAR(120) NOT NULL,
    mime_type               VARCHAR(120) NOT NULL,
    tamano_bytes            BIGINT       NOT NULL,
    fecha_carga             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 5. GESTIÓN DE DOCUMENTOS (UNIFICADA)
-- =============================================================================

-- 5.1 Versionado de documentos de proyecto
CREATE TABLE documento_proyecto_version (
    documento_proyecto_version_id BIGSERIAL PRIMARY KEY,
    proyecto_id              VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    tipo_documento           VARCHAR(30)  NOT NULL,
    numero_version           INTEGER      NOT NULL,
    nombre_archivo_original  VARCHAR(255) NOT NULL,
    nombre_almacenado        VARCHAR(255) NOT NULL,
    ruta_almacenamiento      VARCHAR(500) NOT NULL,
    mime_type                VARCHAR(100) NOT NULL,
    tamano_bytes             BIGINT       NOT NULL,
    observacion              VARCHAR(1000),
    estado                   VARCHAR(30)  NOT NULL CHECK (estado IN ('ACTUAL','HISTORICA','REVERTIDA')),
    subido_por               VARCHAR(200),
    subido_rol               VARCHAR(80),
    subido_en                TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (proyecto_id, tipo_documento, numero_version)
);

-- 5.2 Documento versionado por entregable (entidad DocumentoVersion.java)
CREATE TABLE documento_version (
    documento_version_id     BIGSERIAL    PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    numero_version           INTEGER      NOT NULL,
    nombre_archivo_original  VARCHAR(255) NOT NULL,
    archivo_storage          VARCHAR(300) NOT NULL,
    mime_type                VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    size_bytes               BIGINT,
    checksum_sha256          VARCHAR(64),
    fecha_entrega            DATE,
    comentario_carga         VARCHAR(1000),
    estado                   VARCHAR(30)  NOT NULL CHECK (estado IN ('ACTUAL','HISTORICA','REVERTIDA')),
    subido_por               VARCHAR(200),
    subido_rol               VARCHAR(80),
    subido_en                TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (entregable_id, numero_version)
);

-- 5.3 Observaciones sobre documentos
CREATE TABLE documento_observacion (
    documento_observacion_id BIGSERIAL   PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    documento_version_id     BIGINT       REFERENCES documento_version(documento_version_id) ON DELETE SET NULL,
    observacion              TEXT        NOT NULL,
    estado                   VARCHAR(30) NOT NULL CHECK (estado IN ('ABIERTA','SUBSANADA','CERRADA')),
    creada_por               VARCHAR(200),
    creada_rol               VARCHAR(80),
    creada_en                TIMESTAMP   NOT NULL DEFAULT NOW(),
    subsanada_por            VARCHAR(200),
    subsanada_en             TIMESTAMP,
    comentario_subsanacion   VARCHAR(1000),
    cerrada_por              VARCHAR(200),
    cerrada_en               TIMESTAMP
);

-- 5.3 Auditoría de operaciones sobre documentos
CREATE TABLE documento_auditoria (
    documento_auditoria_id   BIGSERIAL   PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    documento_version_id     BIGINT       REFERENCES documento_version(documento_version_id) ON DELETE SET NULL,
    documento_observacion_id BIGINT      REFERENCES documento_observacion(documento_observacion_id) ON DELETE SET NULL,
    accion                   VARCHAR(50) NOT NULL,
    actor                    VARCHAR(200),
    actor_rol                VARCHAR(80),
    fecha                    TIMESTAMP   NOT NULL DEFAULT NOW(),
    metadata                 TEXT
);

-- 5.4 Acceso público a evidencias (tokens)
CREATE TABLE public_evidence_access (
    id                       BIGSERIAL    PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    token                    VARCHAR(64)  NOT NULL UNIQUE,
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(200)
);

-- 5.5 Documento legacy (compatibilidad con entidad Documento.java)
CREATE TABLE documento (
    id                       BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(30)  NOT NULL,
    tipo_documento           VARCHAR(30)  NOT NULL,
    tipo_documento_config_id INTEGER      REFERENCES tipo_documento_config(tipo_documento_id),
    nombre_original          VARCHAR(255) NOT NULL,
    nombre_almacenado        VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento      VARCHAR(500) NOT NULL,
    url_descarga             VARCHAR(500),
    mime_type                VARCHAR(100) NOT NULL,
    tamano_bytes             BIGINT       NOT NULL,
    fecha_carga              TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion      TIMESTAMP
);

-- 5.6 Documento dinámico (entidad DocumentoDinamico.java)
CREATE TABLE documento_dinamico (
    id                       BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(30)  NOT NULL,
    tipo_documento           VARCHAR(100) NOT NULL,
    nombre_original          VARCHAR(255) NOT NULL,
    nombre_almacenado        VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento      VARCHAR(500) NOT NULL,
    url_descarga             VARCHAR(500),
    mime_type                VARCHAR(100) NOT NULL,
    tamano_bytes             BIGINT       NOT NULL,
    fecha_carga              TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion      TIMESTAMP
);

CREATE INDEX idx_proyecto_tipo ON documento_dinamico(proyecto_id, tipo_documento);

-- 5.9 Revisión individual de documentos pre-wizard
CREATE TABLE documento_pre_wizard_revision (
    documento_pre_wizard_revision_id BIGSERIAL   PRIMARY KEY,
    proyecto_id                      VARCHAR(30) NOT NULL,
    tipo_documento                   VARCHAR(30) NOT NULL,
    estado                           VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    observacion                      VARCHAR(1000),
    revisado_por                     VARCHAR(200),
    revisado_en                      TIMESTAMP,
    creado_en                        TIMESTAMP    NOT NULL DEFAULT NOW(),
    actualizado_en                   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pre_wizard_revision UNIQUE (proyecto_id, tipo_documento),
    CONSTRAINT chk_pre_wizard_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'DEVUELTO')),
    CONSTRAINT chk_pre_wizard_tipo CHECK (tipo_documento IN (
        'VIABILIZACION',
        'PLAN_COMUNICACIONES',
        'MATRIZ_RIESGOS_VIABILIDAD'
    ))
);

CREATE INDEX idx_pre_wizard_revision_proyecto
    ON documento_pre_wizard_revision (proyecto_id);

-- 5.7 Log de notificaciones de avance
CREATE TABLE notification_log (
    id                       BIGSERIAL    PRIMARY KEY,
    project_id               VARCHAR(40)  NOT NULL,
    notification_date        DATE         NOT NULL,
    notification_type        VARCHAR(50)  NOT NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, notification_date, notification_type)
);

-- 5.8 Log de envío de emails
CREATE TABLE notification_mail_dispatch_log (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient                VARCHAR(200) NOT NULL,
    subject                  VARCHAR(500) NOT NULL,
    status                   VARCHAR(50)  NOT NULL,
    detail                   TEXT,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 6. SISTEMA DE NOTIFICACIONES
-- =============================================================================

CREATE TABLE notification_event_catalog (
    code                     VARCHAR(120) PRIMARY KEY,
    name                     VARCHAR(200) NOT NULL,
    description              TEXT,
    category                 VARCHAR(40)  NOT NULL,
    default_enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_project_context BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_template (
    event_code               VARCHAR(120) PRIMARY KEY REFERENCES notification_event_catalog(code),
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    html_enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    severity                 VARCHAR(30)  DEFAULT 'INFO',
    scope                    VARCHAR(30)  DEFAULT 'GLOBAL',
    subject_template         VARCHAR(500) NOT NULL,
    body_template            TEXT         NOT NULL,
    target_roles             JSONB,
    updated_by               VARCHAR(120),
    updated_at               TIMESTAMP
);

CREATE TABLE notification_preference (
    id                       BIGSERIAL    PRIMARY KEY,
    user_id                  BIGINT       NOT NULL REFERENCES usuarios(id),
    event_code               VARCHAR(120) NOT NULL,
    project_id               VARCHAR(40),
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    email_enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP,
    UNIQUE (user_id, event_code, project_id)
);

CREATE TABLE notification_in_app (
    id                       BIGSERIAL    PRIMARY KEY,
    recipient_user_id        BIGINT       NOT NULL REFERENCES usuarios(id),
    title                    VARCHAR(200) NOT NULL,
    message                  TEXT         NOT NULL,
    event_code               VARCHAR(120) NOT NULL,
    read_status              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at                  TIMESTAMP,
    source_entity_id         VARCHAR(80),
    severity                 VARCHAR(30),
    target_url               TEXT
);

CREATE TABLE notification_audit (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_code               VARCHAR(255) NOT NULL,
    recipient                VARCHAR(255) NOT NULL,
    channel                  VARCHAR(50)  NOT NULL,
    status                   VARCHAR(50)  NOT NULL,
    failure_reason           TEXT,
    project_id               VARCHAR(40),
    notification_type        VARCHAR(50),
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 7. CIERRE DE PROYECTO
-- =============================================================================

CREATE TABLE project_closure_template (
    id                       BIGSERIAL    PRIMARY KEY,
    codigo_proceso           VARCHAR(30)  NOT NULL DEFAULT 'A-GT-FR-004',
    version_num              INTEGER      NOT NULL DEFAULT 1,
    nombre_documento         VARCHAR(200) NOT NULL DEFAULT 'Acta de Cierre del Proyecto',
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    template_json            JSONB        NOT NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80)
);

CREATE UNIQUE INDEX uk_closure_template_active
    ON project_closure_template (activo) WHERE activo = TRUE;

CREATE TABLE project_closures (
    id                       BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(40)  NOT NULL UNIQUE REFERENCES proyecto(proyecto_id),
    template_id              BIGINT       NOT NULL REFERENCES project_closure_template(id),
    template_snapshot        JSONB        NOT NULL,
    form_data                JSONB        NOT NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(80)
);

CREATE TABLE project_closure_question (
    id                       BIGSERIAL    PRIMARY KEY,
    texto                    VARCHAR(500) NOT NULL,
    tipo_respuesta           VARCHAR(30)  NOT NULL DEFAULT 'texto_libre',
    opciones                 JSONB,
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    orden                    INTEGER      NOT NULL DEFAULT 0,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80)
);

CREATE TABLE project_closure_answer (
    id                       BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(40)  NOT NULL,
    question_id              BIGINT       NOT NULL REFERENCES project_closure_question(id),
    respuesta                TEXT,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (proyecto_id, question_id)
);

CREATE TABLE actas_cierre (
    acta_id                  BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(40)  NOT NULL UNIQUE REFERENCES proyecto(proyecto_id),
    resumen_ejecutivo        TEXT         NOT NULL,
    fecha_cierre             TIMESTAMP    NOT NULL,
    avance_final             NUMERIC(5,2) NOT NULL,
    progreso_programado_final NUMERIC(5,2),
    progreso_ejecutado_final NUMERIC(5,2),
    diferencia_final         NUMERIC(5,2),
    eficacia_final           NUMERIC(6,4),
    estado_final             VARCHAR(30),
    corte_calculo            DATE,
    snapshot_json            TEXT,
    archivo_pdf              VARCHAR(255),
    ruta_archivo_pdf         VARCHAR(255),
    archivo_docx             VARCHAR(255),
    ruta_archivo_docx        VARCHAR(255)
);

-- =============================================================================
-- 8. WORKFLOW / CAMBIOS
-- =============================================================================

CREATE TABLE entregable_cambio_fecha (
    id                       BIGSERIAL    PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id),
    fecha_anterior           DATE         NOT NULL,
    fecha_nueva              DATE         NOT NULL,
    justificacion            TEXT         NOT NULL,
    archivo_pdf              VARCHAR(300) NOT NULL,
    nombre_original          VARCHAR(300),
    usuario                  VARCHAR(255) NOT NULL,
    usuario_rol              VARCHAR(500),
    creado_en                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE entregable_cambio_descripcion (
    id                       BIGSERIAL    PRIMARY KEY,
    entregable_id            INTEGER      NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    descripcion_anterior     TEXT,
    descripcion_nueva        TEXT         NOT NULL,
    justificacion            TEXT         NOT NULL,
    archivo_pdf              TEXT         NOT NULL,
    nombre_original          VARCHAR(300),
    usuario                  VARCHAR(255) NOT NULL,
    usuario_rol              VARCHAR(500),
    creado_en                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proyecto_beneficio_impacto (
    id                                BIGSERIAL PRIMARY KEY,
    proyecto_id                       VARCHAR(40) NOT NULL UNIQUE REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    estado                            VARCHAR(30) NOT NULL,
    requerido_en                      TIMESTAMP,
    requerido_por                     VARCHAR(120),
    diligenciado_en                   TIMESTAMP,
    diligenciado_por                  VARCHAR(120),
    revisado_en                       TIMESTAMP,
    revisado_por                      VARCHAR(120),
    poblacion_beneficiada_directa     INTEGER,
    poblacion_beneficiada_indirecta   INTEGER,
    poblacion_objetivo                INTEGER,
    territorio_beneficiado            VARCHAR(200),
    beneficio_principal               TEXT,
    impacto_social                    TEXT,
    impacto_institucional             TEXT,
    impacto_economico                 TEXT,
    alineacion_plan_desarrollo        TEXT,
    alineacion_peti                   TEXT,
    metas_contribuidas                TEXT,
    indicador_base                    TEXT,
    indicador_meta                    TEXT,
    indicador_resultado               TEXT,
    fuente_verificacion               TEXT,
    observaciones                     TEXT,
    snapshot_json                     JSONB,
    created_at                        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE respuestas_furag (
    respuesta_furag_id       BIGSERIAL    PRIMARY KEY,
    proyecto_id              VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    codigo_pregunta          VARCHAR(80)  NOT NULL,
    pregunta                 TEXT         NOT NULL,
    respuesta                VARCHAR(5),
    obligatoria              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_actualizacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (proyecto_id, codigo_pregunta)
);

-- =============================================================================
-- 9. AUDITORÍA Y ANALÍTICA
-- =============================================================================

CREATE TABLE system_audit_log (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id               VARCHAR(100),
    usuario_nombre           VARCHAR(255),
    usuario_rol              VARCHAR(100),
    accion                   VARCHAR(50)  NOT NULL,
    modulo                   VARCHAR(150) NOT NULL,
    metodo_http              VARCHAR(10),
    recurso                  VARCHAR(255),
    codigo_estado            INTEGER      NOT NULL,
    estado                   VARCHAR(20)  NOT NULL,
    detalle                  TEXT,
    traza_error              TEXT,
    ip_origen                VARCHAR(45),
    user_agent               TEXT,
    duracion_ms              BIGINT,
    eliminado                BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_eliminacion        TIMESTAMP,
    fecha_creacion           TIMESTAMP    NOT NULL DEFAULT NOW(),
    entidad_tipo             VARCHAR(100),
    entidad_id               VARCHAR(100),
    respuesta_body           TEXT,
    request_body             TEXT
);

CREATE TABLE auditoria_ponderaciones (
    auditoria_id             SERIAL       PRIMARY KEY,
    proyecto_id              VARCHAR(40)  REFERENCES proyecto(proyecto_id),
    fase_id                  INTEGER      REFERENCES fase(fase_id),
    ponderacion_anterior     NUMERIC(5,2),
    ponderacion_nueva        NUMERIC(5,2),
    razon_cambio             VARCHAR(255),
    usuario_id               VARCHAR(50),
    fecha_cambio             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analitica_portafolio_snapshot (
    snapshot_id              BIGSERIAL    PRIMARY KEY,
    corte_calculo            TIMESTAMPTZ  NOT NULL UNIQUE,
    fuente                   VARCHAR(80)  NOT NULL DEFAULT 'PORTAFOLIO',
    snapshot_json            JSONB        NOT NULL,
    fecha_generacion         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE advance_report_uploads (
    id                       BIGSERIAL    PRIMARY KEY,
    project_id               VARCHAR(40)  NOT NULL REFERENCES proyecto(proyecto_id),
    periodo                  VARCHAR(20)  NOT NULL,
    file_name                VARCHAR(255) NOT NULL,
    file_path                VARCHAR(500) NOT NULL,
    file_size                BIGINT,
    uploaded_by              VARCHAR(120) NOT NULL,
    uploaded_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    estado                   VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    observaciones            VARCHAR(1000),
    UNIQUE (project_id, periodo)
);

-- =============================================================================
-- 10. ÍNDICES DE RENDIMIENTO
-- =============================================================================

CREATE INDEX idx_proyecto_director_usuario ON proyecto(director_usuario_id);
CREATE INDEX idx_proyecto_estado ON proyecto(estado_config_id);
CREATE INDEX idx_fase_proyecto ON fase(proyecto_id);
CREATE INDEX idx_hito_fase ON hito(fase_id);
CREATE INDEX idx_entregable_hito ON entregable(hito_id);
CREATE INDEX idx_entregable_estado ON entregable(estado_config_id);
CREATE INDEX idx_riesgos_proyecto ON riesgos(proyecto_id);
CREATE INDEX idx_riesgo_solucion_riesgo ON riesgo_solucion_adjunto(riesgo_id);
CREATE INDEX idx_riesgo_tratamiento_riesgo ON riesgo_tratamiento(riesgo_id);
CREATE INDEX idx_riesgo_tratamiento_adjunto ON riesgo_tratamiento_adjunto(riesgo_tratamiento_id);
CREATE INDEX idx_documento_version_proyecto ON documento_proyecto_version(proyecto_id);
CREATE INDEX idx_documento_version_tipo ON documento_proyecto_version(tipo_documento);
CREATE INDEX idx_documento_version_estado ON documento_proyecto_version(estado);
CREATE INDEX idx_documento_observacion_entregable ON documento_observacion(entregable_id);
CREATE INDEX idx_documento_observacion_version ON documento_observacion(documento_version_id);
CREATE INDEX idx_documento_observacion_estado ON documento_observacion(estado);
CREATE INDEX idx_documento_auditoria_entregable ON documento_auditoria(entregable_id);
CREATE INDEX idx_documento_auditoria_version ON documento_auditoria(documento_version_id);
CREATE INDEX idx_documento_auditoria_accion ON documento_auditoria(accion);
CREATE INDEX idx_usuarios_username ON usuarios(username);
CREATE INDEX idx_usuarios_correo ON usuarios(correo);
CREATE INDEX idx_roles_codigo ON roles(codigo);
CREATE INDEX idx_permisos_codigo ON permisos(codigo);
CREATE INDEX idx_rol_permiso_rol ON rol_permiso(rol_id);
CREATE INDEX idx_rol_permiso_permiso ON rol_permiso(permiso_id);
CREATE INDEX idx_usuario_proyecto_usuario ON usuario_proyecto(usuario_id);
CREATE INDEX idx_usuario_proyecto_proyecto ON usuario_proyecto(proyecto_id);
CREATE INDEX idx_usuario_permiso_usuario ON usuario_permiso(usuario_id);
CREATE INDEX idx_usuario_permiso_permiso ON usuario_permiso(permiso_id);
CREATE INDEX idx_notification_in_app_recipient ON notification_in_app(recipient_user_id, created_at DESC);
CREATE INDEX idx_notification_in_app_unread ON notification_in_app(recipient_user_id, read_status);
CREATE INDEX idx_notification_preference_user ON notification_preference(user_id);
CREATE INDEX idx_notification_audit_recipient ON notification_audit(recipient);
CREATE INDEX idx_notification_audit_event ON notification_audit(event_code);
CREATE INDEX idx_notification_audit_status ON notification_audit(status);
CREATE INDEX idx_notification_audit_created ON notification_audit(created_at);
CREATE INDEX idx_audit_log_fecha ON system_audit_log(fecha_creacion DESC);
CREATE INDEX idx_audit_log_usuario ON system_audit_log(usuario_id);
CREATE INDEX idx_audit_log_accion ON system_audit_log(accion);
CREATE INDEX idx_audit_log_modulo ON system_audit_log(modulo);
CREATE INDEX idx_audit_log_entidad ON system_audit_log(entidad_tipo, entidad_id);
CREATE INDEX idx_auditoria_ponderaciones_proyecto ON auditoria_ponderaciones(proyecto_id);
CREATE INDEX idx_lista_parametrica_clave ON lista_parametrica_config(lista_clave);
CREATE INDEX idx_respuestas_furag_proyecto ON respuestas_furag(proyecto_id);
CREATE INDEX idx_matriz_riesgo_nivel ON matriz_riesgo(nivel_riesgo);
CREATE INDEX idx_public_evidence_token ON public_evidence_access(token);
CREATE INDEX idx_public_evidence_entregable ON public_evidence_access(entregable_id);
CREATE INDEX idx_cambio_fecha_entregable ON entregable_cambio_fecha(entregable_id);
CREATE INDEX idx_cambio_descripcion_entregable ON entregable_cambio_descripcion(entregable_id);
CREATE INDEX idx_analitica_snapshot_fecha ON analitica_portafolio_snapshot(fecha_generacion DESC);
CREATE INDEX idx_advance_report_project ON advance_report_uploads(project_id);

-- =============================================================================
-- 11. FUNCIONES Y TRIGGERS
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validar_ponderacion_fase()
RETURNS TRIGGER AS $$
DECLARE
    v_suma NUMERIC(5,2);
BEGIN
    IF NEW.ponderacion < 0.01 OR NEW.ponderacion > 100 THEN
        RAISE EXCEPTION 'La ponderación debe estar entre 0.01 y 100. Valor: %', NEW.ponderacion;
    END IF;
    SELECT COALESCE(SUM(ponderacion), 0) INTO v_suma
    FROM fase
    WHERE proyecto_id = NEW.proyecto_id AND fase_id IS DISTINCT FROM NEW.fase_id;
    v_suma := v_suma + NEW.ponderacion;
    IF v_suma > 100 THEN
        RAISE WARNING 'La suma de ponderaciones del proyecto % excede 100: %', NEW.proyecto_id, v_suma;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_validar_ponderacion_fase_insert
    BEFORE INSERT ON fase
    FOR EACH ROW EXECUTE FUNCTION fn_validar_ponderacion_fase();

CREATE TRIGGER tr_validar_ponderacion_fase_update
    BEFORE UPDATE ON fase
    FOR EACH ROW WHEN (OLD.ponderacion IS DISTINCT FROM NEW.ponderacion)
    EXECUTE FUNCTION fn_validar_ponderacion_fase();

CREATE OR REPLACE FUNCTION fn_auditar_cambios_ponderacion()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.ponderacion IS DISTINCT FROM NEW.ponderacion THEN
        INSERT INTO auditoria_ponderaciones
            (proyecto_id, fase_id, ponderacion_anterior, ponderacion_nueva, usuario_id, fecha_cambio)
        VALUES
            (NEW.proyecto_id, NEW.fase_id, OLD.ponderacion, NEW.ponderacion, current_setting('app.current_user_id', TRUE), NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_auditar_cambios_ponderacion
    AFTER UPDATE ON fase
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambios_ponderacion();

CREATE OR REPLACE FUNCTION calc_suma_ponderaciones_fases(p_proyecto_id VARCHAR)
RETURNS NUMERIC AS $$
    SELECT COALESCE(SUM(ponderacion), 0) FROM fase WHERE proyecto_id = p_proyecto_id;
$$ LANGUAGE sql;

CREATE OR REPLACE FUNCTION normalizar_ponderaciones_fases(p_proyecto_id VARCHAR)
RETURNS VOID AS $$
DECLARE
    v_total NUMERIC(5,2);
    v_factor NUMERIC(5,4);
BEGIN
    v_total := calc_suma_ponderaciones_fases(p_proyecto_id);
    IF v_total > 0 AND v_total != 100 THEN
        v_factor := 100.0 / v_total;
        UPDATE fase SET ponderacion = ROUND(ponderacion * v_factor, 2) WHERE proyecto_id = p_proyecto_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 5.10 Documentación interna (solo gestores/admin)
CREATE TABLE documento_interno (
    id                       BIGSERIAL    PRIMARY KEY,
    codigo                   VARCHAR(30)  NOT NULL UNIQUE,
    nombre                   VARCHAR(255) NOT NULL,
    descripcion              VARCHAR(1000),
    fecha_creacion           DATE         NOT NULL DEFAULT CURRENT_DATE,
    nombre_original          VARCHAR(255) NOT NULL,
    nombre_almacenado        VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento      VARCHAR(500) NOT NULL,
    mime_type                VARCHAR(100) NOT NULL,
    tamano_bytes             BIGINT       NOT NULL,
    creado_por               VARCHAR(200) NOT NULL,
    creado_en                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documento_interno_fecha ON documento_interno(fecha_creacion);
CREATE INDEX idx_documento_interno_nombre ON documento_interno(nombre);

-- =============================================================================
-- FIN DEL ESQUEMA
-- =============================================================================
