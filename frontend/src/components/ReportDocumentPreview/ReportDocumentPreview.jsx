import gobLogo from '../../assets/logo gob cun.png';
import stdLogo from '../../assets/STD.png';
import './ReportDocumentPreview.css';

const fmt = (value) => (value === null || value === undefined || value === '' ? '—' : value);

const toPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '0.00%';
  }
  return `${Number(value).toFixed(2)}%`;
};

const fmtDate = (dateStr) => {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleDateString('es-CO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

const todayStr = () =>
  new Date().toLocaleDateString('es-CO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

const REPORT_META = {
  ESTADO_PROYECTO: {
    title: 'ESTADO DE PROYECTO ESPECÍFICO',
    filterFn: (project) => project?.nombre || 'Proyecto seleccionado',
    hasCounts: false,
  },
  TODOS_LOS_PROYECTOS: {
    title: 'ESTADO DE TODOS LOS PROYECTOS',
    filterFn: () => 'Proyectos PETI',
    hasCounts: false,
  },
  RETRASOS_ENTREGA: {
    title: 'PROYECTOS CON RETRASOS EN LA FECHA DE ENTREGA',
    filterFn: () => 'Todos los proyectos',
    hasCounts: false,
  },
  PLAN_COMUNICACIONES: {
    title: 'PLAN DE COMUNICACIONES',
    filterFn: () => 'Proyectos con plan de comunicaciones',
    hasCounts: true,
    countLabels: [
      'Proyectos con plan de comunicaciones:',
      'Proyectos sin plan de comunicaciones:',
    ],
  },
  FURAG: {
    title: 'PREGUNTAS FURAG',
    filterFn: () => 'Proyectos por dependencia',
    hasCounts: true,
    countLabels: [
      'Proyectos que responden FURAG:',
      'Proyectos que no responden FURAG:',
    ],
  },
  VERIFICACION_RIESGOS: {
    title: 'VERIFICACIÓN DE TRATAMIENTO A RIESGOS',
    filterFn: () => 'Proyectos con cierre',
    hasCounts: true,
    countLabels: [
      'Proyectos con tratamiento diligenciado:',
      'Proyectos sin tratamiento diligenciado:',
    ],
  },
};

const deriveCounts = (reportId, reportData) => {
  if (reportId === 'VERIFICACION_RIESGOS') {
    const risks = Array.isArray(reportData) ? reportData : [];
    const treated = risks.filter((risk) => risk.tratamiento && risk.tratamiento.trim() !== '').length;
    return [treated, Math.max(risks.length - treated, 0)];
  }

  if (reportId === 'FURAG') {
    const answers = Array.isArray(reportData?.respuestas) ? reportData.respuestas : [];
    const yes = answers.length > 0 ? 1 : 0;
    return [yes, 1 - yes];
  }

  if (reportId === 'PLAN_COMUNICACIONES') {
    const yes = reportData?.planPdfUrl ? 1 : 0;
    return [yes, 1 - yes];
  }

  return [0, 0];
};

const TableEstadoProyecto = ({ reportData }) => (
  <>
    <div className="report-doc__section-head">
      <h4>Información general del proyecto</h4>
    </div>
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>Código del proyecto</th>
          <th>Nombre del proyecto</th>
          <th>Director del proyecto</th>
          <th>Dependencia</th>
          <th>Avance total (%)</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>{fmt(reportData?.proyectoId)}</td>
          <td>{fmt(reportData?.nombre)}</td>
          <td>{fmt(reportData?.directorNombre)}</td>
          <td>{fmt(reportData?.dependencia)}</td>
          <td className="report-doc__table-value-emphasis">{toPercent(reportData?.avanceTotal)}</td>
        </tr>
      </tbody>
    </table>

    <div className="report-doc__section-head">
      <h4>Entregables vencidos</h4>
    </div>
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>#</th>
          <th>Nombre del entregable</th>
          <th>Fase</th>
          <th>Fecha de entrega programada</th>
        </tr>
      </thead>
      <tbody>
        {reportData?.entregablesVencidos?.length ? (
          reportData.entregablesVencidos.map((item, index) => (
            <tr key={item.id || index}>
              <td>{index + 1}</td>
              <td>{fmt(item.nombre)}</td>
              <td>{fmt(item.faseNombre)}</td>
              <td>{fmtDate(item.fechaEntrega)}</td>
            </tr>
          ))
        ) : (
          <tr>
            <td colSpan={4} className="report-doc__empty-cell">
              No hay entregables vencidos.
            </td>
          </tr>
        )}
      </tbody>
    </table>
  </>
);

const TableTodosProyectos = ({ reportData }) => {
  const projects = Array.isArray(reportData) ? reportData : [];

  return (
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>Código del proyecto</th>
          <th>Nombre del proyecto</th>
          <th>Avance total (%)</th>
          <th>Estado del proyecto</th>
        </tr>
      </thead>
      <tbody>
        {projects.length ? (
          projects.map((project) => (
            <tr key={project.id}>
              <td>{fmt(project.id)}</td>
              <td>{fmt(project.nombre)}</td>
              <td className="report-doc__table-value-emphasis">{toPercent(project.avance)}</td>
              <td>{fmt(project.estado)}</td>
            </tr>
          ))
        ) : (
          <tr>
            <td colSpan={4} className="report-doc__empty-cell">Sin datos disponibles.</td>
          </tr>
        )}
      </tbody>
    </table>
  );
};

const TableRetrasos = ({ reportData }) => {
  const projects = Array.isArray(reportData) ? reportData : [];

  if (!projects.length) {
    return (
      <table className="report-doc__table">
        <thead>
          <tr>
            <th>Código del proyecto</th>
            <th>Nombre del proyecto</th>
            <th>Avance total (%)</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colSpan={3} className="report-doc__empty-cell">Sin datos disponibles.</td>
          </tr>
        </tbody>
      </table>
    );
  }

  return projects.map((project, index) => (
    <div key={project.id || index} className="report-doc__delay-block">
      <div className="report-doc__section-head">
        <h4>Proyecto con retraso</h4>
      </div>
      <table className="report-doc__table">
        <thead>
          <tr>
            <th>Código del proyecto</th>
            <th>Nombre del proyecto</th>
            <th>Avance total (%)</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{fmt(project.id)}</td>
            <td>{fmt(project.nombre)}</td>
            <td className="report-doc__table-value-emphasis">{toPercent(project.avance)}</td>
          </tr>
        </tbody>
      </table>

      <table className="report-doc__table">
        <thead>
          <tr>
            <th>Entregables con fecha vencida</th>
            <th>Fecha de entrega programada</th>
          </tr>
        </thead>
        <tbody>
          {project.entregablesVencidos?.length ? (
            project.entregablesVencidos.map((deliverable, deliverableIndex) => (
              <tr key={deliverable.id || deliverableIndex}>
                <td>{fmt(deliverable.nombre ?? deliverable)}</td>
                <td>{fmtDate(deliverable.fechaEntrega ?? deliverable.fecha)}</td>
              </tr>
            ))
          ) : (
            Array.from({ length: Math.max(Number(project.entregablesAtrasados) || 1, 1) }, (_, deliverableIndex) => (
              <tr key={deliverableIndex}>
                <td>{deliverableIndex + 1}</td>
                <td>[DD/MM/AAAA]</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  ));
};

const TablePlanComunicaciones = ({ reportData, selectedProject }) => {
  const tienePlan = reportData?.planPdfUrl ? 'SI' : 'NO';

  return (
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>Código del proyecto</th>
          <th>Nombre del proyecto</th>
          <th>Director del proyecto</th>
          <th>Dependencia</th>
          <th>Plan de comunicaciones</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>{fmt(selectedProject?.id)}</td>
          <td>{fmt(reportData?.nombre ?? selectedProject?.nombre)}</td>
          <td>{fmt(selectedProject?.director ?? selectedProject?.directorNombre)}</td>
          <td>{fmt(reportData?.dependencia ?? selectedProject?.dependencia)}</td>
          <td className={tienePlan === 'SI' ? 'report-doc__si' : 'report-doc__no'}>{tienePlan}</td>
        </tr>
      </tbody>
    </table>
  );
};

const TableFurag = ({ reportData, selectedProject }) => {
  const responde = reportData?.objetivoGeneral || reportData?.esPeti ? 'SI' : 'NO';

  return (
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>Código del proyecto</th>
          <th>Nombre del proyecto</th>
          <th>Director del proyecto</th>
          <th>Dependencia</th>
          <th>Responde FURAG</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>{fmt(selectedProject?.id)}</td>
          <td>{fmt(reportData?.nombre ?? selectedProject?.nombre)}</td>
          <td>{fmt(selectedProject?.director ?? selectedProject?.directorNombre)}</td>
          <td>{fmt(selectedProject?.dependencia)}</td>
          <td className={responde === 'SI' ? 'report-doc__si' : 'report-doc__no'}>{responde}</td>
        </tr>
      </tbody>
    </table>
  );
};

const TableRiesgos = ({ reportData, selectedProject }) => {
  const risks = Array.isArray(reportData) ? reportData : [];
  const hasTratamiento = risks.some(
    (risk) => risk.tratamiento && risk.tratamiento.trim() !== '' && risk.estado !== 'PENDIENTE',
  );

  return (
    <table className="report-doc__table">
      <thead>
        <tr>
          <th>Código del proyecto</th>
          <th>Nombre del proyecto</th>
          <th>Director del proyecto</th>
          <th>Dependencia</th>
          <th>Diligenció tratamiento de riesgos</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>{fmt(selectedProject?.id)}</td>
          <td>{fmt(selectedProject?.nombre)}</td>
          <td>{fmt(selectedProject?.director ?? selectedProject?.directorNombre)}</td>
          <td>{fmt(selectedProject?.dependencia)}</td>
          <td className={hasTratamiento ? 'report-doc__si' : 'report-doc__no'}>
            {hasTratamiento ? 'SI' : 'NO'}
          </td>
        </tr>
      </tbody>
    </table>
  );
};

const ReportDocumentPreview = ({ reportId, reportData, selectedProject }) => {
  const meta = REPORT_META[reportId];

  if (!meta) {
    return null;
  }

  const title = meta.title;
  const filter = meta.filterFn(selectedProject);
  const vigencia = reportData?.vigenciaPeti || '2024-2027';
  const dateStr = todayStr();
  const [countYes, countNo] = meta.hasCounts ? deriveCounts(reportId, reportData) : [0, 0];

  const renderTable = () => {
    switch (reportId) {
      case 'ESTADO_PROYECTO':
        return <TableEstadoProyecto reportData={reportData} />;
      case 'TODOS_LOS_PROYECTOS':
        return <TableTodosProyectos reportData={reportData} />;
      case 'RETRASOS_ENTREGA':
        return <TableRetrasos reportData={reportData} />;
      case 'PLAN_COMUNICACIONES':
        return <TablePlanComunicaciones reportData={reportData} selectedProject={selectedProject} />;
      case 'FURAG':
        return <TableFurag reportData={reportData} selectedProject={selectedProject} />;
      case 'VERIFICACION_RIESGOS':
        return <TableRiesgos reportData={reportData} selectedProject={selectedProject} />;
      default:
        return null;
    }
  };

  return (
    <div className="report-doc">
      <header className="report-doc__masthead">
        <div className="report-doc__brand">
          <img
            src={gobLogo}
            alt="Gobernación de Cundinamarca"
            className="report-doc__header-logo"
          />
          <div className="report-doc__brand-copy">
            <p className="report-doc__eyebrow">Sistema integral de seguimiento institucional</p>
            <h1 className="report-doc__title">REPORTE: {title}</h1>
            <h2 className="report-doc__filter">
              FILTRO: <span className="report-doc__filter-value">[{filter}]</span>
            </h2>
          </div>
        </div>

        <aside className="report-doc__stamp">
          <span>Fecha del reporte</span>
          <strong>{dateStr}</strong>
          <small>Documento oficial</small>
        </aside>
      </header>

      <div className="report-doc__meta-grid">
        <div className="report-doc__meta-card">
          <span>Fecha del reporte</span>
          <strong>{dateStr}</strong>
        </div>
        <div className="report-doc__meta-card">
          <span>Vigencia</span>
          <strong>{vigencia}</strong>
        </div>
      </div>

      {meta.hasCounts && (
        <div className="report-doc__counts">
          <div className="report-doc__count-card">
            <span>{meta.countLabels[0]}</span>
            <strong>{countYes}</strong>
          </div>
          <div className="report-doc__count-card">
            <span>{meta.countLabels[1]}</span>
            <strong>{countNo}</strong>
          </div>
        </div>
      )}

      <div className="report-doc__body">
        {renderTable()}
      </div>

      <footer className="report-doc__footer">
        <hr className="report-doc__footer-rule" />
        <div className="report-doc__footer-content">
          <div className="report-doc__footer-address">
            <p>Calle 26 #51-53 Bogotá D.C.</p>
            <p><strong>Sede Administrativa</strong> - Torre Central Piso 7.</p>
            <p>Código Postal: 111321 - Teléfono: 7491513</p>
            <p>f /CundiGob • @CundinamarcaGob</p>
            <p>www.cundinamarca.gov.co</p>
          </div>
          <img
            src={stdLogo}
            alt="Transformación Digital - Gobernación de Cundinamarca"
            className="report-doc__footer-logo"
          />
        </div>
      </footer>
    </div>
  );
};

export default ReportDocumentPreview;
