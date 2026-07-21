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
import securityService from '../../services/securityService';
import './ProjectBenefitImpactPanel.css';

const emptyForm = {
  beneficiosValorPublico: '',
  impactosValorPublico: '',
  observaciones: '',
};

const emitGlobalToast = (detail) => {
  window.dispatchEvent(new CustomEvent('proyecta:toast', { detail }));
};

const refreshNotifications = () => {
  window.dispatchEvent(new Event('proyecta:notificaciones:refresh'));
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
        <div style={{
          marginTop: '0.75rem',
          fontWeight: 'normal',
          color: '#475569',
          lineHeight: '1.5',
          wordWrap: 'break-word',
          overflowWrap: 'break-word',
          whiteSpace: 'pre-wrap'
        }}>
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
  const [rejectError, setRejectError] = useState('');

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

    if (data.estado === 'APROBADO') {
      return {
        tone: 'success',
        title: 'Aprobado',
        detail: 'La informacion de beneficio e impacto ha sido auditada y aprobada.',
      };
    }

    return {
      tone: 'warning',
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
      const nextState = String(saved?.estado || 'DILIGENCIADO').toUpperCase();
      const eventType = nextState === 'OBSERVADO'
        ? 'BENEFICIO_IMPACTO_REENVIADO'
        : nextState === 'APROBADO'
          ? 'BENEFICIO_IMPACTO_APROBADO'
          : 'BENEFICIO_IMPACTO_DILIGENCIADO';
      setSuccess('La informacion de beneficio e impacto quedo guardada y lista para revision.');
      emitGlobalToast({
        tone: 'success',
        title: 'Beneficio e impacto guardado',
        message:
          nextState === 'APROBADO'
            ? 'El registro quedo aprobado y los involucrados fueron notificados.'
            : nextState === 'OBSERVADO'
              ? 'El registro fue reenviado tras observaciones y se notifico al equipo.'
              : 'El registro fue diligenciado y se notifico al equipo.',
      });
      refreshNotifications();
      void securityService.notifyProjectBenefitImpactEvent(proyectoId, {
        proyectoId,
        proyectoNombre: projectName || '',
        evento: eventType,
        estado: nextState,
        mensaje:
          nextState === 'APROBADO'
            ? 'El beneficio e impacto fue aprobado.'
            : nextState === 'OBSERVADO'
              ? 'El beneficio e impacto fue corregido y reenviado.'
              : 'El beneficio e impacto fue diligenciado por el Director.',
      }).catch((notifyError) => {
        console.warn('No fue posible notificar a los involucrados del proyecto:', notifyError);
      });
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
      setRejectError('Debe ingresar el motivo del rechazo para continuar.');
      return;
    }

    try {
      setReviewing(true);
      setError('');
      setSuccess('');
      setRejectError('');
      const response = await projectService.reviewBenefitImpact(proyectoId, aprobado, observacion);
      const saved = unwrap(response);
      setData(saved);
      setForm(toFormState(saved));
      setShowRejectInput(false);
      setObservacion('');
      setRejectError('');
      if (!aprobado) {
        setEditorOpen(Boolean(saved?.editable && !isGestor));
      }
      setSuccess(aprobado ? 'Informacion aprobada exitosamente.' : 'Informacion observada. El Director debe corregirla y reenviarla.');
      emitGlobalToast({
        tone: aprobado ? 'success' : 'warning',
        title: aprobado ? 'Beneficio e impacto aprobado' : 'Beneficio e impacto observado',
        message: aprobado
          ? 'Se notifico la aprobacion al equipo del proyecto.'
          : 'Se notifico la observacion para que el Director corrija y reenvíe.',
      });
      refreshNotifications();
      void securityService.notifyProjectBenefitImpactEvent(proyectoId, {
        proyectoId,
        proyectoNombre: projectName || '',
        evento: aprobado ? 'BENEFICIO_IMPACTO_APROBADO' : 'BENEFICIO_IMPACTO_OBSERVADO',
        estado: aprobado ? 'APROBADO' : 'OBSERVADO',
        mensaje: aprobado
          ? 'El beneficio e impacto fue aprobado por el Gestor.'
          : 'El beneficio e impacto fue observado por el Gestor y requiere correccion.',
        observacion: aprobado ? '' : observacion.trim(),
      }).catch((notifyError) => {
        console.warn('No fue posible notificar la revision del beneficio e impacto:', notifyError);
      });
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
    return null;
  }

  return (
    <>
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
                  disabled={true}
                  placeholder="Observaciones de la revisión del Gestor/Admin."
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
