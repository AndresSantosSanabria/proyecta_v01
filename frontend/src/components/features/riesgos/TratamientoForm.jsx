import { useState } from 'react';
import { Loader2, Plus, Trash2 } from 'lucide-react';
import riskService from '../../../services/riskService';

const MAX_FILES = 10;
const MAX_SIZE_MB = 20;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

const formatBytes = (bytes) => {
  if (!bytes) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const TratamientoForm = ({ proyectoId, riesgoId, onSuccess, onCancel }) => {
  const [comentario, setComentario] = useState('');
  const [archivos, setArchivos] = useState([]);
  const [saving, setSaving] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const newFiles = Array.from(e.target.files || []);
    const totalAfter = archivos.length + newFiles.length;

    if (totalAfter > MAX_FILES) {
      setError(`No puedes adjuntar más de ${MAX_FILES} archivos.`);
      return;
    }

    const oversized = newFiles.find((f) => f.size > MAX_SIZE_BYTES);
    if (oversized) {
      setError(`El archivo "${oversized.name}" excede el tamaño máximo de ${MAX_SIZE_MB} MB.`);
      return;
    }

    const invalidType = newFiles.find((f) => !f.name.toLowerCase().endsWith('.pdf') && f.type !== 'application/pdf');
    if (invalidType) {
      setError(`El archivo "${invalidType.name}" no es un PDF válido.`);
      return;
    }

    setError('');
    setArchivos((prev) => [...prev, ...newFiles]);
    e.target.value = '';
  };

  const removeFile = (index) => {
    setArchivos((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!comentario.trim()) {
      setError('El comentario del tratamiento es obligatorio.');
      return;
    }

    setSaving(true);
    setError('');
    setUploadProgress(0);

    try {
      await riskService.crearTratamiento(
        proyectoId,
        riesgoId,
        comentario.trim(),
        archivos.length > 0 ? archivos : null,
        (percent) => setUploadProgress(percent)
      );
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err?.response?.data?.detail || err?.response?.data?.message || 'No fue posible guardar el tratamiento.');
    } finally {
      setSaving(false);
      setUploadProgress(0);
    }
  };

  return (
    <form className="tratamiento-form" onSubmit={handleSubmit}>
      {error && (
        <div className="riesgos-alert solution-alert">
          {error}
        </div>
      )}

      <label className="field-label">
        Comentario / Acción de mitigación <span className="required">*</span>
      </label>
      <textarea
        className="risk-textarea"
        rows={3}
        placeholder="Describe la acción de mitigación realizada..."
        value={comentario}
        onChange={(e) => setComentario(e.target.value)}
        required
      />

      <label className="field-label">
        Archivos PDF {archivos.length > 0 && `(${archivos.length}/${MAX_FILES})`}
      </label>
      <label className="file-picker">
        <span>Elegir archivos</span>
        <input
          type="file"
          accept=".pdf,application/pdf"
          multiple
          onChange={handleFileChange}
          disabled={archivos.length >= MAX_FILES}
        />
      </label>

      <div className="pending-files">
        {archivos.length === 0 ? (
          <div className="empty-box">Aún no has seleccionado archivos.</div>
        ) : (
          archivos.map((file, index) => (
            <article className="pending-file" key={`${file.name}_${file.size}_${file.lastModified}`}>
              <div className="solution-item-copy">
                <strong>{file.name}</strong>
                <span>{formatBytes(file.size)}</span>
              </div>
              <button
                type="button"
                className="btn-danger compact"
                onClick={() => removeFile(index)}
                title="Quitar archivo"
              >
                <Trash2 size={14} />
              </button>
            </article>
          ))
        )}
      </div>

      {saving && uploadProgress > 0 && (
        <div className="upload-progress-bar">
          <div className="upload-progress-fill" style={{ width: `${uploadProgress}%` }} />
          <span>{uploadProgress}%</span>
        </div>
      )}

      <div className="form-actions">
        <button type="button" className="btn-secondary" onClick={onCancel} disabled={saving}>
          Cancelar
        </button>
        <button type="submit" className="btn-primary" disabled={saving || !comentario.trim()}>
          {saving ? <><Loader2 size={16} className="animate-spin" /> Guardando...</> : <><Plus size={16} /> Guardar tratamiento</>}
        </button>
      </div>
    </form>
  );
};

export default TratamientoForm;
