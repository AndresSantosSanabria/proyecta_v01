import gobLogo from '../../assets/logo gob cun.png';
import stdLogo from '../../assets/STD.png';
import './ReportDocumentPreview.css';

/* ─── helpers ─────────────────────────────────────────────────────────────── */
const fmt = (v) => (v === null || v === undefined || v === '' ? '—' : v);

const toPercent = (v) => {
  const n = Number(v);
  if (v === null || v === undefined || Number.isNaN(n)) return '0.00 %';
  return `${n.toFixed(2)} %`;
};

const fmtDate = (d) => {
  if (!d) return '—';
  try {
    return new Date(d).toLocaleDateString('es-CO', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    });
  } catch { return d; }
};

const todayStr = () =>
  new Date().toLocaleDateString('es-CO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });

/* ─── section header ──────────────────────────────────────────────────────── */
const SectionHead = ({ number, title }) => (
  <div className="rdoc__section-head">
    <span className="rdoc__section-number">{number}.</span>
    <h3 className="rdoc__section-title">{title}</h3>
  </div>
);

/* ─── field grid (label / value pairs) ───────────────────────────────────── */
const FieldGrid = ({ fields }) => (
  <div className="rdoc__field-grid">
    {fields.map(({ label, value, wide, emphasis }) => (
      <div key={label} className={`rdoc__field${wide ? ' rdoc__field--wide' : ''}`}>
        <span className="rdoc__field-label">{label}</span>
        <span className={`rdoc__field-value${emphasis ? ' rdoc__field-value--emphasis' : ''}`}>
          {value}
        </span>
      </div>
    ))}
  </div>
);

/* ─── generic table ──────────────────────────────────────────────────────── */
const DocTable = ({ columns, rows, emptyMsg = 'Sin datos disponibles.' }) => (
  <table className="rdoc__table">
    <thead>
      <tr>
        {columns.map((col) => (
          <th key={col.key} style={col.width ? { width: col.width } : undefined}>
            {col.label}
          </th>
        ))}
      </tr>
    </thead>
    <tbody>
      {rows.length ? rows.map((row, i) => (
        <tr key={i}>
          {columns.map((col) => (
            <td key={col.key} className={col.className}>
              {col.render ? col.render(row, i) : fmt(row[col.key])}
            </td>
          ))}
        </tr>
      )) : (
        <tr>
          <td colSpan={columns.length} className="rdoc__table-empty">
            {emptyMsg}
          </td>
        </tr>
      )}
    </tbody>
  </table>
);

/* ─── status badge ────────────────────────────────────────────────────────── */
const StatusBadge = ({ value }) => {
  const up = String(value || '').toUpperCase();
  const cls = up === 'SI' ? 'rdoc__badge rdoc__badge--yes'
    : up === 'NO' ? 'rdoc__badge rdoc__badge--no'
    : 'rdoc__badge';
  return <span className={cls}>{value}</span>;
};

/* ═══════════════════════════════════════════════════════════════════════════
   REPORT BODIES
   ═══════════════════════════════════════════════════════════════════════════ */

/* 1. ESTADO DE PROYECTO ESPECÍFICO */
const BodyEstadoProyecto = ({ reportData }) => {
  const entregables = Array.isArray(reportData?.entregablesVencidos)
    ? reportData.entregablesVencidos : [];

  return (
    <>
      <SectionHead number="1" title="Información general del proyecto" />
      <FieldGrid fields={[
        { label: 'Código del proyecto',  value: fmt(reportData?.proyectoId) },
        { label: 'Nombre del proyecto',  value: fmt(reportData?.nombre), wide: true },
        { label: 'Director del proyecto', value: fmt(reportData?.directorNombre) },
        { label: 'Dependencia',           value: fmt(reportData?.dependencia) },
        { label: 'Avance total',          value: toPercent(reportData?.avanceTotal), emphasis: true },
        { label: 'Estado',                value: fmt(reportData?.estado) },
        { label: 'Vigencia',              value: fmt(reportData?.vigenciaPeti) },
        { label: 'Fecha de inicio',       value: fmtDate(reportData?.fechaInicio) },
      ]} />

      <SectionHead number="2" title="Entregables vencidos" />
      <DocTable
        columns={[
          { key: '_n',    label: '#',                        width: '48px',  render: (_, i) => i + 1 },
          { key: 'nombre',      label: 'Nombre del entregable' },
          { key: 'faseNombre',  label: 'Fase' },
          { key: 'fechaEntrega', label: 'Fecha de entrega programada', render: (r) => fmtDate(r.fechaEntrega) },
        ]}
        rows={entregables}
        emptyMsg="No hay entregables vencidos registrados para este proyecto."
      />

      {reportData?.observaciones && (
        <>
          <SectionHead number="3" title="Observaciones" />
          <div className="rdoc__obs-box">{reportData.observaciones}</div>
        </>
      )}
    </>
  );
};

/* 2. TODOS LOS PROYECTOS */
const BodyTodosProyectos = ({ reportData }) => {
  const rows = Array.isArray(reportData) ? reportData : [];
  const promedio = rows.length
    ? rows.reduce((s, r) => s + Number(r.avance || 0), 0) / rows.length
    : 0;
  const atrasados = rows.reduce((s, r) => s + Number(r.entregablesAtrasados || 0), 0);

  return (
    <>
      <SectionHead number="1" title="Resumen ejecutivo del portafolio" />
      <FieldGrid fields={[
        { label: 'Total de proyectos',        value: rows.length },
        { label: 'Avance promedio',            value: toPercent(promedio), emphasis: true },
        { label: 'Entregables atrasados',      value: atrasados },
        { label: 'Dependencias involucradas',  value: new Set(rows.map((r) => r.dependencia)).size },
      ]} />

      <SectionHead number="2" title="Listado de proyectos" />
      <DocTable
        columns={[
          { key: 'id',        label: 'Código' },
          { key: 'nombre',    label: 'Nombre del proyecto', wide: true },
          { key: 'dependencia', label: 'Dependencia' },
          { key: 'avance',    label: 'Avance (%)',  render: (r) => toPercent(r.avance), className: 'rdoc__td-emphasis' },
          { key: 'estado',    label: 'Estado' },
        ]}
        rows={rows}
        emptyMsg="No hay proyectos disponibles en el portafolio."
      />
    </>
  );
};

/* 3. RETRASOS EN FECHA DE ENTREGA */
const BodyRetrasos = ({ reportData }) => {
  const rows = Array.isArray(reportData) ? reportData : [];

  if (!rows.length) {
    return (
      <>
        <SectionHead number="1" title="Proyectos con retrasos" />
        <div className="rdoc__obs-box rdoc__obs-box--ok">
          ✓ No se registran proyectos con retrasos en la fecha de entrega.
        </div>
      </>
    );
  }

  return (
    <>
      <SectionHead number="1" title="Resumen de retrasos" />
      <FieldGrid fields={[
        { label: 'Proyectos con retrasos',    value: rows.length },
        { label: 'Entregables atrasados totales', value: rows.reduce((s, r) => s + Number(r.entregablesAtrasados || 0), 0) },
      ]} />

      {rows.map((project, idx) => {
        const deliverables = Array.isArray(project.entregablesVencidos)
          ? project.entregablesVencidos : [];
        return (
          <div key={project.id || idx} className="rdoc__delay-block">
            <SectionHead number={idx + 2} title={`Proyecto: ${fmt(project.nombre)}`} />
            <FieldGrid fields={[
              { label: 'Código',     value: fmt(project.id) },
              { label: 'Avance',     value: toPercent(project.avance), emphasis: true },
              { label: 'Dependencia', value: fmt(project.dependencia) },
              { label: 'Entregables atrasados', value: fmt(project.entregablesAtrasados) },
            ]} />
            {deliverables.length > 0 && (
              <DocTable
                columns={[
                  { key: 'nombre',       label: 'Entregable' },
                  { key: 'fechaEntrega', label: 'Fecha vencida', render: (r) => fmtDate(r.fechaEntrega ?? r.fecha) },
                ]}
                rows={deliverables}
              />
            )}
          </div>
        );
      })}
    </>
  );
};

/* 4. PLAN DE COMUNICACIONES */
const BodyPlanComunicaciones = ({ reportData, selectedProject }) => {
  const tienePlan = reportData?.planPdfUrl ? 'SI' : 'NO';
  return (
    <>
      <SectionHead number="1" title="Información del proyecto" />
      <FieldGrid fields={[
        { label: 'Código del proyecto',  value: fmt(selectedProject?.id) },
        { label: 'Nombre del proyecto',  value: fmt(reportData?.nombre ?? selectedProject?.nombre), wide: true },
        { label: 'Director del proyecto', value: fmt(selectedProject?.director ?? selectedProject?.directorNombre) },
        { label: 'Dependencia',           value: fmt(reportData?.dependencia ?? selectedProject?.dependencia) },
      ]} />

      <SectionHead number="2" title="Estado del plan de comunicaciones" />
      <div className="rdoc__field-grid">
        <div className="rdoc__field rdoc__field--wide">
          <span className="rdoc__field-label">Plan de comunicaciones cargado</span>
          <StatusBadge value={tienePlan} />
        </div>
        {reportData?.planPdfUrl && (
          <div className="rdoc__field rdoc__field--wide">
            <span className="rdoc__field-label">URL del documento</span>
            <span className="rdoc__field-value">{reportData.planPdfUrl}</span>
          </div>
        )}
      </div>
    </>
  );
};

/* 5. FURAG */
const BodyFurag = ({ reportData, selectedProject }) => {
  const responde = reportData?.objetivoGeneral || reportData?.esPeti ? 'SI' : 'NO';
  const respuestas = Array.isArray(reportData?.respuestas) ? reportData.respuestas : [];

  return (
    <>
      <SectionHead number="1" title="Información del proyecto" />
      <FieldGrid fields={[
        { label: 'Código del proyecto',  value: fmt(selectedProject?.id) },
        { label: 'Nombre del proyecto',  value: fmt(reportData?.nombre ?? selectedProject?.nombre), wide: true },
        { label: 'Director del proyecto', value: fmt(selectedProject?.director ?? selectedProject?.directorNombre) },
        { label: 'Dependencia',           value: fmt(selectedProject?.dependencia) },
      ]} />

      <SectionHead number="2" title="Datos PETI y FURAG" />
      <FieldGrid fields={[
        { label: 'Es proyecto PETI',        value: reportData?.esPeti ? 'SI' : 'NO' },
        { label: 'Responde FURAG',          value: responde },
        { label: 'Estrategia PETI',         value: fmt(reportData?.estrategiaPeti), wide: true },
        { label: 'Vigencia PETI',           value: fmt(reportData?.vigenciaPeti) },
        { label: 'Objetivo general',        value: fmt(reportData?.objetivoGeneral), wide: true },
      ]} />

      {respuestas.length > 0 && (
        <>
          <SectionHead number="3" title="Respuestas FURAG" />
          <DocTable
            columns={[
              { key: '_n',       label: '#', width: '48px', render: (r, i) => i + 1 },
              { key: 'pregunta', label: 'Pregunta', wide: true },
              { key: 'respuesta', label: 'Respuesta' },
            ]}
            rows={respuestas}
          />
        </>
      )}
    </>
  );
};

/* 6. VERIFICACIÓN DE RIESGOS */
const BodyRiesgos = ({ reportData, selectedProject }) => {
  const risks = Array.isArray(reportData) ? reportData : [];
  const hasTratamiento = risks.some(
    (r) => r.tratamiento && r.tratamiento.trim() !== '' && r.estado !== 'PENDIENTE',
  );

  const nivelCls = (nivel) => {
    const n = String(nivel || '').toUpperCase();
    if (n === 'BAJO') return 'rdoc__nivel-bajo';
    if (n === 'MODERADO') return 'rdoc__nivel-moderado';
    if (n === 'ALTO') return 'rdoc__nivel-alto';
    if (n === 'EXTREMO' || n === 'CRITICO') return 'rdoc__nivel-extremo';
    return '';
  };

  return (
    <>
      <SectionHead number="1" title="Información del proyecto" />
      <FieldGrid fields={[
        { label: 'Código del proyecto',           value: fmt(selectedProject?.id) },
        { label: 'Nombre del proyecto',           value: fmt(selectedProject?.nombre), wide: true },
        { label: 'Director del proyecto',          value: fmt(selectedProject?.director ?? selectedProject?.directorNombre) },
        { label: 'Dependencia',                    value: fmt(selectedProject?.dependencia) },
        { label: 'Diligenció tratamiento de riesgos', value: hasTratamiento ? 'SI' : 'NO', emphasis: !hasTratamiento },
      ]} />

      <SectionHead number="2" title="Resumen de riesgos" />
      <FieldGrid fields={[
        { label: 'Riesgos totales',    value: risks.length },
        { label: 'Con tratamiento',    value: risks.filter((r) => r.tratamiento?.trim()).length },
        { label: 'Sin tratamiento',    value: risks.filter((r) => !r.tratamiento?.trim()).length },
        { label: 'Riesgos extremos',   value: risks.filter((r) => ['EXTREMO','CRITICO'].includes(String(r.nivel||'').toUpperCase())).length },
      ]} />

      {risks.length > 0 && (
        <>
          <SectionHead number="3" title="Detalle de riesgos" />
          <DocTable
            columns={[
              { key: '_n',          label: '#',       width: '40px',  render: (r, i) => i + 1 },
              { key: 'nombre',      label: 'Riesgo' },
              { key: 'nivel',       label: 'Nivel',   width: '90px',
                render: (r) => <span className={nivelCls(r.nivel)}>{fmt(r.nivel)}</span> },
              { key: 'tratamiento', label: 'Tratamiento' },
              { key: 'estado',      label: 'Estado',  width: '100px' },
            ]}
            rows={risks}
            emptyMsg="No se registran riesgos para este proyecto."
          />
        </>
      )}
    </>
  );
};

/* ═══════════════════════════════════════════════════════════════════════════
   MAIN COMPONENT
   ═══════════════════════════════════════════════════════════════════════════ */

const REPORT_META = {
  ESTADO_PROYECTO: {
    title: 'ESTADO DE PROYECTO ESPECÍFICO',
    code: 'RPT-001',
  },
  TODOS_LOS_PROYECTOS: {
    title: 'ESTADO DE TODOS LOS PROYECTOS',
    code: 'RPT-002',
  },
  RETRASOS_ENTREGA: {
    title: 'PROYECTOS CON RETRASOS EN LA FECHA DE ENTREGA',
    code: 'RPT-003',
  },
  PLAN_COMUNICACIONES: {
    title: 'PLAN DE COMUNICACIONES',
    code: 'RPT-004',
  },
  FURAG: {
    title: 'PREGUNTAS FURAG',
    code: 'RPT-005',
  },
  VERIFICACION_RIESGOS: {
    title: 'VERIFICACIÓN DE TRATAMIENTO A RIESGOS',
    code: 'RPT-006',
  },
};

const ReportDocumentPreview = ({ reportId, reportData, selectedProject }) => {
  const meta = REPORT_META[reportId];
  if (!meta) return null;

  const vigencia = reportData?.vigenciaPeti
    || (Array.isArray(reportData) ? '' : reportData?.vigencia)
    || '2024-2027';

  const renderBody = () => {
    switch (reportId) {
      case 'ESTADO_PROYECTO':
        return <BodyEstadoProyecto reportData={reportData} />;
      case 'TODOS_LOS_PROYECTOS':
        return <BodyTodosProyectos reportData={reportData} />;
      case 'RETRASOS_ENTREGA':
        return <BodyRetrasos reportData={reportData} />;
      case 'PLAN_COMUNICACIONES':
        return <BodyPlanComunicaciones reportData={reportData} selectedProject={selectedProject} />;
      case 'FURAG':
        return <BodyFurag reportData={reportData} selectedProject={selectedProject} />;
      case 'VERIFICACION_RIESGOS':
        return <BodyRiesgos reportData={reportData} selectedProject={selectedProject} />;
      default:
        return null;
    }
  };

  return (
    <div className="rdoc">

      {/* ── HEADER ─────────────────────────────────────────────────────── */}
      <header className="rdoc__header">
        <div className="rdoc__header-brand">
          <img
            src={gobLogo}
            alt="Gobernación de Cundinamarca"
            className="rdoc__header-logo"
          />
          <div className="rdoc__header-org">
            <p className="rdoc__header-supertitle">Gobernación de Cundinamarca</p>
            <p className="rdoc__header-subtitle">
              Sistema Integral de Seguimiento Institucional
            </p>
          </div>
        </div>
        <div className="rdoc__header-stamp">
          <div className="rdoc__stamp-row">
            <span className="rdoc__stamp-label">Código</span>
            <span className="rdoc__stamp-value">{meta.code}</span>
          </div>
          <div className="rdoc__stamp-row">
            <span className="rdoc__stamp-label">Fecha del reporte</span>
            <span className="rdoc__stamp-value">{todayStr()}</span>
          </div>
          <div className="rdoc__stamp-row">
            <span className="rdoc__stamp-label">Vigencia</span>
            <span className="rdoc__stamp-value">{vigencia}</span>
          </div>
          <div className="rdoc__stamp-badge">Documento oficial</div>
        </div>
      </header>

      {/* ── TITLE BAND ─────────────────────────────────────────────────── */}
      <div className="rdoc__title-band">
        <h1 className="rdoc__title">REPORTE: {meta.title}</h1>
      </div>

      {/* ── BODY ───────────────────────────────────────────────────────── */}
      <div className="rdoc__body">
        {renderBody()}
      </div>

      {/* ── DIVIDER ────────────────────────────────────────────────────── */}
      <div className="rdoc__divider" />

      {/* ── SIGNATURE STRIP ────────────────────────────────────────────── */}
      <div className="rdoc__signatures">
        <div className="rdoc__sig">
          <div className="rdoc__sig-line" />
          <p className="rdoc__sig-role">Elaborado por</p>
        </div>
        <div className="rdoc__sig">
          <div className="rdoc__sig-line" />
          <p className="rdoc__sig-role">Revisado por</p>
        </div>
        <div className="rdoc__sig">
          <div className="rdoc__sig-line" />
          <p className="rdoc__sig-role">Aprobado por</p>
        </div>
      </div>

      {/* ── FOOTER ─────────────────────────────────────────────────────── */}
      <footer className="rdoc__footer">
        <div className="rdoc__footer-left">
          <p>Calle 26 #51-53, Bogotá D.C.</p>
          <p><strong>Sede Administrativa</strong> — Torre Central, Piso 7</p>
          <p>Código Postal: 111321 &nbsp;|&nbsp; Tel: 7491513</p>
          <p>f /CundiGob &nbsp;·&nbsp; @CundinamarcaGob &nbsp;·&nbsp; www.cundinamarca.gov.co</p>
        </div>
        <img
          src={stdLogo}
          alt="Transformación Digital"
          className="rdoc__footer-logo"
        />
      </footer>
    </div>
  );
};

export default ReportDocumentPreview;
