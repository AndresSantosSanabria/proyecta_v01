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

const FURAG_KEYS = [
  'infraestructuraDatos',
  'interoperabilidad',
  'digitalizacionAutomatizacion',
  'contratacionPublica',
  'serviciosNube',
  'sandbox',
  'tecnologiasEmergentes',
];

const normalizeText = (value) =>
  String(value || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();

const sameText = (left, right) => normalizeText(left) === normalizeText(right);

const hasText = (value) => value !== null && value !== undefined && String(value).trim() !== '';

const furagCompleto = (project) => {
  const furag = project?.furag || {};
  return FURAG_KEYS.every((key) => hasText(furag[key]));
};

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
const BodyEstadoProyecto = ({ reportData, selectedProject }) => {
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
        { label: 'Estado',                value: fmt(selectedProject?.estado ?? reportData?.estado) },
        { label: 'Vigencia',              value: fmt(selectedProject?.vigenciaPeti ?? reportData?.vigenciaPeti) },
        { label: 'Patrocinador',          value: fmt(reportData?.patrocinadorNombre) },
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

    </>
  );
};

/* 2. TODOS LOS PROYECTOS */
const BodyTodosProyectos = ({ reportData }) => {
  const rows = Array.isArray(reportData) ? reportData : [];

  return (
    <>
      <table className="rdoc__table">
        <thead>
          <tr>
            <th>Código del proyecto</th>
            <th>Nombre del proyecto</th>
            <th>Avance total del proyecto (%)</th>
            <th>Estado del proyecto (En desarrollo - cerrado)</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row.id || i}>
              <td>{fmt(row.id)}</td>
              <td>{fmt(row.nombre)}</td>
              <td className="rdoc__td-emphasis">{toPercent(row.avance)}</td>
              <td>{fmt(row.estado)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
};

/* 3. RETRASOS EN FECHA DE ENTREGA */
const BodyRetrasos = ({ reportData }) => {
  const rows = Array.isArray(reportData) ? reportData : [];
  const deliverables = [
    { numero: '1', nombre: '', fechaEntrega: '[DD/MM/AAAA]' },
    { numero: '2', nombre: '', fechaEntrega: '[DD/MM/AAAA]' },
    { numero: '3', nombre: '', fechaEntrega: '[DD/MM/AAAA]' },
  ];

  return (
    <div className="rdoc__furag">
      <div className="rdoc__furag-meta">
        <p><strong>FILTRO:</strong> [Todos los proyectos]</p>
        <p><strong>Fecha del reporte:</strong> {todayStr()}</p>
        <p><strong>Vigencia:</strong> [2024-2027]</p>
      </div>

      <table className="rdoc__table">
        <thead>
          <tr>
            <th>C?digo del proyecto</th>
            <th>Nombre del proyecto</th>
            <th>Avance total del proyecto (%)</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row.id || i}>
              <td>{fmt(row.id)}</td>
              <td>{fmt(row.nombre)}</td>
              <td className="rdoc__td-emphasis">{toPercent(row.avance)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="rdoc__spacer" />
      <SectionHead number="2" title="Nombre de los entregables que tienen fecha vencida" />
      <DocTable
        columns={[
          { key: 'numero', label: '', width: '56px' },
          { key: 'nombre', label: 'Nombre de los entregables que tienen fecha vencida', width: '55%' },
          { key: 'fechaEntrega', label: 'Fecha de entrega programada', width: '45%' },
        ]}
        rows={deliverables}
      />
    </div>
  );
};

/* 4. PLAN DE COMUNICACIONES */
const BodyPlanComunicaciones = ({ reportData, projects }) => {
  const sourceProjects = Array.isArray(projects) ? projects : [];
  const rows = sourceProjects
    .filter((project) => !project?.peti)
    .sort((a, b) => String(a?.id || '').localeCompare(String(b?.id || ''), 'es', { sensitivity: 'base' }))
    .map((project) => ({
      id: project?.id,
      nombre: project?.nombre,
      director: project?.director ?? project?.directorNombre,
      dependencia: project?.dependencia,
      tienePlan: Boolean(project?.planComunicacionesPdf),
    }));

  const totalConPlan = rows.filter((row) => row.tienePlan).length;
  const totalSinPlan = rows.length - totalConPlan;

  return (
    <div className="rdoc__furag">
      <div className="rdoc__furag-meta">
        <p><strong>FILTRO:</strong> [Proyectos No PETI]</p>
        <p><strong>Fecha del reporte:</strong> {todayStr()}</p>
        <p><strong>Vigencia:</strong> [2024-2027]</p>
      </div>

      <div className="rdoc__furag-counts">
        <p><strong>Conteo de proyectos con plan de comunicaciones:</strong> {totalConPlan}</p>
        <p><strong>Conteo de proyectos sin plan de comunicaciones:</strong> {totalSinPlan}</p>
      </div>

      <DocTable
        columns={[
          { key: 'id', label: 'C?digo del proyecto', width: '18%' },
          { key: 'nombre', label: 'Nombre del proyecto', width: '22%' },
          { key: 'director', label: 'Director del proyecto', width: '20%' },
          { key: 'dependencia', label: 'Dependencia', width: '20%' },
          {
            key: 'tienePlan',
            label: 'Plan de comunicaciones',
            width: '20%',
            render: (row) => <StatusBadge value={row.tienePlan ? 'SI' : 'NO'} />,
          },
        ]}
        rows={rows}
        emptyMsg="No hay proyectos No PETI registrados."
      />
    </div>
  );
};

/* 6. VERIFICACIÓN DE RIESGOS */
const BodyRiesgos = ({ reportData }) => {
  const rows = Array.isArray(reportData) ? reportData : [];
  const totalYes = rows.filter((row) => row.diligencioTratamiento).length;
  const totalNo = Math.max(rows.length - totalYes, 0);

  return (
    <div className="rdoc__furag">
      <div className="rdoc__furag-meta">
        <p><strong>FILTRO:</strong> [Proyectos con cierre]</p>
        <p><strong>Fecha del reporte:</strong> {todayStr()}</p>
        <p><strong>Vigencia:</strong> [2024-2027]</p>
      </div>

      <div className="rdoc__furag-counts">
        <p><strong>Conteo de proyectos que diligenciaron tratamiento de riesgos:</strong> {totalYes}</p>
        <p><strong>Conteo de proyectos que NO diligenciaron tratamiento de riesgos:</strong> {totalNo}</p>
      </div>

      <DocTable
        columns={[
          { key: 'proyectoId', label: 'Código del proyecto', width: '18%' },
          { key: 'nombreProyecto', label: 'Nombre del proyecto', width: '27%' },
          { key: 'directorProyecto', label: 'Director del proyecto', width: '19%' },
          { key: 'dependencia', label: 'Dependencia', width: '21%' },
          {
            key: 'diligencioTratamiento',
            label: 'Diligenció tratamiento de riesgos',
            width: '15%',
            render: (row) => <StatusBadge value={row.diligencioTratamiento ? 'SI' : 'NO'} />,
          },
        ]}
        rows={rows}
        emptyMsg="No hay proyectos con cierre registrados."
      />
    </div>
  );
};

/* ═══════════════════════════════════════════════════════════════════════════
   MAIN COMPONENT
   ═══════════════════════════════════════════════════════════════════════════ */

/* 5. FURAG */
const BodyFuragV2 = ({ reportData, selectedProject, projects }) => {
  const dependency = selectedProject?.dependencia || reportData?.dependencia || '';
  const sourceProjects = Array.isArray(projects) ? projects : [];
  const rows = sourceProjects
    .filter((project) => sameText(project?.dependencia, dependency))
    .sort((a, b) => String(a?.id || '').localeCompare(String(b?.id || ''), 'es', { sensitivity: 'base' }))
    .map((project) => ({
      id: project?.id,
      nombre: project?.nombre,
      director: project?.director ?? project?.directorNombre,
      dependencia: project?.dependencia,
      responde: furagCompleto(project),
    }));

  const totalResponden = rows.filter((row) => row.responde).length;
  const totalNoResponden = rows.length - totalResponden;
  const vigencia = selectedProject?.vigenciaPeti || reportData?.vigenciaPeti || '2024-2027';

  return (
    <div className="rdoc__furag">
      <div className="rdoc__furag-meta">
        <p><strong>FILTRO:</strong> [Proyectos por dependencia]</p>
        <p><strong>Fecha del reporte:</strong> {todayStr()}</p>
        <p><strong>Vigencia:</strong> {vigencia}</p>
      </div>

      <div className="rdoc__furag-counts">
        <p><strong>Conteo de proyectos que responden preguntas FURAG:</strong> {totalResponden}</p>
        <p><strong>Conteo de proyectos que NO responden preguntas FURAG:</strong> {totalNoResponden}</p>
      </div>

      <DocTable
        columns={[
          { key: 'id', label: 'Código del proyecto', width: '18%' },
          { key: 'nombre', label: 'Nombre del proyecto', width: '22%' },
          { key: 'director', label: 'Director del proyecto', width: '20%' },
          { key: 'dependencia', label: 'Dependencia', width: '20%' },
          {
            key: 'responde',
            label: 'Responde todas las preguntas FURAG',
            width: '20%',
            render: (row) => <StatusBadge value={row.responde ? 'SI' : 'NO'} />,
          },
        ]}
        rows={rows}
        emptyMsg="No hay proyectos registrados para la dependencia seleccionada."
      />
    </div>
  );
};

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

const ReportDocumentPreview = ({ reportId, reportData, selectedProject, projects }) => {
  const meta = REPORT_META[reportId];
  if (!meta) return null;

  const vigencia = reportData?.vigenciaPeti
    || (Array.isArray(reportData) ? '' : reportData?.vigencia)
    || '2024-2027';

  const renderBody = () => {
    switch (reportId) {
      case 'ESTADO_PROYECTO':
        return <BodyEstadoProyecto reportData={reportData} selectedProject={selectedProject} />;
      case 'TODOS_LOS_PROYECTOS':
        return <BodyTodosProyectos reportData={reportData} />;
      case 'RETRASOS_ENTREGA':
        return <BodyRetrasos reportData={reportData} />;
      case 'PLAN_COMUNICACIONES':
        return <BodyPlanComunicaciones reportData={reportData} projects={projects} />;
      case 'FURAG':
        return <BodyFuragV2 reportData={reportData} selectedProject={selectedProject} projects={projects} />;
      case 'VERIFICACION_RIESGOS':
        return <BodyRiesgos reportData={reportData} />;
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
