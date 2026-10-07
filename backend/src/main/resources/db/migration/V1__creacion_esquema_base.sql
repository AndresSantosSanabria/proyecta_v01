-- =============================================================================
-- PROYECTA - Esquema Base Consolidado (V1)
-- Fecha: 2026-10-02 (consolida la reestructuracion 2026-10)
-- Base de datos: PostgreSQL 15+
-- Esquema: proyecta_db
--
-- Esta migracion es el estado FINAL del esquema; equivalente al historial
-- anterior V1..V8 fusionado:
--   * integridad: 58 FKs validadas (hijos de proyecto -> CASCADE),
--     UNIQUE/CHECK de dominio, tipos bigint normalizados
--   * estado dual por catalogo (estado_proyecto_config / estado_entregable_config)
--   * convencion de nombres: proyecto_id / creado_en / actualizado_en
--     (excepcion: documento_interno.fecha_creacion = fecha del documento)
--   * indices: sin redundantes, cobertura de todas las FKs
--   * system_audit_log particionada por rango (creado_en) 2026-2031 + DEFAULT
--   * FK compuestas riesgos(probabilidad, impacto) -> matriz_riesgo
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Las funciones se declaran antes que las tablas (orden de pg_dump): se omite
-- la validacion de cuerpos al crear; las tablas existen al momento de ejecutar.
SET check_function_bodies = false;

--
-- PostgreSQL database dump
--


-- Dumped from database version 17.9
-- Dumped by pg_dump version 17.9


--
-- Name: proyecta_db; Type: SCHEMA; Schema: -; Owner: -
--


--
-- Name: calc_suma_ponderaciones_fases(character varying); Type: FUNCTION; Schema: proyecta_db; Owner: -
--

CREATE OR REPLACE FUNCTION proyecta_db.calc_suma_ponderaciones_fases(p_proyecto_id character varying) RETURNS numeric
    LANGUAGE sql
    AS $$
    SELECT COALESCE(SUM(ponderacion), 0) FROM fase WHERE proyecto_id = p_proyecto_id;
$$;


--
-- Name: fn_auditar_cambios_ponderacion(); Type: FUNCTION; Schema: proyecta_db; Owner: -
--

CREATE OR REPLACE FUNCTION proyecta_db.fn_auditar_cambios_ponderacion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF OLD.ponderacion IS DISTINCT FROM NEW.ponderacion THEN
        INSERT INTO auditoria_ponderaciones
            (proyecto_id, fase_id, ponderacion_anterior, ponderacion_nueva, usuario_id, fecha_cambio)
        VALUES
            (NEW.proyecto_id, NEW.fase_id, OLD.ponderacion, NEW.ponderacion, current_setting('app.current_user_id', TRUE), NOW());
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_validar_ponderacion_fase(); Type: FUNCTION; Schema: proyecta_db; Owner: -
--

CREATE OR REPLACE FUNCTION proyecta_db.fn_validar_ponderacion_fase() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: normalizar_ponderaciones_fases(character varying); Type: FUNCTION; Schema: proyecta_db; Owner: -
--

CREATE OR REPLACE FUNCTION proyecta_db.normalizar_ponderaciones_fases(p_proyecto_id character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: actas_cierre; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.actas_cierre (
    acta_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    resumen_ejecutivo text NOT NULL,
    fecha_cierre timestamp without time zone NOT NULL,
    avance_final numeric(5,2) NOT NULL,
    progreso_programado_final numeric(5,2),
    progreso_ejecutado_final numeric(5,2),
    diferencia_final numeric(5,2),
    eficacia_final numeric(6,4),
    estado_final character varying(30),
    corte_calculo date,
    snapshot_json text,
    archivo_pdf character varying(255),
    ruta_archivo_pdf character varying(255),
    archivo_docx character varying(255),
    ruta_archivo_docx character varying(255)
);


--
-- Name: actas_cierre_acta_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.actas_cierre_acta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: actas_cierre_acta_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.actas_cierre_acta_id_seq OWNED BY proyecta_db.actas_cierre.acta_id;


--
-- Name: advance_report_uploads; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.advance_report_uploads (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    periodo character varying(20) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    file_size bigint,
    uploaded_by character varying(120) NOT NULL,
    uploaded_at timestamp without time zone DEFAULT now() NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observaciones character varying(1000),
    verified_by character varying(120),
    verified_at timestamp without time zone,
    returned_by character varying(120),
    returned_at timestamp without time zone,
    subido_rol character varying(60),
    CONSTRAINT chk_adv_uploads_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VERIFICADO'::character varying, 'DEVUELTO'::character varying])::text[])))
);


--
-- Name: advance_report_uploads_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.advance_report_uploads_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: advance_report_uploads_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.advance_report_uploads_id_seq OWNED BY proyecta_db.advance_report_uploads.id;


--
-- Name: advance_report_version; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.advance_report_version (
    id bigint NOT NULL,
    upload_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    periodo character varying(20) NOT NULL,
    numero_version integer NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    file_size bigint,
    mime_type character varying(120),
    estado character varying(20) DEFAULT 'ACTUAL'::character varying NOT NULL,
    observacion character varying(1000),
    subido_por character varying(120),
    subido_rol character varying(60),
    subido_en timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_adv_version_estado CHECK (((estado)::text = ANY ((ARRAY['ACTUAL'::character varying, 'HISTORICA'::character varying])::text[])))
);


--
-- Name: advance_report_version_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.advance_report_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: advance_report_version_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.advance_report_version_id_seq OWNED BY proyecta_db.advance_report_version.id;


--
-- Name: analitica_portafolio_snapshot; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.analitica_portafolio_snapshot (
    snapshot_id bigint NOT NULL,
    corte_calculo timestamp with time zone NOT NULL,
    fuente character varying(80) DEFAULT 'PORTAFOLIO'::character varying NOT NULL,
    snapshot_json jsonb NOT NULL,
    fecha_generacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: analitica_portafolio_snapshot_snapshot_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.analitica_portafolio_snapshot_snapshot_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: analitica_portafolio_snapshot_snapshot_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.analitica_portafolio_snapshot_snapshot_id_seq OWNED BY proyecta_db.analitica_portafolio_snapshot.snapshot_id;


--
-- Name: auditoria_ponderaciones; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.auditoria_ponderaciones (
    auditoria_id integer NOT NULL,
    proyecto_id character varying(40),
    fase_id integer,
    ponderacion_anterior numeric(5,2),
    ponderacion_nueva numeric(5,2),
    razon_cambio character varying(255),
    usuario_id character varying(50),
    fecha_cambio timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: auditoria_ponderaciones_auditoria_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.auditoria_ponderaciones_auditoria_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_ponderaciones_auditoria_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.auditoria_ponderaciones_auditoria_id_seq OWNED BY proyecta_db.auditoria_ponderaciones.auditoria_id;


--
-- Name: documento; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    tipo_documento character varying(30) NOT NULL,
    tipo_documento_config_id bigint,
    nombre_original character varying(255) NOT NULL,
    nombre_almacenado character varying(255) NOT NULL,
    ruta_almacenamiento character varying(500) NOT NULL,
    url_descarga character varying(500),
    mime_type character varying(100) NOT NULL,
    tamano_bytes bigint NOT NULL,
    fecha_carga timestamp without time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp without time zone
);


--
-- Name: documento_auditoria; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_auditoria (
    documento_auditoria_id bigint NOT NULL,
    entregable_id integer NOT NULL,
    documento_version_id bigint,
    documento_observacion_id bigint,
    accion character varying(50) NOT NULL,
    actor character varying(200),
    actor_rol character varying(80),
    fecha timestamp without time zone DEFAULT now() NOT NULL,
    metadata text
);


--
-- Name: documento_auditoria_documento_auditoria_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_auditoria_documento_auditoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_auditoria_documento_auditoria_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_auditoria_documento_auditoria_id_seq OWNED BY proyecta_db.documento_auditoria.documento_auditoria_id;


--
-- Name: documento_dinamico; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_dinamico (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    tipo_documento character varying(100) NOT NULL,
    nombre_original character varying(255) NOT NULL,
    nombre_almacenado character varying(255) NOT NULL,
    ruta_almacenamiento character varying(500) NOT NULL,
    url_descarga character varying(500),
    mime_type character varying(100) NOT NULL,
    tamano_bytes bigint NOT NULL,
    fecha_carga timestamp without time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp without time zone
);


--
-- Name: documento_dinamico_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_dinamico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_dinamico_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_dinamico_id_seq OWNED BY proyecta_db.documento_dinamico.id;


--
-- Name: documento_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_id_seq OWNED BY proyecta_db.documento.id;


--
-- Name: documento_interno; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_interno (
    id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(255) NOT NULL,
    descripcion character varying(1000),
    fecha_creacion date DEFAULT CURRENT_DATE NOT NULL,
    nombre_original character varying(255) NOT NULL,
    nombre_almacenado character varying(255) NOT NULL,
    ruta_almacenamiento character varying(500) NOT NULL,
    mime_type character varying(100) NOT NULL,
    tamano_bytes bigint NOT NULL,
    creado_por bigint,
    creado_por_nombre character varying(200) NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: documento_interno_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_interno_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_interno_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_interno_id_seq OWNED BY proyecta_db.documento_interno.id;


--
-- Name: documento_observacion; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_observacion (
    documento_observacion_id bigint NOT NULL,
    entregable_id integer NOT NULL,
    documento_version_id bigint,
    observacion text NOT NULL,
    estado character varying(30) NOT NULL,
    creada_por character varying(200),
    creada_rol character varying(80),
    creada_en timestamp without time zone DEFAULT now() NOT NULL,
    subsanada_por character varying(200),
    subsanada_en timestamp without time zone,
    comentario_subsanacion character varying(1000),
    cerrada_por character varying(200),
    cerrada_en timestamp without time zone,
    CONSTRAINT documento_observacion_estado_check CHECK (((estado)::text = ANY ((ARRAY['ABIERTA'::character varying, 'SUBSANADA'::character varying, 'CERRADA'::character varying])::text[])))
);


--
-- Name: documento_observacion_documento_observacion_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_observacion_documento_observacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_observacion_documento_observacion_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_observacion_documento_observacion_id_seq OWNED BY proyecta_db.documento_observacion.documento_observacion_id;


--
-- Name: documento_pre_wizard_revision; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_pre_wizard_revision (
    documento_pre_wizard_revision_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    tipo_documento character varying(30) NOT NULL,
    estado character varying(30) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    observacion character varying(1000),
    revisado_por character varying(200),
    revisado_en timestamp without time zone,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_pre_wizard_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADO'::character varying, 'DEVUELTO'::character varying])::text[]))),
    CONSTRAINT chk_pre_wizard_tipo CHECK (((tipo_documento)::text = ANY ((ARRAY['VIABILIZACION'::character varying, 'PLAN_COMUNICACIONES'::character varying, 'MATRIZ_RIESGOS_VIABILIDAD'::character varying])::text[])))
);


--
-- Name: documento_pre_wizard_revision_documento_pre_wizard_revision_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_pre_wizard_revision_documento_pre_wizard_revision_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_pre_wizard_revision_documento_pre_wizard_revision_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_pre_wizard_revision_documento_pre_wizard_revision_seq OWNED BY proyecta_db.documento_pre_wizard_revision.documento_pre_wizard_revision_id;


--
-- Name: documento_proyecto_version; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_proyecto_version (
    documento_proyecto_version_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    tipo_documento character varying(30) NOT NULL,
    numero_version integer NOT NULL,
    nombre_archivo_original character varying(255) NOT NULL,
    nombre_almacenado character varying(255) NOT NULL,
    ruta_almacenamiento character varying(500) NOT NULL,
    mime_type character varying(100) NOT NULL,
    tamano_bytes bigint NOT NULL,
    observacion character varying(1000),
    estado character varying(30) NOT NULL,
    subido_por character varying(200),
    subido_rol character varying(80),
    subido_en timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT documento_proyecto_version_estado_check CHECK (((estado)::text = ANY ((ARRAY['ACTUAL'::character varying, 'HISTORICA'::character varying, 'REVERTIDA'::character varying])::text[])))
);


--
-- Name: documento_proyecto_version_documento_proyecto_version_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_proyecto_version_documento_proyecto_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_proyecto_version_documento_proyecto_version_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_proyecto_version_documento_proyecto_version_id_seq OWNED BY proyecta_db.documento_proyecto_version.documento_proyecto_version_id;


--
-- Name: documento_version; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.documento_version (
    documento_version_id bigint NOT NULL,
    entregable_id integer NOT NULL,
    numero_version integer NOT NULL,
    nombre_archivo_original character varying(255) NOT NULL,
    archivo_storage character varying(300) NOT NULL,
    mime_type character varying(100) DEFAULT 'application/pdf'::character varying NOT NULL,
    size_bytes bigint,
    checksum_sha256 character varying(64),
    fecha_entrega date,
    comentario_carga character varying(1000),
    estado character varying(30) NOT NULL,
    subido_por character varying(200),
    subido_rol character varying(80),
    subido_en timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT documento_version_estado_check CHECK (((estado)::text = ANY ((ARRAY['ACTUAL'::character varying, 'HISTORICA'::character varying, 'REVERTIDA'::character varying])::text[])))
);


--
-- Name: documento_version_documento_version_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.documento_version_documento_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documento_version_documento_version_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.documento_version_documento_version_id_seq OWNED BY proyecta_db.documento_version.documento_version_id;


--
-- Name: entregable; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.entregable (
    entregable_id integer NOT NULL,
    nombre character varying(300) NOT NULL,
    descripcion character varying(300),
    ponderacion numeric(5,2) NOT NULL,
    estado_config_id bigint NOT NULL,
    conforme boolean DEFAULT false NOT NULL,
    archivo_pdf character varying(300),
    fecha_limite date,
    fecha_entrega_real date,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    hito_id integer NOT NULL,
    observacion_revision character varying(1000),
    fecha_inicio date,
    retroactivo boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_entregable_ponderacion CHECK (((ponderacion >= (0)::numeric) AND (ponderacion <= (100)::numeric)))
);


--
-- Name: entregable_cambio_descripcion; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.entregable_cambio_descripcion (
    id bigint NOT NULL,
    entregable_id integer NOT NULL,
    descripcion_anterior text,
    descripcion_nueva text NOT NULL,
    justificacion text NOT NULL,
    archivo_pdf text NOT NULL,
    nombre_original character varying(300),
    usuario character varying(255) NOT NULL,
    usuario_rol character varying(500),
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: entregable_cambio_descripcion_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.entregable_cambio_descripcion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entregable_cambio_descripcion_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.entregable_cambio_descripcion_id_seq OWNED BY proyecta_db.entregable_cambio_descripcion.id;


--
-- Name: entregable_cambio_fecha; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.entregable_cambio_fecha (
    id bigint NOT NULL,
    entregable_id integer NOT NULL,
    fecha_anterior date NOT NULL,
    fecha_nueva date NOT NULL,
    justificacion text NOT NULL,
    archivo_pdf character varying(300) NOT NULL,
    nombre_original character varying(300),
    usuario character varying(255) NOT NULL,
    usuario_rol character varying(500),
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: entregable_cambio_fecha_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.entregable_cambio_fecha_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entregable_cambio_fecha_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.entregable_cambio_fecha_id_seq OWNED BY proyecta_db.entregable_cambio_fecha.id;


--
-- Name: entregable_entregable_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.entregable_entregable_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entregable_entregable_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.entregable_entregable_id_seq OWNED BY proyecta_db.entregable.entregable_id;


--
-- Name: estado_entregable_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.estado_entregable_config (
    estado_entregable_id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(300),
    es_conforme boolean DEFAULT false NOT NULL,
    es_terminal boolean DEFAULT false NOT NULL,
    cuenta_avance numeric(5,2) DEFAULT 0 NOT NULL,
    color_hex character varying(7),
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estado_entregable_config_estado_entregable_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.estado_entregable_config_estado_entregable_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estado_entregable_config_estado_entregable_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.estado_entregable_config_estado_entregable_id_seq OWNED BY proyecta_db.estado_entregable_config.estado_entregable_id;


--
-- Name: estado_proyecto_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.estado_proyecto_config (
    estado_proyecto_id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(300),
    color_hex character varying(7),
    es_terminal boolean DEFAULT false NOT NULL,
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estado_proyecto_config_estado_proyecto_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.estado_proyecto_config_estado_proyecto_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estado_proyecto_config_estado_proyecto_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.estado_proyecto_config_estado_proyecto_id_seq OWNED BY proyecta_db.estado_proyecto_config.estado_proyecto_id;


--
-- Name: estrategia_peti_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.estrategia_peti_config (
    estrategia_peti_id bigint NOT NULL,
    codigo character varying(80) NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text,
    vigencia_desde character varying(20),
    vigencia_hasta character varying(20),
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: estrategia_peti_config_estrategia_peti_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.estrategia_peti_config_estrategia_peti_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estrategia_peti_config_estrategia_peti_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.estrategia_peti_config_estrategia_peti_id_seq OWNED BY proyecta_db.estrategia_peti_config.estrategia_peti_id;


--
-- Name: fase; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.fase (
    fase_id integer NOT NULL,
    nombre character varying(150),
    descripcion character varying(300),
    ponderacion numeric(5,2) NOT NULL,
    avance_calculado numeric(5,2) DEFAULT 0.00 NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    CONSTRAINT chk_fase_avance_calculado CHECK (((avance_calculado >= (0)::numeric) AND (avance_calculado <= (100)::numeric))),
    CONSTRAINT chk_fase_ponderacion CHECK (((ponderacion >= (0)::numeric) AND (ponderacion <= (100)::numeric)))
);


--
-- Name: fase_fase_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.fase_fase_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fase_fase_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.fase_fase_id_seq OWNED BY proyecta_db.fase.fase_id;


--
-- Name: hito; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.hito (
    hito_id integer NOT NULL,
    nombre character varying(150),
    descripcion character varying(300),
    ponderacion numeric(5,2) NOT NULL,
    avance_calculado numeric(5,2) DEFAULT 0.00 NOT NULL,
    estado_revision character varying(30),
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    fase_id integer NOT NULL,
    CONSTRAINT chk_hito_avance_calculado CHECK (((avance_calculado >= (0)::numeric) AND (avance_calculado <= (100)::numeric))),
    CONSTRAINT chk_hito_ponderacion CHECK (((ponderacion >= (0)::numeric) AND (ponderacion <= (100)::numeric))),
    CONSTRAINT hito_estado_revision_check CHECK (((estado_revision)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying])::text[])))
);


--
-- Name: hito_hito_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.hito_hito_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hito_hito_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.hito_hito_id_seq OWNED BY proyecta_db.hito.hito_id;


--
-- Name: lista_parametrica_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.lista_parametrica_config (
    id bigint NOT NULL,
    lista_clave character varying(60) NOT NULL,
    item_codigo character varying(200) NOT NULL,
    item_nombre character varying(200) NOT NULL,
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    lista_nombre_campo character varying(200),
    lista_descripcion character varying(500),
    lista_tipo character varying(30) DEFAULT 'Lista'::character varying
);


--
-- Name: lista_parametrica_config_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.lista_parametrica_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lista_parametrica_config_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.lista_parametrica_config_id_seq OWNED BY proyecta_db.lista_parametrica_config.id;


--
-- Name: matriz_riesgo; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.matriz_riesgo (
    matriz_riesgo_id bigint NOT NULL,
    probabilidad character varying(30) NOT NULL,
    impacto character varying(30) NOT NULL,
    nivel_riesgo character varying(30) NOT NULL,
    color character varying(20) NOT NULL,
    puntaje integer NOT NULL
);


--
-- Name: matriz_riesgo_matriz_riesgo_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.matriz_riesgo_matriz_riesgo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matriz_riesgo_matriz_riesgo_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.matriz_riesgo_matriz_riesgo_id_seq OWNED BY proyecta_db.matriz_riesgo.matriz_riesgo_id;


--
-- Name: notification_audit; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_audit (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_code character varying(120) NOT NULL,
    recipient character varying(255) NOT NULL,
    channel character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    failure_reason text,
    proyecto_id character varying(40),
    notification_type character varying(50),
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: notification_event_catalog; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_event_catalog (
    code character varying(120) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    category character varying(40) NOT NULL,
    default_enabled boolean DEFAULT true NOT NULL,
    active boolean DEFAULT true NOT NULL,
    requires_project_context boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: notification_in_app; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_in_app (
    id bigint NOT NULL,
    recipient_user_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    message text NOT NULL,
    event_code character varying(120) NOT NULL,
    read_status boolean DEFAULT false NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at timestamp without time zone,
    source_entity_id character varying(80),
    severity character varying(30),
    target_url text
);


--
-- Name: notification_in_app_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.notification_in_app_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_in_app_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.notification_in_app_id_seq OWNED BY proyecta_db.notification_in_app.id;


--
-- Name: notification_log; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_log (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    notification_date date NOT NULL,
    notification_type character varying(50) NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: notification_log_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.notification_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_log_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.notification_log_id_seq OWNED BY proyecta_db.notification_log.id;


--
-- Name: notification_mail_dispatch_log; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_mail_dispatch_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    recipient character varying(200) NOT NULL,
    subject character varying(500) NOT NULL,
    status character varying(50) NOT NULL,
    detail text,
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: notification_preference; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_preference (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    event_code character varying(120) NOT NULL,
    proyecto_id character varying(40),
    enabled boolean DEFAULT true NOT NULL,
    email_enabled boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone
);


--
-- Name: notification_preference_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.notification_preference_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_preference_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.notification_preference_id_seq OWNED BY proyecta_db.notification_preference.id;


--
-- Name: notification_template; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.notification_template (
    event_code character varying(120) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    html_enabled boolean DEFAULT false NOT NULL,
    severity character varying(30) DEFAULT 'INFO'::character varying,
    scope character varying(30) DEFAULT 'GLOBAL'::character varying,
    subject_template character varying(500) NOT NULL,
    body_template text NOT NULL,
    target_roles jsonb,
    updated_by character varying(120),
    actualizado_en timestamp without time zone
);


--
-- Name: objetivos_especificos; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.objetivos_especificos (
    obj_id integer NOT NULL,
    descripcion text NOT NULL,
    orden smallint,
    proyecto_id character varying(40)
);


--
-- Name: objetivos_especificos_obj_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.objetivos_especificos_obj_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: objetivos_especificos_obj_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.objetivos_especificos_obj_id_seq OWNED BY proyecta_db.objetivos_especificos.obj_id;


--
-- Name: patrocinador; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.patrocinador (
    patrocinador_id integer NOT NULL,
    nombre character varying(120) NOT NULL,
    cargo character varying(100),
    dependencia character varying(100),
    entidad character varying(150),
    proceso_sigc character varying(100),
    procedimiento character varying(150)
);


--
-- Name: patrocinador_patrocinador_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.patrocinador_patrocinador_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: patrocinador_patrocinador_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.patrocinador_patrocinador_id_seq OWNED BY proyecta_db.patrocinador.patrocinador_id;


--
-- Name: permisos; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.permisos (
    id bigint NOT NULL,
    codigo character varying(120) NOT NULL,
    nombre character varying(180) NOT NULL,
    descripcion character varying(400),
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: permisos_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.permisos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permisos_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.permisos_id_seq OWNED BY proyecta_db.permisos.id;


--
-- Name: project_closure_answer; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.project_closure_answer (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    question_id bigint NOT NULL,
    respuesta text,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: project_closure_answer_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.project_closure_answer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_closure_answer_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.project_closure_answer_id_seq OWNED BY proyecta_db.project_closure_answer.id;


--
-- Name: project_closure_question; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.project_closure_question (
    id bigint NOT NULL,
    texto character varying(500) NOT NULL,
    tipo_respuesta character varying(30) DEFAULT 'texto_libre'::character varying NOT NULL,
    opciones jsonb,
    activo boolean DEFAULT true NOT NULL,
    orden integer DEFAULT 0 NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(80),
    updated_by character varying(80)
);


--
-- Name: project_closure_question_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.project_closure_question_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_closure_question_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.project_closure_question_id_seq OWNED BY proyecta_db.project_closure_question.id;


--
-- Name: project_closure_template; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.project_closure_template (
    id bigint NOT NULL,
    codigo_proceso character varying(30) DEFAULT 'A-GT-FR-004'::character varying NOT NULL,
    version_num integer DEFAULT 1 NOT NULL,
    nombre_documento character varying(200) DEFAULT 'Acta de Cierre del Proyecto'::character varying NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    template_json jsonb NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(80),
    updated_by character varying(80)
);


--
-- Name: project_closure_template_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.project_closure_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_closure_template_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.project_closure_template_id_seq OWNED BY proyecta_db.project_closure_template.id;


--
-- Name: project_closures; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.project_closures (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    template_id bigint NOT NULL,
    template_snapshot jsonb NOT NULL,
    form_data jsonb NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(80)
);


--
-- Name: project_closures_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.project_closures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_closures_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.project_closures_id_seq OWNED BY proyecta_db.project_closures.id;


--
-- Name: proyecto; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.proyecto (
    proyecto_id character varying(40) NOT NULL,
    nombre character varying(300) NOT NULL,
    dependencia character varying(200),
    director_nombre character varying(120),
    director_correo character varying(200),
    director_usuario_id bigint,
    objetivo_general text,
    es_peti boolean DEFAULT false NOT NULL,
    estrategia_peti_config_id bigint,
    vigencia_peti character varying(20),
    fecha_inicio date,
    fecha_cierre date,
    tiene_plan_comunicaciones boolean DEFAULT false NOT NULL,
    cronograma_pdf character varying(255),
    acta_constitucion_pdf character varying(255),
    plan_comunicaciones_pdf character varying(255),
    viabilizacion_pdf character varying(255),
    viabilidad_estado character varying(20) DEFAULT 'PENDIENTE'::character varying,
    viabilidad_observaciones text,
    viabilidad_revisado_por character varying(120),
    viabilidad_revisado_en timestamp without time zone,
    documentos_cargados boolean DEFAULT false NOT NULL,
    documentos_verificados boolean DEFAULT false NOT NULL,
    fecha_verificacion_documentos timestamp without time zone,
    fecha_limite_completar date,
    cierre_forzoso boolean DEFAULT false NOT NULL,
    cierre_forzoso_por character varying(120),
    cierre_forzoso_en timestamp without time zone,
    acta_constitucion_cargada boolean DEFAULT false NOT NULL,
    estado_config_id bigint NOT NULL,
    avance_total numeric(5,2) DEFAULT 0.00 NOT NULL,
    cierre_solicitado boolean DEFAULT false NOT NULL,
    cierre_solicitado_en timestamp without time zone,
    cierre_solicitado_por character varying(120),
    cierre_estado character varying(30),
    cierre_observaciones text,
    cierre_borrador_json text,
    completitud_borrador_json text,
    completitud_fases_completadas text,
    patrocinador_id integer,
    presupuesto_estimado numeric(24,2),
    alcance_detallado text,
    fecha_registro timestamp without time zone DEFAULT now() NOT NULL,
    requiere_completitud_director boolean DEFAULT false NOT NULL,
    primer_ingreso_director_at timestamp without time zone,
    completado_por_director_at timestamp without time zone,
    registrado_inicial_por character varying(120),
    email_message_id character varying(255),
    CONSTRAINT chk_proyecto_avance_total CHECK (((avance_total >= (0)::numeric) AND (avance_total <= (100)::numeric))),
    CONSTRAINT chk_proyecto_cierre_estado CHECK (((cierre_estado IS NULL) OR ((cierre_estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying])::text[])))),
    CONSTRAINT chk_proyecto_viabilidad_estado CHECK (((viabilidad_estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CARGADA'::character varying, 'APROBADA'::character varying, 'DEVUELTA'::character varying])::text[])))
);


--
-- Name: proyecto_beneficio_impacto; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.proyecto_beneficio_impacto (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    estado character varying(30) NOT NULL,
    requerido_en timestamp without time zone,
    requerido_por character varying(120),
    diligenciado_en timestamp without time zone,
    diligenciado_por character varying(120),
    revisado_en timestamp without time zone,
    revisado_por character varying(120),
    poblacion_beneficiada_directa integer,
    poblacion_beneficiada_indirecta integer,
    poblacion_objetivo integer,
    territorio_beneficiado character varying(200),
    beneficio_principal text,
    impacto_social text,
    impacto_institucional text,
    impacto_economico text,
    alineacion_plan_desarrollo text,
    alineacion_peti text,
    metas_contribuidas text,
    indicador_base text,
    indicador_meta text,
    indicador_resultado text,
    fuente_verificacion text,
    observaciones text,
    snapshot_json jsonb,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_beneficio_impacto_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'DILIGENCIADO'::character varying, 'OBSERVADO'::character varying, 'APROBADO'::character varying])::text[])))
);


--
-- Name: proyecto_beneficio_impacto_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.proyecto_beneficio_impacto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: proyecto_beneficio_impacto_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.proyecto_beneficio_impacto_id_seq OWNED BY proyecta_db.proyecto_beneficio_impacto.id;


--
-- Name: proyecto_equipo; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.proyecto_equipo (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    miembro_nombre character varying(120),
    miembro_rol character varying(100),
    miembro_cargo character varying(100),
    miembro_dependencia character varying(150),
    miembro_telefono character varying(30),
    miembro_correo character varying(150)
);


--
-- Name: proyecto_equipo_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.proyecto_equipo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: proyecto_equipo_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.proyecto_equipo_id_seq OWNED BY proyecta_db.proyecto_equipo.id;


--
-- Name: proyecto_stakeholder; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.proyecto_stakeholder (
    id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    stakeholder_rol character varying(150),
    stakeholder_descripcion text,
    stakeholder_interes text,
    stakeholder_impacto text
);


--
-- Name: proyecto_stakeholder_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.proyecto_stakeholder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: proyecto_stakeholder_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.proyecto_stakeholder_id_seq OWNED BY proyecta_db.proyecto_stakeholder.id;


--
-- Name: public_evidence_access; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.public_evidence_access (
    id bigint NOT NULL,
    entregable_id integer NOT NULL,
    token character varying(64) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(200)
);


--
-- Name: public_evidence_access_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.public_evidence_access_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: public_evidence_access_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.public_evidence_access_id_seq OWNED BY proyecta_db.public_evidence_access.id;


--
-- Name: reporte_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.reporte_config (
    id character varying(50) NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(300),
    orden integer NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: respuestas_furag; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.respuestas_furag (
    respuesta_furag_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    codigo_pregunta character varying(80) NOT NULL,
    pregunta text NOT NULL,
    respuesta character varying(5),
    obligatoria boolean DEFAULT true NOT NULL,
    fecha_actualizacion timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: respuestas_furag_respuesta_furag_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.respuestas_furag_respuesta_furag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: respuestas_furag_respuesta_furag_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.respuestas_furag_respuesta_furag_id_seq OWNED BY proyecta_db.respuestas_furag.respuesta_furag_id;


--
-- Name: riesgo_solucion_adjunto; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.riesgo_solucion_adjunto (
    riesgo_solucion_adjunto_id bigint NOT NULL,
    riesgo_id integer NOT NULL,
    nombre_original character varying(300) NOT NULL,
    nombre_almacenado character varying(300) NOT NULL,
    ruta_almacenamiento character varying(120) NOT NULL,
    mime_type character varying(120) NOT NULL,
    tamano_bytes bigint NOT NULL,
    fecha_carga timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: riesgo_solucion_adjunto_riesgo_solucion_adjunto_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.riesgo_solucion_adjunto_riesgo_solucion_adjunto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: riesgo_solucion_adjunto_riesgo_solucion_adjunto_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.riesgo_solucion_adjunto_riesgo_solucion_adjunto_id_seq OWNED BY proyecta_db.riesgo_solucion_adjunto.riesgo_solucion_adjunto_id;


--
-- Name: riesgo_tratamiento; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.riesgo_tratamiento (
    riesgo_tratamiento_id bigint NOT NULL,
    riesgo_id integer NOT NULL,
    iteracion integer NOT NULL,
    comentario text NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: riesgo_tratamiento_adjunto; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.riesgo_tratamiento_adjunto (
    riesgo_tratamiento_adjunto_id bigint NOT NULL,
    riesgo_tratamiento_id bigint NOT NULL,
    nombre_original character varying(300) NOT NULL,
    nombre_almacenado character varying(300) NOT NULL,
    ruta_almacenamiento character varying(120) NOT NULL,
    mime_type character varying(120) NOT NULL,
    tamano_bytes bigint NOT NULL,
    fecha_carga timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: riesgo_tratamiento_adjunto_riesgo_tratamiento_adjunto_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.riesgo_tratamiento_adjunto_riesgo_tratamiento_adjunto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: riesgo_tratamiento_adjunto_riesgo_tratamiento_adjunto_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.riesgo_tratamiento_adjunto_riesgo_tratamiento_adjunto_id_seq OWNED BY proyecta_db.riesgo_tratamiento_adjunto.riesgo_tratamiento_adjunto_id;


--
-- Name: riesgo_tratamiento_riesgo_tratamiento_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.riesgo_tratamiento_riesgo_tratamiento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: riesgo_tratamiento_riesgo_tratamiento_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.riesgo_tratamiento_riesgo_tratamiento_id_seq OWNED BY proyecta_db.riesgo_tratamiento.riesgo_tratamiento_id;


--
-- Name: riesgos; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.riesgos (
    riesgo_id integer NOT NULL,
    codigo character varying(10),
    descripcion text NOT NULL,
    probabilidad character varying(10),
    impacto character varying(10),
    nivel character varying(30),
    tratamiento text,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    fecha_actualizacion timestamp without time zone,
    proyecto_id character varying(40) NOT NULL,
    categoria_riesgo character varying(120),
    causa text,
    consecuencia text,
    controles_existentes text,
    tipo_control character varying(80),
    valoracion_control character varying(80),
    probabilidad_residual character varying(10),
    impacto_residual character varying(10),
    nivel_residual character varying(20),
    acciones_mitigacion text,
    entidad_responsable character varying(150),
    rol_responsable character varying(150),
    fecha_accion date,
    evidencia_indicador text,
    tipo_riesgo character varying(20) DEFAULT 'GENERAL'::character varying NOT NULL,
    created_by character varying(150),
    CONSTRAINT chk_riesgos_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'TRATADO'::character varying])::text[]))),
    CONSTRAINT riesgos_impacto_check CHECK (((impacto)::text = ANY ((ARRAY['UNO'::character varying, 'DOS'::character varying, 'TRES'::character varying, 'CUATRO'::character varying, 'CINCO'::character varying])::text[]))),
    CONSTRAINT riesgos_impacto_residual_check CHECK (((impacto_residual)::text = ANY ((ARRAY['UNO'::character varying, 'DOS'::character varying, 'TRES'::character varying, 'CUATRO'::character varying, 'CINCO'::character varying])::text[]))),
    CONSTRAINT riesgos_probabilidad_check CHECK (((probabilidad)::text = ANY ((ARRAY['UNO'::character varying, 'DOS'::character varying, 'TRES'::character varying, 'CUATRO'::character varying, 'CINCO'::character varying])::text[]))),
    CONSTRAINT riesgos_probabilidad_residual_check CHECK (((probabilidad_residual)::text = ANY ((ARRAY['UNO'::character varying, 'DOS'::character varying, 'TRES'::character varying, 'CUATRO'::character varying, 'CINCO'::character varying])::text[])))
);


--
-- Name: riesgos_riesgo_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.riesgos_riesgo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: riesgos_riesgo_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.riesgos_riesgo_id_seq OWNED BY proyecta_db.riesgos.riesgo_id;


--
-- Name: rol_permiso; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.rol_permiso (
    id bigint NOT NULL,
    rol_id bigint NOT NULL,
    permiso_id bigint NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: rol_permiso_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.rol_permiso_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rol_permiso_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.rol_permiso_id_seq OWNED BY proyecta_db.rol_permiso.id;


--
-- Name: roles; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.roles (
    id bigint NOT NULL,
    codigo character varying(60) NOT NULL,
    nombre character varying(120) NOT NULL,
    descripcion character varying(300),
    transversal boolean DEFAULT false NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.roles_id_seq OWNED BY proyecta_db.roles.id;


--
-- Name: system_audit_log; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.system_audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id character varying(100),
    usuario_nombre character varying(255),
    usuario_rol character varying(100),
    accion character varying(50) NOT NULL,
    modulo character varying(150) NOT NULL,
    metodo_http character varying(10),
    recurso character varying(255),
    codigo_estado integer NOT NULL,
    estado character varying(20) NOT NULL,
    detalle text,
    traza_error text,
    ip_origen character varying(45),
    user_agent text,
    duracion_ms bigint,
    eliminado boolean DEFAULT false NOT NULL,
    fecha_eliminacion timestamp without time zone,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    entidad_tipo character varying(100),
    entidad_id character varying(100),
    respuesta_body text,
    request_body text,
    CONSTRAINT chk_audit_log_estado CHECK (((estado)::text = ANY ((ARRAY['SUCCESS'::character varying, 'ERROR'::character varying])::text[])))
);


--
-- Name: system_parameters; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.system_parameters (
    param_key character varying(50) NOT NULL,
    param_value character varying(255) NOT NULL,
    descripcion character varying(255)
);


--
-- Name: tipo_documento_config; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.tipo_documento_config (
    tipo_documento_id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(300),
    require_pdf boolean DEFAULT true NOT NULL,
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: tipo_documento_config_tipo_documento_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.tipo_documento_config_tipo_documento_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tipo_documento_config_tipo_documento_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.tipo_documento_config_tipo_documento_id_seq OWNED BY proyecta_db.tipo_documento_config.tipo_documento_id;


--
-- Name: usuario_permiso; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.usuario_permiso (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    permiso_id bigint NOT NULL,
    concedido boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT now() NOT NULL,
    fecha_modificacion timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: usuario_permiso_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.usuario_permiso_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuario_permiso_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.usuario_permiso_id_seq OWNED BY proyecta_db.usuario_permiso.id;


--
-- Name: usuario_proyecto; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.usuario_proyecto (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    proyecto_id character varying(40) NOT NULL,
    cargo character varying(80) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: usuario_proyecto_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.usuario_proyecto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuario_proyecto_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.usuario_proyecto_id_seq OWNED BY proyecta_db.usuario_proyecto.id;


--
-- Name: usuarios; Type: TABLE; Schema: proyecta_db; Owner: -
--

CREATE TABLE proyecta_db.usuarios (
    id bigint NOT NULL,
    keycloak_sub character varying(120) NOT NULL,
    username character varying(120) NOT NULL,
    nombre character varying(180) NOT NULL,
    correo character varying(200) NOT NULL,
    dependencia character varying(180),
    rol_codigo character varying(120),
    rol_nombre character varying(180),
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ultimo_acceso timestamp without time zone,
    recibir_notificaciones_globales boolean DEFAULT false NOT NULL
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: proyecta_db; Owner: -
--

CREATE SEQUENCE proyecta_db.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: proyecta_db; Owner: -
--

ALTER SEQUENCE proyecta_db.usuarios_id_seq OWNED BY proyecta_db.usuarios.id;


--
-- Name: actas_cierre acta_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.actas_cierre ALTER COLUMN acta_id SET DEFAULT nextval('proyecta_db.actas_cierre_acta_id_seq'::regclass);


--
-- Name: advance_report_uploads id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_uploads ALTER COLUMN id SET DEFAULT nextval('proyecta_db.advance_report_uploads_id_seq'::regclass);


--
-- Name: advance_report_version id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_version ALTER COLUMN id SET DEFAULT nextval('proyecta_db.advance_report_version_id_seq'::regclass);


--
-- Name: analitica_portafolio_snapshot snapshot_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.analitica_portafolio_snapshot ALTER COLUMN snapshot_id SET DEFAULT nextval('proyecta_db.analitica_portafolio_snapshot_snapshot_id_seq'::regclass);


--
-- Name: auditoria_ponderaciones auditoria_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.auditoria_ponderaciones ALTER COLUMN auditoria_id SET DEFAULT nextval('proyecta_db.auditoria_ponderaciones_auditoria_id_seq'::regclass);


--
-- Name: documento id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento ALTER COLUMN id SET DEFAULT nextval('proyecta_db.documento_id_seq'::regclass);


--
-- Name: documento_auditoria documento_auditoria_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_auditoria ALTER COLUMN documento_auditoria_id SET DEFAULT nextval('proyecta_db.documento_auditoria_documento_auditoria_id_seq'::regclass);


--
-- Name: documento_dinamico id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_dinamico ALTER COLUMN id SET DEFAULT nextval('proyecta_db.documento_dinamico_id_seq'::regclass);


--
-- Name: documento_interno id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_interno ALTER COLUMN id SET DEFAULT nextval('proyecta_db.documento_interno_id_seq'::regclass);


--
-- Name: documento_observacion documento_observacion_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_observacion ALTER COLUMN documento_observacion_id SET DEFAULT nextval('proyecta_db.documento_observacion_documento_observacion_id_seq'::regclass);


--
-- Name: documento_pre_wizard_revision documento_pre_wizard_revision_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_pre_wizard_revision ALTER COLUMN documento_pre_wizard_revision_id SET DEFAULT nextval('proyecta_db.documento_pre_wizard_revision_documento_pre_wizard_revision_seq'::regclass);


--
-- Name: documento_proyecto_version documento_proyecto_version_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_proyecto_version ALTER COLUMN documento_proyecto_version_id SET DEFAULT nextval('proyecta_db.documento_proyecto_version_documento_proyecto_version_id_seq'::regclass);


--
-- Name: documento_version documento_version_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_version ALTER COLUMN documento_version_id SET DEFAULT nextval('proyecta_db.documento_version_documento_version_id_seq'::regclass);


--
-- Name: entregable entregable_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable ALTER COLUMN entregable_id SET DEFAULT nextval('proyecta_db.entregable_entregable_id_seq'::regclass);


--
-- Name: entregable_cambio_descripcion id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_descripcion ALTER COLUMN id SET DEFAULT nextval('proyecta_db.entregable_cambio_descripcion_id_seq'::regclass);


--
-- Name: entregable_cambio_fecha id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_fecha ALTER COLUMN id SET DEFAULT nextval('proyecta_db.entregable_cambio_fecha_id_seq'::regclass);


--
-- Name: estado_entregable_config estado_entregable_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_entregable_config ALTER COLUMN estado_entregable_id SET DEFAULT nextval('proyecta_db.estado_entregable_config_estado_entregable_id_seq'::regclass);


--
-- Name: estado_proyecto_config estado_proyecto_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_proyecto_config ALTER COLUMN estado_proyecto_id SET DEFAULT nextval('proyecta_db.estado_proyecto_config_estado_proyecto_id_seq'::regclass);


--
-- Name: estrategia_peti_config estrategia_peti_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estrategia_peti_config ALTER COLUMN estrategia_peti_id SET DEFAULT nextval('proyecta_db.estrategia_peti_config_estrategia_peti_id_seq'::regclass);


--
-- Name: fase fase_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.fase ALTER COLUMN fase_id SET DEFAULT nextval('proyecta_db.fase_fase_id_seq'::regclass);


--
-- Name: hito hito_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.hito ALTER COLUMN hito_id SET DEFAULT nextval('proyecta_db.hito_hito_id_seq'::regclass);


--
-- Name: lista_parametrica_config id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.lista_parametrica_config ALTER COLUMN id SET DEFAULT nextval('proyecta_db.lista_parametrica_config_id_seq'::regclass);


--
-- Name: matriz_riesgo matriz_riesgo_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.matriz_riesgo ALTER COLUMN matriz_riesgo_id SET DEFAULT nextval('proyecta_db.matriz_riesgo_matriz_riesgo_id_seq'::regclass);


--
-- Name: notification_in_app id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_in_app ALTER COLUMN id SET DEFAULT nextval('proyecta_db.notification_in_app_id_seq'::regclass);


--
-- Name: notification_log id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_log ALTER COLUMN id SET DEFAULT nextval('proyecta_db.notification_log_id_seq'::regclass);


--
-- Name: notification_preference id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference ALTER COLUMN id SET DEFAULT nextval('proyecta_db.notification_preference_id_seq'::regclass);


--
-- Name: objetivos_especificos obj_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.objetivos_especificos ALTER COLUMN obj_id SET DEFAULT nextval('proyecta_db.objetivos_especificos_obj_id_seq'::regclass);


--
-- Name: patrocinador patrocinador_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.patrocinador ALTER COLUMN patrocinador_id SET DEFAULT nextval('proyecta_db.patrocinador_patrocinador_id_seq'::regclass);


--
-- Name: permisos id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.permisos ALTER COLUMN id SET DEFAULT nextval('proyecta_db.permisos_id_seq'::regclass);


--
-- Name: project_closure_answer id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_answer ALTER COLUMN id SET DEFAULT nextval('proyecta_db.project_closure_answer_id_seq'::regclass);


--
-- Name: project_closure_question id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_question ALTER COLUMN id SET DEFAULT nextval('proyecta_db.project_closure_question_id_seq'::regclass);


--
-- Name: project_closure_template id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_template ALTER COLUMN id SET DEFAULT nextval('proyecta_db.project_closure_template_id_seq'::regclass);


--
-- Name: project_closures id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closures ALTER COLUMN id SET DEFAULT nextval('proyecta_db.project_closures_id_seq'::regclass);


--
-- Name: proyecto_beneficio_impacto id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_beneficio_impacto ALTER COLUMN id SET DEFAULT nextval('proyecta_db.proyecto_beneficio_impacto_id_seq'::regclass);


--
-- Name: proyecto_equipo id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_equipo ALTER COLUMN id SET DEFAULT nextval('proyecta_db.proyecto_equipo_id_seq'::regclass);


--
-- Name: proyecto_stakeholder id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_stakeholder ALTER COLUMN id SET DEFAULT nextval('proyecta_db.proyecto_stakeholder_id_seq'::regclass);


--
-- Name: public_evidence_access id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.public_evidence_access ALTER COLUMN id SET DEFAULT nextval('proyecta_db.public_evidence_access_id_seq'::regclass);


--
-- Name: respuestas_furag respuesta_furag_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.respuestas_furag ALTER COLUMN respuesta_furag_id SET DEFAULT nextval('proyecta_db.respuestas_furag_respuesta_furag_id_seq'::regclass);


--
-- Name: riesgo_solucion_adjunto riesgo_solucion_adjunto_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_solucion_adjunto ALTER COLUMN riesgo_solucion_adjunto_id SET DEFAULT nextval('proyecta_db.riesgo_solucion_adjunto_riesgo_solucion_adjunto_id_seq'::regclass);


--
-- Name: riesgo_tratamiento riesgo_tratamiento_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento ALTER COLUMN riesgo_tratamiento_id SET DEFAULT nextval('proyecta_db.riesgo_tratamiento_riesgo_tratamiento_id_seq'::regclass);


--
-- Name: riesgo_tratamiento_adjunto riesgo_tratamiento_adjunto_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento_adjunto ALTER COLUMN riesgo_tratamiento_adjunto_id SET DEFAULT nextval('proyecta_db.riesgo_tratamiento_adjunto_riesgo_tratamiento_adjunto_id_seq'::regclass);


--
-- Name: riesgos riesgo_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos ALTER COLUMN riesgo_id SET DEFAULT nextval('proyecta_db.riesgos_riesgo_id_seq'::regclass);


--
-- Name: rol_permiso id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.rol_permiso ALTER COLUMN id SET DEFAULT nextval('proyecta_db.rol_permiso_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.roles ALTER COLUMN id SET DEFAULT nextval('proyecta_db.roles_id_seq'::regclass);


--
-- Name: tipo_documento_config tipo_documento_id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.tipo_documento_config ALTER COLUMN tipo_documento_id SET DEFAULT nextval('proyecta_db.tipo_documento_config_tipo_documento_id_seq'::regclass);


--
-- Name: usuario_permiso id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_permiso ALTER COLUMN id SET DEFAULT nextval('proyecta_db.usuario_permiso_id_seq'::regclass);


--
-- Name: usuario_proyecto id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_proyecto ALTER COLUMN id SET DEFAULT nextval('proyecta_db.usuario_proyecto_id_seq'::regclass);


--
-- Name: usuarios id; Type: DEFAULT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuarios ALTER COLUMN id SET DEFAULT nextval('proyecta_db.usuarios_id_seq'::regclass);


--
-- Name: actas_cierre actas_cierre_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.actas_cierre
    ADD CONSTRAINT actas_cierre_pkey PRIMARY KEY (acta_id);


--
-- Name: actas_cierre actas_cierre_proyecto_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.actas_cierre
    ADD CONSTRAINT actas_cierre_proyecto_id_key UNIQUE (proyecto_id);


--
-- Name: advance_report_uploads advance_report_uploads_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_uploads
    ADD CONSTRAINT advance_report_uploads_pkey PRIMARY KEY (id);


--
-- Name: advance_report_uploads advance_report_uploads_project_id_periodo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_uploads
    ADD CONSTRAINT advance_report_uploads_project_id_periodo_key UNIQUE (proyecto_id, periodo);


--
-- Name: advance_report_version advance_report_version_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_version
    ADD CONSTRAINT advance_report_version_pkey PRIMARY KEY (id);


--
-- Name: advance_report_version advance_report_version_upload_id_numero_version_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_version
    ADD CONSTRAINT advance_report_version_upload_id_numero_version_key UNIQUE (upload_id, numero_version);


--
-- Name: analitica_portafolio_snapshot analitica_portafolio_snapshot_corte_calculo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.analitica_portafolio_snapshot
    ADD CONSTRAINT analitica_portafolio_snapshot_corte_calculo_key UNIQUE (corte_calculo);


--
-- Name: analitica_portafolio_snapshot analitica_portafolio_snapshot_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.analitica_portafolio_snapshot
    ADD CONSTRAINT analitica_portafolio_snapshot_pkey PRIMARY KEY (snapshot_id);


--
-- Name: auditoria_ponderaciones auditoria_ponderaciones_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.auditoria_ponderaciones
    ADD CONSTRAINT auditoria_ponderaciones_pkey PRIMARY KEY (auditoria_id);


--
-- Name: documento_auditoria documento_auditoria_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_auditoria
    ADD CONSTRAINT documento_auditoria_pkey PRIMARY KEY (documento_auditoria_id);


--
-- Name: documento_dinamico documento_dinamico_nombre_almacenado_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_dinamico
    ADD CONSTRAINT documento_dinamico_nombre_almacenado_key UNIQUE (nombre_almacenado);


--
-- Name: documento_dinamico documento_dinamico_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_dinamico
    ADD CONSTRAINT documento_dinamico_pkey PRIMARY KEY (id);


--
-- Name: documento_interno documento_interno_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_interno
    ADD CONSTRAINT documento_interno_codigo_key UNIQUE (codigo);


--
-- Name: documento_interno documento_interno_nombre_almacenado_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_interno
    ADD CONSTRAINT documento_interno_nombre_almacenado_key UNIQUE (nombre_almacenado);


--
-- Name: documento_interno documento_interno_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_interno
    ADD CONSTRAINT documento_interno_pkey PRIMARY KEY (id);

COMMENT ON COLUMN proyecta_db.documento_interno.creado_por IS 'FK a usuarios.id del autor del documento';
COMMENT ON COLUMN proyecta_db.documento_interno.creado_por_nombre IS 'Nombre visible del autor al momento de crear el documento';


--
-- Name: documento documento_nombre_almacenado_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento
    ADD CONSTRAINT documento_nombre_almacenado_key UNIQUE (nombre_almacenado);


--
-- Name: documento_observacion documento_observacion_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_observacion
    ADD CONSTRAINT documento_observacion_pkey PRIMARY KEY (documento_observacion_id);


--
-- Name: documento documento_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento
    ADD CONSTRAINT documento_pkey PRIMARY KEY (id);


--
-- Name: documento_pre_wizard_revision documento_pre_wizard_revision_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_pre_wizard_revision
    ADD CONSTRAINT documento_pre_wizard_revision_pkey PRIMARY KEY (documento_pre_wizard_revision_id);


--
-- Name: documento_proyecto_version documento_proyecto_version_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_proyecto_version
    ADD CONSTRAINT documento_proyecto_version_pkey PRIMARY KEY (documento_proyecto_version_id);


--
-- Name: documento_proyecto_version documento_proyecto_version_proyecto_id_tipo_documento_numer_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_proyecto_version
    ADD CONSTRAINT documento_proyecto_version_proyecto_id_tipo_documento_numer_key UNIQUE (proyecto_id, tipo_documento, numero_version);


--
-- Name: documento_version documento_version_entregable_id_numero_version_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_version
    ADD CONSTRAINT documento_version_entregable_id_numero_version_key UNIQUE (entregable_id, numero_version);


--
-- Name: documento_version documento_version_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_version
    ADD CONSTRAINT documento_version_pkey PRIMARY KEY (documento_version_id);


--
-- Name: entregable_cambio_descripcion entregable_cambio_descripcion_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_descripcion
    ADD CONSTRAINT entregable_cambio_descripcion_pkey PRIMARY KEY (id);


--
-- Name: entregable_cambio_fecha entregable_cambio_fecha_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_fecha
    ADD CONSTRAINT entregable_cambio_fecha_pkey PRIMARY KEY (id);


--
-- Name: entregable entregable_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable
    ADD CONSTRAINT entregable_pkey PRIMARY KEY (entregable_id);


--
-- Name: estado_entregable_config estado_entregable_config_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_entregable_config
    ADD CONSTRAINT estado_entregable_config_codigo_key UNIQUE (codigo);


--
-- Name: estado_entregable_config estado_entregable_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_entregable_config
    ADD CONSTRAINT estado_entregable_config_pkey PRIMARY KEY (estado_entregable_id);


--
-- Name: estado_proyecto_config estado_proyecto_config_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_proyecto_config
    ADD CONSTRAINT estado_proyecto_config_codigo_key UNIQUE (codigo);


--
-- Name: estado_proyecto_config estado_proyecto_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estado_proyecto_config
    ADD CONSTRAINT estado_proyecto_config_pkey PRIMARY KEY (estado_proyecto_id);


--
-- Name: estrategia_peti_config estrategia_peti_config_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estrategia_peti_config
    ADD CONSTRAINT estrategia_peti_config_codigo_key UNIQUE (codigo);


--
-- Name: estrategia_peti_config estrategia_peti_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.estrategia_peti_config
    ADD CONSTRAINT estrategia_peti_config_pkey PRIMARY KEY (estrategia_peti_id);


--
-- Name: fase fase_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.fase
    ADD CONSTRAINT fase_pkey PRIMARY KEY (fase_id);


--
-- Name: hito hito_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.hito
    ADD CONSTRAINT hito_pkey PRIMARY KEY (hito_id);


--
-- Name: lista_parametrica_config lista_parametrica_config_lista_clave_item_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.lista_parametrica_config
    ADD CONSTRAINT lista_parametrica_config_lista_clave_item_codigo_key UNIQUE (lista_clave, item_codigo);


--
-- Name: lista_parametrica_config lista_parametrica_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.lista_parametrica_config
    ADD CONSTRAINT lista_parametrica_config_pkey PRIMARY KEY (id);


--
-- Name: matriz_riesgo matriz_riesgo_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.matriz_riesgo
    ADD CONSTRAINT matriz_riesgo_pkey PRIMARY KEY (matriz_riesgo_id);


--
-- Name: matriz_riesgo matriz_riesgo_probabilidad_impacto_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.matriz_riesgo
    ADD CONSTRAINT matriz_riesgo_probabilidad_impacto_key UNIQUE (probabilidad, impacto);


--
-- Name: notification_audit notification_audit_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_audit
    ADD CONSTRAINT notification_audit_pkey PRIMARY KEY (id);


--
-- Name: notification_event_catalog notification_event_catalog_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_event_catalog
    ADD CONSTRAINT notification_event_catalog_pkey PRIMARY KEY (code);


--
-- Name: notification_in_app notification_in_app_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_in_app
    ADD CONSTRAINT notification_in_app_pkey PRIMARY KEY (id);


--
-- Name: notification_log notification_log_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_log
    ADD CONSTRAINT notification_log_pkey PRIMARY KEY (id);


--
-- Name: notification_log notification_log_project_id_notification_date_notification__key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_log
    ADD CONSTRAINT notification_log_project_id_notification_date_notification__key UNIQUE (proyecto_id, notification_date, notification_type);


--
-- Name: notification_mail_dispatch_log notification_mail_dispatch_log_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_mail_dispatch_log
    ADD CONSTRAINT notification_mail_dispatch_log_pkey PRIMARY KEY (id);


--
-- Name: notification_preference notification_preference_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference
    ADD CONSTRAINT notification_preference_pkey PRIMARY KEY (id);


--
-- Name: notification_preference notification_preference_user_id_event_code_project_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference
    ADD CONSTRAINT notification_preference_user_id_event_code_project_id_key UNIQUE (user_id, event_code, proyecto_id);


--
-- Name: notification_template notification_template_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_template
    ADD CONSTRAINT notification_template_pkey PRIMARY KEY (event_code);


--
-- Name: objetivos_especificos objetivos_especificos_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.objetivos_especificos
    ADD CONSTRAINT objetivos_especificos_pkey PRIMARY KEY (obj_id);


--
-- Name: patrocinador patrocinador_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.patrocinador
    ADD CONSTRAINT patrocinador_pkey PRIMARY KEY (patrocinador_id);


--
-- Name: permisos permisos_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.permisos
    ADD CONSTRAINT permisos_codigo_key UNIQUE (codigo);


--
-- Name: permisos permisos_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.permisos
    ADD CONSTRAINT permisos_pkey PRIMARY KEY (id);


--
-- Name: project_closure_answer project_closure_answer_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_answer
    ADD CONSTRAINT project_closure_answer_pkey PRIMARY KEY (id);


--
-- Name: project_closure_answer project_closure_answer_proyecto_id_question_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_answer
    ADD CONSTRAINT project_closure_answer_proyecto_id_question_id_key UNIQUE (proyecto_id, question_id);


--
-- Name: project_closure_question project_closure_question_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_question
    ADD CONSTRAINT project_closure_question_pkey PRIMARY KEY (id);


--
-- Name: project_closure_template project_closure_template_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_template
    ADD CONSTRAINT project_closure_template_pkey PRIMARY KEY (id);


--
-- Name: project_closures project_closures_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closures
    ADD CONSTRAINT project_closures_pkey PRIMARY KEY (id);


--
-- Name: project_closures project_closures_proyecto_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closures
    ADD CONSTRAINT project_closures_proyecto_id_key UNIQUE (proyecto_id);


--
-- Name: proyecto_beneficio_impacto proyecto_beneficio_impacto_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_beneficio_impacto
    ADD CONSTRAINT proyecto_beneficio_impacto_pkey PRIMARY KEY (id);


--
-- Name: proyecto_beneficio_impacto proyecto_beneficio_impacto_proyecto_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_beneficio_impacto
    ADD CONSTRAINT proyecto_beneficio_impacto_proyecto_id_key UNIQUE (proyecto_id);


--
-- Name: proyecto_equipo proyecto_equipo_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_equipo
    ADD CONSTRAINT proyecto_equipo_pkey PRIMARY KEY (id);


--
-- Name: proyecto proyecto_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT proyecto_pkey PRIMARY KEY (proyecto_id);


--
-- Name: proyecto_stakeholder proyecto_stakeholder_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_stakeholder
    ADD CONSTRAINT proyecto_stakeholder_pkey PRIMARY KEY (id);


--
-- Name: public_evidence_access public_evidence_access_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.public_evidence_access
    ADD CONSTRAINT public_evidence_access_pkey PRIMARY KEY (id);


--
-- Name: public_evidence_access public_evidence_access_token_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.public_evidence_access
    ADD CONSTRAINT public_evidence_access_token_key UNIQUE (token);


--
-- Name: reporte_config reporte_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.reporte_config
    ADD CONSTRAINT reporte_config_pkey PRIMARY KEY (id);


--
-- Name: respuestas_furag respuestas_furag_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.respuestas_furag
    ADD CONSTRAINT respuestas_furag_pkey PRIMARY KEY (respuesta_furag_id);


--
-- Name: respuestas_furag respuestas_furag_proyecto_id_codigo_pregunta_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.respuestas_furag
    ADD CONSTRAINT respuestas_furag_proyecto_id_codigo_pregunta_key UNIQUE (proyecto_id, codigo_pregunta);


--
-- Name: riesgo_solucion_adjunto riesgo_solucion_adjunto_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_solucion_adjunto
    ADD CONSTRAINT riesgo_solucion_adjunto_pkey PRIMARY KEY (riesgo_solucion_adjunto_id);


--
-- Name: riesgo_tratamiento_adjunto riesgo_tratamiento_adjunto_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento_adjunto
    ADD CONSTRAINT riesgo_tratamiento_adjunto_pkey PRIMARY KEY (riesgo_tratamiento_adjunto_id);


--
-- Name: riesgo_tratamiento riesgo_tratamiento_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento
    ADD CONSTRAINT riesgo_tratamiento_pkey PRIMARY KEY (riesgo_tratamiento_id);


--
-- Name: riesgo_tratamiento riesgo_tratamiento_riesgo_id_iteracion_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento
    ADD CONSTRAINT riesgo_tratamiento_riesgo_id_iteracion_key UNIQUE (riesgo_id, iteracion);


--
-- Name: riesgos riesgos_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos
    ADD CONSTRAINT riesgos_pkey PRIMARY KEY (riesgo_id);


--
-- Name: rol_permiso rol_permiso_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.rol_permiso
    ADD CONSTRAINT rol_permiso_pkey PRIMARY KEY (id);


--
-- Name: rol_permiso rol_permiso_rol_id_permiso_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.rol_permiso
    ADD CONSTRAINT rol_permiso_rol_id_permiso_id_key UNIQUE (rol_id, permiso_id);


--
-- Name: roles roles_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.roles
    ADD CONSTRAINT roles_codigo_key UNIQUE (codigo);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: system_audit_log system_audit_log_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.system_audit_log
    ADD CONSTRAINT system_audit_log_pkey PRIMARY KEY (id, creado_en);


--
-- Name: system_parameters system_parameters_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.system_parameters
    ADD CONSTRAINT system_parameters_pkey PRIMARY KEY (param_key);


--
-- Name: tipo_documento_config tipo_documento_config_codigo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.tipo_documento_config
    ADD CONSTRAINT tipo_documento_config_codigo_key UNIQUE (codigo);


--
-- Name: tipo_documento_config tipo_documento_config_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.tipo_documento_config
    ADD CONSTRAINT tipo_documento_config_pkey PRIMARY KEY (tipo_documento_id);


--
-- Name: documento_dinamico uq_documento_dinamico_proyecto_tipo; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_dinamico
    ADD CONSTRAINT uq_documento_dinamico_proyecto_tipo UNIQUE (proyecto_id, tipo_documento);


--
-- Name: documento uq_documento_proyecto_tipo; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento
    ADD CONSTRAINT uq_documento_proyecto_tipo UNIQUE (proyecto_id, tipo_documento_config_id);


--
-- Name: documento_pre_wizard_revision uq_pre_wizard_revision; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_pre_wizard_revision
    ADD CONSTRAINT uq_pre_wizard_revision UNIQUE (proyecto_id, tipo_documento);


--
-- Name: proyecto uq_proyecto_nombre_dependencia; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT uq_proyecto_nombre_dependencia UNIQUE (nombre, dependencia);


--
-- Name: riesgos uq_riesgos_codigo; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos
    ADD CONSTRAINT uq_riesgos_codigo UNIQUE (codigo);


--
-- Name: usuario_permiso usuario_permiso_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_permiso
    ADD CONSTRAINT usuario_permiso_pkey PRIMARY KEY (id);


--
-- Name: usuario_permiso usuario_permiso_usuario_id_permiso_id_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_permiso
    ADD CONSTRAINT usuario_permiso_usuario_id_permiso_id_key UNIQUE (usuario_id, permiso_id);


--
-- Name: usuario_proyecto usuario_proyecto_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_proyecto
    ADD CONSTRAINT usuario_proyecto_pkey PRIMARY KEY (id);


--
-- Name: usuario_proyecto usuario_proyecto_usuario_id_proyecto_id_cargo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_proyecto
    ADD CONSTRAINT usuario_proyecto_usuario_id_proyecto_id_cargo_key UNIQUE (usuario_id, proyecto_id, cargo);


--
-- Name: usuarios usuarios_correo_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuarios
    ADD CONSTRAINT usuarios_correo_key UNIQUE (correo);


--
-- Name: usuarios usuarios_keycloak_sub_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuarios
    ADD CONSTRAINT usuarios_keycloak_sub_key UNIQUE (keycloak_sub);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (username);


--
-- Name: idx_adv_version_project_periodo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_adv_version_project_periodo ON proyecta_db.advance_report_version USING btree (proyecto_id, periodo);


--
-- Name: idx_adv_version_upload; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_adv_version_upload ON proyecta_db.advance_report_version USING btree (upload_id);


--
-- Name: idx_analitica_snapshot_fecha; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_analitica_snapshot_fecha ON proyecta_db.analitica_portafolio_snapshot USING btree (fecha_generacion DESC);


--
-- Name: idx_audit_log_accion; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_audit_log_accion ON proyecta_db.system_audit_log USING btree (accion);


--
-- Name: idx_audit_log_entidad; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_audit_log_entidad ON proyecta_db.system_audit_log USING btree (entidad_tipo, entidad_id);


--
-- Name: idx_audit_log_fecha; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_audit_log_fecha ON proyecta_db.system_audit_log USING btree (creado_en DESC);


--
-- Name: idx_audit_log_modulo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_audit_log_modulo ON proyecta_db.system_audit_log USING btree (modulo);


--
-- Name: idx_audit_log_usuario; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_audit_log_usuario ON proyecta_db.system_audit_log USING btree (usuario_id);


--
-- Name: idx_auditoria_ponderaciones_fase; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_auditoria_ponderaciones_fase ON proyecta_db.auditoria_ponderaciones USING btree (fase_id);


--
-- Name: idx_auditoria_ponderaciones_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_auditoria_ponderaciones_proyecto ON proyecta_db.auditoria_ponderaciones USING btree (proyecto_id);


--
-- Name: idx_cambio_descripcion_entregable; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_cambio_descripcion_entregable ON proyecta_db.entregable_cambio_descripcion USING btree (entregable_id);


--
-- Name: idx_cambio_fecha_entregable; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_cambio_fecha_entregable ON proyecta_db.entregable_cambio_fecha USING btree (entregable_id);


--
-- Name: idx_closure_answer_pregunta; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_closure_answer_pregunta ON proyecta_db.project_closure_answer USING btree (question_id);


--
-- Name: idx_closures_template; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_closures_template ON proyecta_db.project_closures USING btree (template_id);


--
-- Name: idx_documento_auditoria_accion; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_auditoria_accion ON proyecta_db.documento_auditoria USING btree (accion);


--
-- Name: idx_documento_auditoria_entregable; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_auditoria_entregable ON proyecta_db.documento_auditoria USING btree (entregable_id);


--
-- Name: idx_documento_auditoria_observacion; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_auditoria_observacion ON proyecta_db.documento_auditoria USING btree (documento_observacion_id);


--
-- Name: idx_documento_auditoria_version; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_auditoria_version ON proyecta_db.documento_auditoria USING btree (documento_version_id);


--
-- Name: idx_documento_interno_fecha; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_interno_fecha ON proyecta_db.documento_interno USING btree (fecha_creacion);


--
-- Name: idx_documento_interno_nombre; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_interno_nombre ON proyecta_db.documento_interno USING btree (nombre);


--
-- Name: idx_documento_observacion_entregable; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_observacion_entregable ON proyecta_db.documento_observacion USING btree (entregable_id);


--
-- Name: idx_documento_observacion_estado; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_observacion_estado ON proyecta_db.documento_observacion USING btree (estado);


--
-- Name: idx_documento_observacion_version; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_observacion_version ON proyecta_db.documento_observacion USING btree (documento_version_id);


--
-- Name: idx_documento_tipo_documento_config; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_tipo_documento_config ON proyecta_db.documento USING btree (tipo_documento_config_id);


--
-- Name: idx_documento_version_estado; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_version_estado ON proyecta_db.documento_proyecto_version USING btree (estado);


--
-- Name: idx_documento_version_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_version_proyecto ON proyecta_db.documento_proyecto_version USING btree (proyecto_id);


--
-- Name: idx_documento_version_tipo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_documento_version_tipo ON proyecta_db.documento_proyecto_version USING btree (tipo_documento);


--
-- Name: idx_entregable_estado; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_entregable_estado ON proyecta_db.entregable USING btree (estado_config_id);


--
-- Name: idx_entregable_hito; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_entregable_hito ON proyecta_db.entregable USING btree (hito_id);


--
-- Name: idx_fase_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_fase_proyecto ON proyecta_db.fase USING btree (proyecto_id);


--
-- Name: idx_hito_fase; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_hito_fase ON proyecta_db.hito USING btree (fase_id);


--
-- Name: idx_matriz_riesgo_nivel; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_matriz_riesgo_nivel ON proyecta_db.matriz_riesgo USING btree (nivel_riesgo);


--
-- Name: idx_notification_audit_created; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_audit_created ON proyecta_db.notification_audit USING btree (creado_en);


--
-- Name: idx_notification_audit_event; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_audit_event ON proyecta_db.notification_audit USING btree (event_code);


--
-- Name: idx_notification_audit_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_audit_proyecto ON proyecta_db.notification_audit USING btree (proyecto_id);


--
-- Name: idx_notification_audit_recipient; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_audit_recipient ON proyecta_db.notification_audit USING btree (recipient);


--
-- Name: idx_notification_audit_status; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_audit_status ON proyecta_db.notification_audit USING btree (status);


--
-- Name: idx_notification_in_app_evento; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_in_app_evento ON proyecta_db.notification_in_app USING btree (event_code);


--
-- Name: idx_notification_in_app_recipient; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_in_app_recipient ON proyecta_db.notification_in_app USING btree (recipient_user_id, creado_en DESC);


--
-- Name: idx_notification_in_app_unread; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_in_app_unread ON proyecta_db.notification_in_app USING btree (recipient_user_id, read_status);


--
-- Name: idx_notification_preference_evento; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_preference_evento ON proyecta_db.notification_preference USING btree (event_code);


--
-- Name: idx_notification_preference_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_preference_proyecto ON proyecta_db.notification_preference USING btree (proyecto_id);


--
-- Name: idx_notification_preference_user; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_notification_preference_user ON proyecta_db.notification_preference USING btree (user_id);


--
-- Name: idx_objetivos_especificos_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_objetivos_especificos_proyecto ON proyecta_db.objetivos_especificos USING btree (proyecto_id);


--
-- Name: idx_pre_wizard_revision_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_pre_wizard_revision_proyecto ON proyecta_db.documento_pre_wizard_revision USING btree (proyecto_id);


--
-- Name: idx_proyecto_director_usuario; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_director_usuario ON proyecta_db.proyecto USING btree (director_usuario_id);


--
-- Name: idx_proyecto_equipo_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_equipo_proyecto ON proyecta_db.proyecto_equipo USING btree (proyecto_id);


--
-- Name: idx_proyecto_estado; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_estado ON proyecta_db.proyecto USING btree (estado_config_id);


--
-- Name: idx_proyecto_estrategia_peti; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_estrategia_peti ON proyecta_db.proyecto USING btree (estrategia_peti_config_id);


--
-- Name: idx_proyecto_patrocinador; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_patrocinador ON proyecta_db.proyecto USING btree (patrocinador_id);


--
-- Name: idx_proyecto_stakeholder_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_stakeholder_proyecto ON proyecta_db.proyecto_stakeholder USING btree (proyecto_id);


--
-- Name: idx_proyecto_tipo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_proyecto_tipo ON proyecta_db.documento_dinamico USING btree (proyecto_id, tipo_documento);


--
-- Name: idx_public_evidence_entregable; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_public_evidence_entregable ON proyecta_db.public_evidence_access USING btree (entregable_id);


--
-- Name: idx_riesgo_solucion_riesgo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_riesgo_solucion_riesgo ON proyecta_db.riesgo_solucion_adjunto USING btree (riesgo_id);


--
-- Name: idx_riesgo_tratamiento_adjunto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_riesgo_tratamiento_adjunto ON proyecta_db.riesgo_tratamiento_adjunto USING btree (riesgo_tratamiento_id);


--
-- Name: idx_riesgo_tratamiento_riesgo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_riesgo_tratamiento_riesgo ON proyecta_db.riesgo_tratamiento USING btree (riesgo_id);


--
-- Name: idx_riesgos_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_riesgos_proyecto ON proyecta_db.riesgos USING btree (proyecto_id);


--
-- Name: idx_rol_permiso_permiso; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_rol_permiso_permiso ON proyecta_db.rol_permiso USING btree (permiso_id);


--
-- Name: idx_usuario_permiso_permiso; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_usuario_permiso_permiso ON proyecta_db.usuario_permiso USING btree (permiso_id);


--
-- Name: idx_usuario_proyecto_proyecto; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE INDEX idx_usuario_proyecto_proyecto ON proyecta_db.usuario_proyecto USING btree (proyecto_id);


--
-- Name: uk_closure_template_active; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE UNIQUE INDEX uk_closure_template_active ON proyecta_db.project_closure_template USING btree (activo) WHERE (activo = true);


--
-- Name: uk_usuario_proyecto_director_activo; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE UNIQUE INDEX uk_usuario_proyecto_director_activo ON proyecta_db.usuario_proyecto USING btree (proyecto_id) WHERE ((upper((cargo)::text) = 'DIRECTOR_PROYECTO'::text) AND (activo = true));


--
-- Name: uq_notification_preference_global; Type: INDEX; Schema: proyecta_db; Owner: -
--

CREATE UNIQUE INDEX uq_notification_preference_global ON proyecta_db.notification_preference USING btree (user_id, event_code) WHERE (proyecto_id IS NULL);


--
-- Name: fase tr_auditar_cambios_ponderacion; Type: TRIGGER; Schema: proyecta_db; Owner: -
--

CREATE TRIGGER tr_auditar_cambios_ponderacion AFTER UPDATE ON proyecta_db.fase FOR EACH ROW EXECUTE FUNCTION proyecta_db.fn_auditar_cambios_ponderacion();


--
-- Name: fase tr_validar_ponderacion_fase_insert; Type: TRIGGER; Schema: proyecta_db; Owner: -
--

CREATE TRIGGER tr_validar_ponderacion_fase_insert BEFORE INSERT ON proyecta_db.fase FOR EACH ROW EXECUTE FUNCTION proyecta_db.fn_validar_ponderacion_fase();


--
-- Name: fase tr_validar_ponderacion_fase_update; Type: TRIGGER; Schema: proyecta_db; Owner: -
--

CREATE TRIGGER tr_validar_ponderacion_fase_update BEFORE UPDATE ON proyecta_db.fase FOR EACH ROW WHEN ((old.ponderacion IS DISTINCT FROM new.ponderacion)) EXECUTE FUNCTION proyecta_db.fn_validar_ponderacion_fase();


--
-- Name: actas_cierre actas_cierre_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.actas_cierre
    ADD CONSTRAINT actas_cierre_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: advance_report_version advance_report_version_upload_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_version
    ADD CONSTRAINT advance_report_version_upload_id_fkey FOREIGN KEY (upload_id) REFERENCES proyecta_db.advance_report_uploads(id) ON DELETE CASCADE;


--
-- Name: documento_auditoria documento_auditoria_documento_observacion_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_auditoria
    ADD CONSTRAINT documento_auditoria_documento_observacion_id_fkey FOREIGN KEY (documento_observacion_id) REFERENCES proyecta_db.documento_observacion(documento_observacion_id) ON DELETE SET NULL;


--
-- Name: documento_auditoria documento_auditoria_documento_version_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_auditoria
    ADD CONSTRAINT documento_auditoria_documento_version_id_fkey FOREIGN KEY (documento_version_id) REFERENCES proyecta_db.documento_version(documento_version_id) ON DELETE SET NULL;


--
-- Name: documento_auditoria documento_auditoria_entregable_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_auditoria
    ADD CONSTRAINT documento_auditoria_entregable_id_fkey FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: documento_observacion documento_observacion_documento_version_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_observacion
    ADD CONSTRAINT documento_observacion_documento_version_id_fkey FOREIGN KEY (documento_version_id) REFERENCES proyecta_db.documento_version(documento_version_id) ON DELETE SET NULL;


--
-- Name: documento_observacion documento_observacion_entregable_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_observacion
    ADD CONSTRAINT documento_observacion_entregable_id_fkey FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: documento_proyecto_version documento_proyecto_version_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_proyecto_version
    ADD CONSTRAINT documento_proyecto_version_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: documento_version documento_version_entregable_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_version
    ADD CONSTRAINT documento_version_entregable_id_fkey FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: entregable_cambio_descripcion entregable_cambio_descripcion_entregable_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_descripcion
    ADD CONSTRAINT entregable_cambio_descripcion_entregable_id_fkey FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: entregable entregable_hito_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable
    ADD CONSTRAINT entregable_hito_id_fkey FOREIGN KEY (hito_id) REFERENCES proyecta_db.hito(hito_id) ON DELETE CASCADE;


--
-- Name: fase fase_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.fase
    ADD CONSTRAINT fase_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: advance_report_uploads fk_adv_uploads_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_uploads
    ADD CONSTRAINT fk_adv_uploads_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: advance_report_version fk_adv_version_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.advance_report_version
    ADD CONSTRAINT fk_adv_version_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: auditoria_ponderaciones fk_auditoria_ponderaciones_fase; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.auditoria_ponderaciones
    ADD CONSTRAINT fk_auditoria_ponderaciones_fase FOREIGN KEY (fase_id) REFERENCES proyecta_db.fase(fase_id) ON DELETE SET NULL;


--
-- Name: auditoria_ponderaciones fk_auditoria_ponderaciones_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.auditoria_ponderaciones
    ADD CONSTRAINT fk_auditoria_ponderaciones_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE SET NULL;


--
-- Name: entregable_cambio_fecha fk_cambio_fecha_entregable; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable_cambio_fecha
    ADD CONSTRAINT fk_cambio_fecha_entregable FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: project_closure_answer fk_closure_answer_pregunta; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_answer
    ADD CONSTRAINT fk_closure_answer_pregunta FOREIGN KEY (question_id) REFERENCES proyecta_db.project_closure_question(id) ON DELETE CASCADE;


--
-- Name: project_closure_answer fk_closure_answer_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closure_answer
    ADD CONSTRAINT fk_closure_answer_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: project_closures fk_closures_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closures
    ADD CONSTRAINT fk_closures_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: project_closures fk_closures_template; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.project_closures
    ADD CONSTRAINT fk_closures_template FOREIGN KEY (template_id) REFERENCES proyecta_db.project_closure_template(id) ON DELETE RESTRICT;


--
-- Name: documento_dinamico fk_documento_dinamico_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_dinamico
    ADD CONSTRAINT fk_documento_dinamico_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: documento fk_documento_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento
    ADD CONSTRAINT fk_documento_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: documento fk_documento_tipo_config; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento
    ADD CONSTRAINT fk_documento_tipo_config FOREIGN KEY (tipo_documento_config_id) REFERENCES proyecta_db.tipo_documento_config(tipo_documento_id) ON DELETE RESTRICT;


--
-- Name: entregable fk_entregable_estado_config; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.entregable
    ADD CONSTRAINT fk_entregable_estado_config FOREIGN KEY (estado_config_id) REFERENCES proyecta_db.estado_entregable_config(estado_entregable_id) ON DELETE RESTRICT;


--
-- Name: notification_audit fk_notification_audit_evento; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_audit
    ADD CONSTRAINT fk_notification_audit_evento FOREIGN KEY (event_code) REFERENCES proyecta_db.notification_event_catalog(code) ON DELETE CASCADE;


--
-- Name: notification_audit fk_notification_audit_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_audit
    ADD CONSTRAINT fk_notification_audit_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE SET NULL;


--
-- Name: notification_in_app fk_notification_in_app_evento; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_in_app
    ADD CONSTRAINT fk_notification_in_app_evento FOREIGN KEY (event_code) REFERENCES proyecta_db.notification_event_catalog(code) ON DELETE CASCADE;


--
-- Name: notification_in_app fk_notification_in_app_usuario; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_in_app
    ADD CONSTRAINT fk_notification_in_app_usuario FOREIGN KEY (recipient_user_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE;


--
-- Name: notification_log fk_notification_log_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_log
    ADD CONSTRAINT fk_notification_log_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: notification_preference fk_notification_preference_evento; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference
    ADD CONSTRAINT fk_notification_preference_evento FOREIGN KEY (event_code) REFERENCES proyecta_db.notification_event_catalog(code) ON DELETE CASCADE;


--
-- Name: notification_preference fk_notification_preference_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference
    ADD CONSTRAINT fk_notification_preference_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: notification_preference fk_notification_preference_usuario; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_preference
    ADD CONSTRAINT fk_notification_preference_usuario FOREIGN KEY (user_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE;


--
-- Name: notification_template fk_notification_template_evento; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.notification_template
    ADD CONSTRAINT fk_notification_template_evento FOREIGN KEY (event_code) REFERENCES proyecta_db.notification_event_catalog(code) ON DELETE CASCADE;


--
-- Name: documento_pre_wizard_revision fk_pre_wizard_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_pre_wizard_revision
    ADD CONSTRAINT fk_pre_wizard_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: proyecto fk_proyecto_estado_config; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT fk_proyecto_estado_config FOREIGN KEY (estado_config_id) REFERENCES proyecta_db.estado_proyecto_config(estado_proyecto_id) ON DELETE RESTRICT;


--
-- Name: proyecto fk_proyecto_estrategia_peti; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT fk_proyecto_estrategia_peti FOREIGN KEY (estrategia_peti_config_id) REFERENCES proyecta_db.estrategia_peti_config(estrategia_peti_id) ON DELETE SET NULL;


--
-- Name: riesgos fk_riesgos_matriz_probabilidad_impacto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos
    ADD CONSTRAINT fk_riesgos_matriz_probabilidad_impacto FOREIGN KEY (probabilidad, impacto) REFERENCES proyecta_db.matriz_riesgo(probabilidad, impacto);


--
-- Name: riesgos fk_riesgos_matriz_residual; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos
    ADD CONSTRAINT fk_riesgos_matriz_residual FOREIGN KEY (probabilidad_residual, impacto_residual) REFERENCES proyecta_db.matriz_riesgo(probabilidad, impacto);


--
-- Name: usuario_proyecto fk_usuario_proyecto_proyecto; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_proyecto
    ADD CONSTRAINT fk_usuario_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: hito hito_fase_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.hito
    ADD CONSTRAINT hito_fase_id_fkey FOREIGN KEY (fase_id) REFERENCES proyecta_db.fase(fase_id) ON DELETE CASCADE;


--
-- Name: objetivos_especificos objetivos_especificos_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.objetivos_especificos
    ADD CONSTRAINT objetivos_especificos_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: proyecto_beneficio_impacto proyecto_beneficio_impacto_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_beneficio_impacto
    ADD CONSTRAINT proyecto_beneficio_impacto_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: proyecto proyecto_director_usuario_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT proyecto_director_usuario_id_fkey FOREIGN KEY (director_usuario_id) REFERENCES proyecta_db.usuarios(id) ON DELETE SET NULL;


--
-- Name: proyecto_equipo proyecto_equipo_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_equipo
    ADD CONSTRAINT proyecto_equipo_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: proyecto proyecto_patrocinador_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto
    ADD CONSTRAINT proyecto_patrocinador_id_fkey FOREIGN KEY (patrocinador_id) REFERENCES proyecta_db.patrocinador(patrocinador_id) ON DELETE SET NULL;


--
-- Name: proyecto_stakeholder proyecto_stakeholder_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.proyecto_stakeholder
    ADD CONSTRAINT proyecto_stakeholder_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: public_evidence_access public_evidence_access_entregable_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.public_evidence_access
    ADD CONSTRAINT public_evidence_access_entregable_id_fkey FOREIGN KEY (entregable_id) REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE;


--
-- Name: respuestas_furag respuestas_furag_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.respuestas_furag
    ADD CONSTRAINT respuestas_furag_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: riesgo_solucion_adjunto riesgo_solucion_adjunto_riesgo_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_solucion_adjunto
    ADD CONSTRAINT riesgo_solucion_adjunto_riesgo_id_fkey FOREIGN KEY (riesgo_id) REFERENCES proyecta_db.riesgos(riesgo_id) ON DELETE CASCADE;


--
-- Name: riesgo_tratamiento_adjunto riesgo_tratamiento_adjunto_riesgo_tratamiento_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento_adjunto
    ADD CONSTRAINT riesgo_tratamiento_adjunto_riesgo_tratamiento_id_fkey FOREIGN KEY (riesgo_tratamiento_id) REFERENCES proyecta_db.riesgo_tratamiento(riesgo_tratamiento_id) ON DELETE CASCADE;


--
-- Name: riesgo_tratamiento riesgo_tratamiento_riesgo_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgo_tratamiento
    ADD CONSTRAINT riesgo_tratamiento_riesgo_id_fkey FOREIGN KEY (riesgo_id) REFERENCES proyecta_db.riesgos(riesgo_id) ON DELETE CASCADE;


--
-- Name: riesgos riesgos_proyecto_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.riesgos
    ADD CONSTRAINT riesgos_proyecto_id_fkey FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE;


--
-- Name: rol_permiso rol_permiso_permiso_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.rol_permiso
    ADD CONSTRAINT rol_permiso_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES proyecta_db.permisos(id) ON DELETE CASCADE;


--
-- Name: rol_permiso rol_permiso_rol_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.rol_permiso
    ADD CONSTRAINT rol_permiso_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES proyecta_db.roles(id) ON DELETE CASCADE;


--
-- Name: usuario_permiso usuario_permiso_permiso_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_permiso
    ADD CONSTRAINT usuario_permiso_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES proyecta_db.permisos(id) ON DELETE CASCADE;


--
-- Name: usuario_permiso usuario_permiso_usuario_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_permiso
    ADD CONSTRAINT usuario_permiso_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE;


--
-- Name: usuario_proyecto usuario_proyecto_usuario_id_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.usuario_proyecto
    ADD CONSTRAINT usuario_proyecto_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE;


--
-- Name: documento_interno documento_interno_creado_por_fkey; Type: FK CONSTRAINT; Schema: proyecta_db; Owner: -
--

ALTER TABLE ONLY proyecta_db.documento_interno
    ADD CONSTRAINT documento_interno_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES proyecta_db.usuarios (id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--


