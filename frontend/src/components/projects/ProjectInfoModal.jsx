import { useState } from 'react';
import { X, ChevronDown, ChevronUp, FileText, Download, Users, Target, Layers, Shield, ClipboardList } from 'lucide-react';
import documentService from '../../services/documentService';
import './ProjectInfoModal.css';

const Section = ({ title, icon: Icon, children, defaultOpen = true }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="pim-section">
      <button type="button" className="pim-section-header" onClick={() => setOpen(!open)}>
        <div className="pim-section-title">
          <Icon size={16} />
          <span>{title}</span>
        </div>
        {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      {open && <div className="pim-section-body">{children}</div>}
    </div>
  );
};

const Field = ({ label, value }) => (
  <div className="pim-field">
    <span className="pim-field-label">{label}</span>
    <strong className="pim-field-value">{value || '—'}</strong>
  </div>
);

const DocumentLink = ({ proyectoId, tipoDocumento, nombre }) => {
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (!proyectoId || !tipoDocumento) return;
    try {
      setDownloading(true);
      const blob = await documentService.descargarDocumento(proyectoId, tipoDocumento);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', nombre || `${tipoDocumento.toLowerCase()}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      console.error('Error descargando documento');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <button type="button" className="pim-doc-link" onClick={handleDownload} disabled={downloading}>
      <FileText size={14} />
      <span>{nombre || tipoDocumento}</span>
      <Download size={14} />
    </button>
  );
};

const ProjectInfoModal = ({ project, open, onClose }) => {
  if (!open || !project) return null;

  const patrocinador = project.patrocinador || {};
  const equipoTrabajo = Array.isArray(project.equipoTrabajo) ? project.equipoTrabajo : [];
  const fases = Array.isArray(project.fases) ? project.fases : [];
  const objetivos = Array.isArray(project.objetivosEspecificos) ? project.objetivosEspecificos : [];
  const furag = project.furag || {};

  return (
    <div className="pim-overlay" role="presentation" onClick={onClose}>
      <div className="pim-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <header className="pim-header">
          <div>
            <span className="pim-kicker">Informacion del proyecto</span>
            <h2>{project.nombre || project.nombreProyecto || 'Proyecto'}</h2>
            <p>{project.codigo || ''} · {project.dependencia || ''}</p>
          </div>
          <button type="button" className="pim-close" onClick={onClose} aria-label="Cerrar">
            <X size={20} />
          </button>
        </header>

        <div className="pim-body">
          <Section title="Datos generales" icon={ClipboardList}>
            <div className="pim-grid">
              <Field label="Dependencia" value={project.dependencia} />
              <Field label="Fecha de inicio" value={project.fechaInicio} />
              <Field label="Presupuesto estimado" value={project.presupuestoEstimado ? `$${Number(project.presupuestoEstimado).toLocaleString('es-CO')}` : null} />
              <Field label="Estado" value={project.estado} />
            </div>
            <div className="pim-field pim-field-wide">
              <span className="pim-field-label">Alcance detallado</span>
              <p className="pim-field-text">{project.alcanceDetallado || '—'}</p>
            </div>
            {objetivos.length > 0 && (
              <div className="pim-field pim-field-wide">
                <span className="pim-field-label">Objetivos especificos</span>
                <ul className="pim-list">
                  {objetivos.map((obj, i) => <li key={i}>{obj}</li>)}
                </ul>
              </div>
            )}
          </Section>

          <Section title="Patrocinador y equipo" icon={Users}>
            <div className="pim-grid">
              <Field label="Nombre" value={patrocinador.nombre} />
              <Field label="Cargo" value={patrocinador.cargo} />
              <Field label="Entidad" value={patrocinador.entidad} />
              <Field label="Proceso SIGC" value={patrocinador.procesoSigc} />
              <Field label="Procedimiento SIGC" value={patrocinador.procedimientoSigc} />
            </div>
            {equipoTrabajo.length > 0 && (
              <div className="pim-field pim-field-wide">
                <span className="pim-field-label">Equipo de trabajo</span>
                <div className="pim-team-list">
                  {equipoTrabajo.map((m, i) => (
                    <div key={i} className="pim-team-member">
                      <strong>{m.nombre}</strong>
                      <span>{m.cargo}{m.rol ? ` · ${m.rol}` : ''}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Section>

          <Section title="Fases, hitos y entregables" icon={Layers} defaultOpen={false}>
            {fases.length > 0 ? fases.map((fase, fi) => (
              <div key={fi} className="pim-phase">
                <div className="pim-phase-header">
                  <strong>Fase {fi + 1}: {fase.nombre}</strong>
                  <span>{fase.ponderacion}%</span>
                </div>
                {(fase.hitos || []).map((hito, hi) => (
                  <div key={hi} className="pim-hito">
                    <div className="pim-hito-header">
                      <span>Hito {fi + 1}.{hi + 1}: {hito.nombre}</span>
                      <span>{hito.ponderacion}%</span>
                    </div>
                    {(hito.entregables || []).map((ent, ei) => (
                      <div key={ei} className="pim-entregable">
                        <span>{ent.nombre}</span>
                        <span>{ent.ponderacion}% · {ent.fechaLimite || 'Sin fecha'}</span>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            )) : <p className="pim-empty">No hay fases configuradas.</p>}
          </Section>

          <Section title="PETI" icon={Shield} defaultOpen={false}>
            <div className="pim-grid">
              <Field label="Proyecto en PETI" value={project.esPeti || project.peti ? 'Si' : 'No'} />
              {(project.esPeti || project.peti) && (
                <>
                  <Field label="Vigencia" value={project.vigenciaPeti} />
                  <Field label="Estrategia" value={project.estrategiaPeti} />
                </>
              )}
              <Field label="Plan de comunicaciones" value={project.tienePlanComunicaciones ? 'Si' : 'No'} />
            </div>
          </Section>

          <Section title="FURAG" icon={Target} defaultOpen={false}>
            <div className="pim-grid">
              <Field label="Infraestructura de datos" value={furag.infraestructuraDatos} />
              <Field label="Interoperabilidad" value={furag.interoperabilidad} />
              <Field label="Digitalizacion/Automatizacion" value={furag.digitalizacionAutomatizacion} />
              <Field label="Contratacion publica" value={furag.contratacionPublica} />
              <Field label="Servicios en la nube" value={furag.serviciosNube} />
              <Field label="Sandbox regulatorio" value={furag.sandbox} />
              <Field label="Tecnologias emergentes" value={furag.tecnologiasEmergentes} />
            </div>
          </Section>

          <Section title="Documentos" icon={FileText} defaultOpen={false}>
            <div className="pim-docs">
              {project.viabilizacionPdf && (
                <DocumentLink proyectoId={project.codigo || project.id} tipoDocumento="VIABILIZACION" nombre="Documento de viabilizacion" />
              )}
              {project.actaConstitucionPdf && (
                <DocumentLink proyectoId={project.codigo || project.id} tipoDocumento="ACTA_CONSTITUCION" nombre="Acta de constitucion" />
              )}
              {project.cronogramaPdf && (
                <DocumentLink proyectoId={project.codigo || project.id} tipoDocumento="CRONOGRAMA" nombre="Cronograma del proyecto" />
              )}
              {project.planComunicacionesPdf && (
                <DocumentLink proyectoId={project.codigo || project.id} tipoDocumento="PLAN_COMUNICACIONES" nombre="Plan de comunicaciones" />
              )}
              {!project.viabilizacionPdf && !project.actaConstitucionPdf && !project.cronogramaPdf && !project.planComunicacionesPdf && (
                <p className="pim-empty">No hay documentos cargados.</p>
              )}
            </div>
          </Section>
        </div>
      </div>
    </div>
  );
};

export default ProjectInfoModal;
