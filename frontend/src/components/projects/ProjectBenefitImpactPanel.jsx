import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Edit3,
  FileText,
  Lock,
  Save,
  ShieldAlert,
  X,
  ChevronDown,
  ChevronUp,
  ThumbsUp,
  ThumbsDown,
} from 'lucide-react';
import projectService from '../../services/projectService';
import './ProjectBenefitImpactPanel.css';

const emptyForm = {
  beneficiosValorPublico: '',
  impactosValorPublico: '',
  observaciones: '',
};

const unwrap = (response) => response?.data?.data ?? response?.data ?? response ?? null;

const toFormState = (payload) => ({
  beneficiosValorPublico: payload?.beneficioPrincipal ?? '',
  impactosValorPublico: payload?.impactoSocial ?? '',
  observaciones: payload?.observaciones ?? '',
});

const Field = ({ label, value, wide = false }) => (
  <article className={`benefit-impact-field${wide ? ' wide' : ''}`}>
    <span>{label}</span>
    <strong>{value || 'No registrado'}</strong>
  </article>
);

const AccordionField = ({ label, value }) => {
  const [open, setOpen] = useState(false);
  return (
    <article
      className="benefit-impact-field wide"
      style={{ border: '1px solid #e2e8f0', borderRadius: '0.375rem', padding: '0.75rem' }}
    >
      <button
        type="button"
        onClick={() => setOpen(!open)}
        style={{
          width: '100%',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'none',
          border: 'none',
          padding: 0,
          cursor: 'pointer',
          textAlign: 'left',
          fontWeight: '600',
        }}
      >
        <span>{label}</span>
        {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      {open && (
        <div style={{ marginTop: '0.75rem', fontWeight: 'normal', color: '#475569', lineHeight: '1.5' }}>
          {value || 'No registrado'}
        </div>
      )}
    </article>
  );
};

const TextareaField = ({ label, name, value, onChange, placeholder, disabled = false, required = false }) => (
  <label className="benefit-impact-form-group">
    <span className="benefit-impact-label">
      {label}
      {required ? ' *' : ''}
    </span>
    <textarea
      name={name}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      rows={5}
      className="benefit-impact-textarea"
    />
  </label>
);

const ProjectBenefitImpactPanel = ({ proyectoId, projectName, refreshToken = 0, onSaved, isGestor = false }) => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [reviewing, setReviewing] = useState(false);
  const [error, setError] = useState('');
  const [data, setData] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [editorOpen, setEditorOpen] = useState(false);
  const [success, setSuccess] = useState('');
  const [forbidden, setForbidden] = useState(false);
  const [showRejectInput, setShowRejectInput] = useState(false);
  const [observacion, setObservacion] = useState('');

  const isEditable = Boolean(data?.editable) && !isGestor;
  const isRequired = Boolean(data?.requerido && data?.editable) && !isGestor;
  const visibleForAudit = Boolean(data?.visibleParaGestor);
  const showEditor = Boolean(editorOpen && isEditable);

  useEffect(() => {
    if (!showEditor) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [showEditor]);

  useEffect(() => {
    let active = true;

    const load = async () => {
      if (!proyectoId) {
        setLoading(false);
        setError('El proyecto no es valido.');
        return;
      }

      try {
        setLoading(true);
        setError('');
        setForbidden(false);
        const response = await projectService.getBenefitImpact(proyectoId);
        if (!active) return;
        const payload = unwrap(response);
        setData(payload);
        setForm(toFormState(payload));
        setEditorOpen(Boolean(payload?.editable && !isGestor && (payload?.requerido || payload?.estado === 'OBSERVADO')));
      } catch (requestError) {
        if (!active) return;
        const status = requestError?.response?.status;
        if (status === 403) {
          setForbidden(true);
          setData(null);
          setForm(emptyForm);
          setEditorOpen(false);
          setError('');
        } else {
          const message =
            requestError?.response?.data?.detail ||
            requestError?.response?.data?.message ||
            requestError?.message ||
            'No fue posible cargar la informacion de beneficio e impacto.';
          setError(message);
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    void load();
    return () => {
      active = false;
    };
  }, [proyectoId, refreshToken, isGestor]);

  const statusMeta = useMemo(() => {
    if (forbidden) {
      return {
        tone: 'warning',
        title: 'Aun no disponible para consulta',
        detail: 'El registro de beneficio e impacto se publica cuando el Director lo diligencia.',
      };
    }

    if (!data) {
      return {
        tone: 'neutral',
        title: 'Sin requerimiento activo',
        detail: 'El proyecto aun no alcanza el hito que obliga a diligenciar la informacion de impacto.',
      };
    }

    if (data.estado === 'DILIGENCIADO') {
      return {
        tone: 'success',
        title: 'Diligenciado',
        detail: 'La informacion ya esta disponible para revision y auditoria.',
      };
    }

    if (data.estado === 'OBSERVADO') {
      return {
        tone: 'danger',
        title: 'Observado',
        detail: 'El Gestor rechazo la informacion y el Director debe corregirla.',
      };
    }

    return {
      tone: 'danger',
      title: 'Pendiente de diligenciar',
      detail: 'El Director debe completar este formulario para habilitar el cierre formal del proyecto.',
    };
  }, [data, forbidden]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!proyectoId || saving) return;

    try {
      setSaving(true);
      setError('');
      setSuccess('');

      const response = await projectService.saveBenefitImpact(proyectoId, form);
      const saved = unwrap(response);
      setData(saved);
      setForm(toFormState(saved));
      setEditorOpen(false);
      setSuccess('La informacion de beneficio e impacto quedo guardada y lista para revision.');
      if (onSaved) onSaved(saved);
    } catch (saveError) {
      const message =
        saveError?.response?.data?.detail ||
        saveError?.response?.data?.message ||
        saveError?.message ||
        'No fue posible guardar la informacion.';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  const handleReview = async (aprobado) => {
    if (!proyectoId || reviewing) return;
    if (!aprobado && !observacion.trim()) {
      setShowRejectInput(true);
      return;
    }

    try {
      setReviewing(true);
      setError('');
      setSuccess('');
      const response = await projectService.reviewBenefitImpact(proyectoId, aprobado, observacion);
      const saved = unwrap(response);
      setData(saved);
      setForm(toFormState(saved));
      setShowRejectInput(false);
      setObservacion('');
      if (!aprobado) {
        setEditorOpen(Boolean(saved?.editable && !isGestor));
      }
      setSuccess(aprobado ? 'Informacion aprobada exitosamente.' : 'Informacion observada. El Director debe corregirla y reenviarla.');
      if (onSaved) onSaved(saved);
    } catch (reviewError) {
      const message =
        reviewError?.response?.data?.detail ||
        reviewError?.response?.data?.message ||
        reviewError?.message ||
        'No fue posible registrar la revision.';
      setError(message);
    } finally {
      setReviewing(false);
    }
  };

  if (loading) {
    return (
      <section className="benefit-impact-card loading">
        <div className="benefit-impact-skeleton" />
        <div className="benefit-impact-skeleton short" />
        <div className="benefit-impact-skeleton" />
      </section>
    );
  }

  return (
    <>
      <section className="benefit-impact-card">
        <div className={`benefit-impact-top ${statusMeta.tone}`}>
          <div className="benefit-impact-title">
            <span className="benefit-impact-kicker">
              <ShieldAlert size={14} />
              Beneficio e impacto del proyecto
            </span>
            <h2>{projectName || 'Proyecto'}</h2>
            <p>{statusMeta.detail}</p>
          </div>

          <div className="benefit-impact-actions">
            <div className={`benefit-impact-state ${statusMeta.tone}`}>{statusMeta.title}</div>
            {isEditable && (
              <button type="button" className="benefit-impact-open" onClick={() => setEditorOpen(true)}>
                {data?.estado === 'DILIGENCIADO' ? <Edit3 size={16} /> : <FileText size={16} />}
                {data?.estado === 'DILIGENCIADO' ? 'Editar' : 'Diligenciar'}
              </button>
            )}
          </div>
        </div>

        {error && (
          <div className="benefit-impact-alert error">
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="benefit-impact-alert success">
            <CheckCircle2 size={18} />
            <span>{success}</span>
          </div>
        )}

        {data ? (
          <>
            <div className="benefit-impact-grid">
              <Field label="Estado" value={data.estado} />
              <AccordionField label="Beneficios del proyecto" value={data.beneficioPrincipal} />
              <AccordionField label="Impactos en valor público" value={data.impactoSocial} />
              <AccordionField label="Observaciones" value={data.observaciones} />
            </div>

            {isGestor && (data.estado === 'DILIGENCIADO' || data.estado === 'OBSERVADO' || data.estado === 'APROBADO') && (
              <div style={{ marginTop: '1.5rem', padding: '1.25rem', background: '#0f172a', borderRadius: '6px', border: '1px solid #334155' }}>
                <h4 style={{ margin: '0 0 1rem', color: '#f1f5f9', fontSize: '1rem' }}>Revision del Gestor</h4>
                {data.estado === 'APROBADO' && (
                  <div className="benefit-impact-alert success">
                    <CheckCircle2 size={16} />
                    <span>
                      Informacion aprobada el {data.revisadoEn ? new Date(data.revisadoEn).toLocaleDateString('es-CO') : '-'} por {data.revisadoPor}.
                    </span>
                  </div>
                )}
                {data.estado === 'OBSERVADO' && (
                  <div className="benefit-impact-alert error" style={{ marginBottom: '1rem' }}>
                    <AlertTriangle size={16} />
                    <span>Observada. Se requiere que el Director corrija y vuelva a diligenciar.</span>
                  </div>
                )}
                {(data.estado === 'DILIGENCIADO' || data.estado === 'OBSERVADO') && (
                  <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-start', flexDirection: 'column' }}>
                    {showRejectInput && (
                      <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        <label style={{ color: '#94a3b8', fontSize: '0.875rem' }}>Observacion de rechazo *</label>
                        <textarea
                          value={observacion}
                          onChange={(e) => setObservacion(e.target.value)}
                          rows={3}
                          placeholder="Describe el motivo del rechazo..."
                          className="benefit-impact-textarea"
                        />
                      </div>
                    )}
                    <div style={{ display: 'flex', gap: '0.75rem' }}>
                      {!showRejectInput && (
                        <button
                          type="button"
                          disabled={reviewing}
                          onClick={() => handleReview(true)}
                          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: '#10b981', color: 'white', border: 'none', padding: '0.6rem 1.25rem', borderRadius: '4px', fontWeight: '600', cursor: 'pointer' }}
                        >
                          <ThumbsUp size={16} /> Aprobar
                        </button>
                      )}
                      <button
                        type="button"
                        disabled={reviewing}
                        onClick={() => (showRejectInput ? handleReview(false) : setShowRejectInput(true))}
                        style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: showRejectInput ? '#ef4444' : '#64748b', color: 'white', border: 'none', padding: '0.6rem 1.25rem', borderRadius: '4px', fontWeight: '600', cursor: 'pointer' }}
                      >
                        <ThumbsDown size={16} /> {showRejectInput ? 'Confirmar Rechazo' : 'Rechazar / Observar'}
                      </button>
                      {showRejectInput && (
                        <button
                          type="button"
                          onClick={() => {
                            setShowRejectInput(false);
                            setObservacion('');
                          }}
                          style={{ background: 'none', color: '#94a3b8', border: '1px solid #334155', padding: '0.6rem 1rem', borderRadius: '4px', cursor: 'pointer' }}
                        >
                          Cancelar
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            <div className="benefit-impact-footnote">
              <Lock size={14} />
              <span>
                {visibleForAudit
                  ? 'Visible para Gestor y auditoria institucional.'
                  : 'Solo visible para el Director mientras esta pendiente de diligenciamiento.'}
              </span>
            </div>
          </>
        ) : (
          <div className="benefit-impact-empty">
            <Lock size={18} />
            <div>
              <strong>No hay informacion publicada todavia.</strong>
              <p>Cuando el proyecto alcance el 100% de entregables aprobados, aparecera el formulario obligatorio.</p>
            </div>
          </div>
        )}
      </section>

      {showEditor && (
        <div
          className="benefit-impact-overlay"
          role="presentation"
          onClick={isRequired ? undefined : () => setEditorOpen(false)}
        >
          <div
            className={`benefit-impact-modal ${isRequired ? 'restrictive' : ''}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="benefit-impact-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="benefit-impact-modal-header">
              <div>
                <span className="benefit-impact-kicker">
                  <ShieldAlert size={14} />
                  Formulario obligatorio
                </span>
                <h3 id="benefit-impact-modal-title">Beneficio e impacto del proyecto</h3>
                <p>Registre solo los beneficios e impactos del proyecto, orientados a la generacion de valor publico.</p>
              </div>
              {!isRequired && (
                <button
                  type="button"
                  className="benefit-impact-close"
                  onClick={() => setEditorOpen(false)}
                  aria-label="Cerrar formulario"
                >
                  <X size={18} />
                </button>
              )}
            </div>

            <form className="benefit-impact-form" onSubmit={handleSubmit}>
              <div className="benefit-impact-form-grid">
                <TextareaField
                  label="Beneficios del proyecto"
                  name="beneficiosValorPublico"
                  value={form.beneficiosValorPublico}
                  onChange={handleChange}
                  required
                  placeholder="Describe el beneficio publico que genera el proyecto para la ciudadania o la entidad."
                />
                <TextareaField
                  label="Impactos en valor publico"
                  name="impactosValorPublico"
                  value={form.impactosValorPublico}
                  onChange={handleChange}
                  required
                  placeholder="Explica como esos beneficios se traducen en valor publico, resultados y percepcion ciudadana."
                />
                <TextareaField
                  label="Observaciones"
                  name="observaciones"
                  value={form.observaciones}
                  onChange={handleChange}
                  placeholder="Observaciones adicionales, si aplican."
                />
              </div>

              <div className="benefit-impact-form-actions">
                <button type="submit" className="benefit-impact-submit" disabled={saving}>
                  <Save size={16} />
                  {saving ? 'Guardando...' : 'Guardar informacion'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};

export default ProjectBenefitImpactPanel;
