import { useEffect, useState } from 'react';
import {
  Save,
  ShieldAlert,
  X,
} from 'lucide-react';
import projectService from '../../services/projectService';
import SpellCheckerTextarea from '../common/SpellCheckerTextarea';
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

const TextareaField = ({ label, name, value, onChange, placeholder, disabled = false, required = false }) => (
  <label className="benefit-impact-form-group">
    <span className="benefit-impact-label">
      {label}
      {required ? ' *' : ''}
    </span>
    <SpellCheckerTextarea
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
  const [error, setError] = useState('');
  const [data, setData] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [editorOpen, setEditorOpen] = useState(false);
  const [forbidden, setForbidden] = useState(false);

  const isEditable = Boolean(data?.editable) && !isGestor;
  const isRequired = Boolean(data?.requerido && data?.editable) && !isGestor;
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
            'No fue posible cargar la información de beneficio e impacto.';
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

      const response = await projectService.saveBenefitImpact(proyectoId, form);
      const saved = unwrap(response);
      setData(saved);
      setForm(toFormState(saved));
      setEditorOpen(false);
      const nextState = String(saved?.estado || 'DILIGENCIADO').toUpperCase();
      emitGlobalToast({
        tone: 'success',
        title: 'Beneficio e impacto guardado',
        message:
          nextState === 'APROBADO'
            ? 'El registro quedó verificado y los involucrados fueron notificados.'
            : nextState === 'OBSERVADO'
              ? 'El registro fue reenviado tras observaciones y se notificó al equipo.'
              : 'El registro fue diligenciado y se notificó al equipo.',
      });
      refreshNotifications();
      if (onSaved) onSaved(saved);
    } catch (saveError) {
      const message =
        saveError?.response?.data?.detail ||
        saveError?.response?.data?.message ||
        saveError?.message ||
        'No fue posible guardar la información.';
      setError(message);
    } finally {
      setSaving(false);
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
                  {saving ? 'Guardando...' : 'Guardar información'}
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
