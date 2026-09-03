import { useEffect, useState } from 'react';
import {
  Plus, Trash2, Save, Eye, EyeOff, GripVertical,
  LoaderCircle, CheckCircle2, Pencil, X, ChevronDown, ChevronUp,
  Type, List, Calendar, Hash,
} from 'lucide-react';
import securityService from '../../services/securityService';
import SpellCheckerTextarea from '../common/SpellCheckerTextarea';
import SpellCheckerInput from '../common/SpellCheckerInput';
import { AutocompleteSelect } from '../common/AutocompleteSelect';
import './ClosureQuestionsPanel.css';

const TIPO_RESPUESTA = [
  { value: 'texto_libre', label: 'Texto Libre', icon: Type },
  { value: 'seleccion_unica', label: 'Seleccion Unica', icon: List },
  { value: 'seleccion_multiple', label: 'Seleccion Multiple', icon: List },
  { value: 'fecha', label: 'Fecha', icon: Calendar },
  { value: 'numero', label: 'Numero', icon: Hash },
];

const ClosureQuestionsPanel = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ texto: '', tipoRespuesta: 'texto_libre', activo: true, opciones: [] });
  const [newOption, setNewOption] = useState('');

  useEffect(() => { loadQuestions(); }, []);

  const getBackendError = (err) => {
    const data = err.response?.data;
    if (data?.detail) return data.detail;
    if (data?.message) return data.message;
    if (typeof data === 'string') return data;
    return 'Error al procesar la solicitud.';
  };

  const loadQuestions = async () => {
    try {
      setLoading(true);
      const res = await securityService.listClosureQuestions();
      const data = res?.data?.data ?? res?.data ?? res;
      setQuestions(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getBackendError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      await securityService.toggleClosureQuestion(id);
      setQuestions((prev) => prev.map((q) => q.id === id ? { ...q, activo: !q.activo } : q));
    } catch (err) {
      setError(getBackendError(err));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Seguro que desea eliminar esta pregunta?')) return;
    try {
      await securityService.deleteClosureQuestion(id);
      setQuestions((prev) => prev.filter((q) => q.id !== id));
      setNotice('Pregunta eliminada.');
      setTimeout(() => setNotice(null), 3000);
    } catch (err) {
      setError(getBackendError(err));
    }
  };

  const openCreate = () => {
    setEditingId(null);
    setForm({ texto: '', tipoRespuesta: 'texto_libre', activo: true, opciones: [] });
    setNewOption('');
    setShowForm(true);
    setError(null);
  };

  const openEdit = (q) => {
    setEditingId(q.id);
    let opts = [];
    if (q.opciones && Array.isArray(q.opciones)) opts = q.opciones;
    setForm({ texto: q.texto, tipoRespuesta: q.tipoRespuesta, activo: q.activo, opciones: opts });
    setNewOption('');
    setShowForm(true);
    setError(null);
  };

  const addOption = () => {
    if (!newOption.trim()) return;
    setForm((f) => ({ ...f, opciones: [...f.opciones, newOption.trim()] }));
    setNewOption('');
  };

  const removeOption = (idx) => {
    setForm((f) => ({ ...f, opciones: f.opciones.filter((_, i) => i !== idx) }));
  };

  const handleSave = async () => {
    if (!form.texto.trim()) {
      setError('El texto de la pregunta es obligatorio.');
      return;
    }
    if ((form.tipoRespuesta === 'seleccion_unica' || form.tipoRespuesta === 'seleccion_multiple') && form.opciones.length === 0) {
      setError('Debe agregar al menos una opcion para preguntas de seleccion.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setNotice(null);
      const payload = {
        texto: form.texto.trim(),
        tipoRespuesta: form.tipoRespuesta,
        activo: form.activo,
        opciones: form.opciones.length > 0 ? form.opciones : null,
      };
      if (editingId) {
        await securityService.updateClosureQuestion(editingId, payload);
        setQuestions((prev) => prev.map((q) => q.id === editingId ? { ...q, ...payload } : q));
        setNotice('Pregunta actualizada.');
      } else {
        const res = await securityService.createClosureQuestion(payload);
        const created = res?.data?.data ?? res?.data ?? res;
        setQuestions((prev) => [...prev, created]);
        setNotice('Pregunta creada.');
      }
      setShowForm(false);
      setEditingId(null);
      setTimeout(() => setNotice(null), 3000);
    } catch (err) {
      setError(getBackendError(err));
    } finally {
      setSaving(false);
    }
  };

  const moveQuestion = async (idx, dir) => {
    const ni = idx + dir;
    if (ni < 0 || ni >= questions.length) return;
    const arr = [...questions];
    [arr[idx], arr[ni]] = [arr[ni], arr[idx]];
    const reordered = arr.map((q, i) => ({ ...q, orden: i + 1 }));
    setQuestions(reordered);
  };

  if (loading) {
    return (
      <div className="cqp-loading">
        <LoaderCircle size={20} className="animate-spin" />
        <span>Cargando preguntas...</span>
      </div>
    );
  }

  const needsOptions = form.tipoRespuesta === 'seleccion_unica' || form.tipoRespuesta === 'seleccion_multiple';

  return (
    <div className="cqp">
      <div className="cqp-header">
        <div className="cqp-header-left">
          <span className="cqp-eyebrow">Configuracion del Formulario de Cierre</span>
          <h2 className="cqp-title">Preguntas del Acta de Cierre</h2>
          <p className="cqp-subtitle">Active o desactive preguntas, agregue nuevas o edite las existentes.</p>
        </div>
        <button type="button" className="cqp-btn cqp-btn-primary" onClick={openCreate}>
          <Plus size={15} /> Nueva Pregunta
        </button>
      </div>

      {notice && <div className="cqp-notice"><CheckCircle2 size={15} /> {notice}</div>}
      {error && <div className="cqp-error">{error}</div>}

      {showForm && (
        <div className="cqp-form-card">
          <div className="cqp-form-header">
            <h3>{editingId ? 'Editar Pregunta' : 'Nueva Pregunta'}</h3>
            <button type="button" className="cqp-btn-icon" onClick={() => { setShowForm(false); setEditingId(null); }}>
              <X size={16} />
            </button>
          </div>
          <div className="cqp-form-body">
            <div className="cqp-form-field">
              <label>Texto de la Pregunta *</label>
              <SpellCheckerTextarea
                className="cqp-input-textarea"
                value={form.texto}
                onChange={(e) => setForm((f) => ({ ...f, texto: e.target.value }))}
                placeholder="Escriba el texto de la pregunta..."
                maxLength={500}
                rows={3}
              />
              <span className="cqp-char-count">{form.texto.length} / 500</span>
            </div>
            <div className="cqp-form-row">
              <div className="cqp-form-field">
                <label>Tipo de Respuesta</label>
                <AutocompleteSelect
                  className="cqp-select"
                  value={form.tipoRespuesta}
                  onChange={(val) => setForm((f) => ({ ...f, tipoRespuesta: val, opciones: [] }))}
                  options={TIPO_RESPUESTA.map((t) => ({ value: t.value, label: t.label }))}
                  placeholder="Seleccionar tipo..."
                  sortAlphabetically={false}
                />
              </div>
              <div className="cqp-form-field">
                <label>Estado</label>
                <label className="cqp-toggle-label">
                  <input
                    type="checkbox"
                    checked={form.activo}
                    onChange={(e) => setForm((f) => ({ ...f, activo: e.target.checked }))}
                  />
                  <span>{form.activo ? 'Activa' : 'Inactiva'}</span>
                </label>
              </div>
            </div>
            {needsOptions && (
              <div className="cqp-form-field">
                <label>Opciones de Respuesta</label>
                <div className="cqp-options-list">
                  {form.opciones.map((opt, i) => (
                    <div key={i} className="cqp-option-chip">
                      <span>{opt}</span>
                      <button type="button" className="cqp-btn-icon-sm" onClick={() => removeOption(i)}>
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
                <div className="cqp-option-add">
                  <SpellCheckerInput
                    className="cqp-input"
                    value={newOption}
                    onChange={(e) => setNewOption(e.target.value)}
                    placeholder="Escriba una opcion y presione Enter"
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addOption(); } }}
                  />
                  <button type="button" className="cqp-btn ctp-btn-secondary" onClick={addOption}>
                    <Plus size={13} /> Agregar
                  </button>
                </div>
              </div>
            )}
          </div>
          <div className="cqp-form-actions">
            <button type="button" className="cqp-btn ctp-btn-secondary" onClick={() => { setShowForm(false); setEditingId(null); }}>
              Cancelar
            </button>
            <button type="button" className="cqp-btn cqp-btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? <LoaderCircle size={14} className="animate-spin" /> : <Save size={14} />}
              {editingId ? 'Guardar Cambios' : 'Crear Pregunta'}
            </button>
          </div>
        </div>
      )}

      <div className="cqp-list">
        {questions.length === 0 && (
          <div className="cqp-empty">
            <p>No hay preguntas configuradas. Cree una nueva pregunta para comenzar.</p>
          </div>
        )}
        {questions.map((q, idx) => {
          const tipo = TIPO_RESPUESTA.find((t) => t.value === q.tipoRespuesta);
          const TipoIcon = tipo?.icon || Type;
          return (
            <div key={q.id} className={`cqp-question-card ${q.activo ? 'active' : 'inactive'}`}>
              <div className="cqp-qg-drag">
                <button type="button" onClick={() => moveQuestion(idx, -1)} disabled={idx === 0}><ChevronUp size={14} /></button>
                <span className="cqp-qg-num">{q.orden || idx + 1}</span>
                <button type="button" onClick={() => moveQuestion(idx, 1)} disabled={idx === questions.length - 1}><ChevronDown size={14} /></button>
              </div>
              <div className="cqp-qg-body">
                <div className="cqp-qg-text">{q.texto}</div>
                <div className="cqp-qg-meta">
                  <span className="cqp-chip"><TipoIcon size={11} /> {tipo?.label || q.tipoRespuesta}</span>
                  {q.opciones && Array.isArray(q.opciones) && q.opciones.length > 0 && (
                    <span className="cqp-chip-muted">{q.opciones.length} opciones</span>
                  )}
                </div>
              </div>
              <div className="cqp-qg-actions">
                <button
                  type="button"
                  className={`cqp-toggle-btn ${q.activo ? 'on' : 'off'}`}
                  onClick={() => handleToggle(q.id)}
                  title={q.activo ? 'Desactivar' : 'Activar'}
                >
                  {q.activo ? <Eye size={15} /> : <EyeOff size={15} />}
                </button>
                <button type="button" className="cqp-btn-icon" onClick={() => openEdit(q)} title="Editar">
                  <Pencil size={14} />
                </button>
                <button type="button" className="cqp-btn-icon cqp-btn-danger" onClick={() => handleDelete(q.id)} title="Eliminar">
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ClosureQuestionsPanel;
