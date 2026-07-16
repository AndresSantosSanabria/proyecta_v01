import { useState } from 'react';
import { Calendar, FileText, Upload, X, LoaderCircle } from 'lucide-react';
import './ModificarFechaModal.css';

export default function ModificarFechaModal({ entregable, proyectoId, onClose, onSaved }) {
  const [nuevaFecha, setNuevaFecha] = useState('');
  const [justificacion, setJustificacion] = useState('');
  const [archivo, setArchivo] = useState(null);
  const [archivoNombre, setArchivoNombre] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const fechaMinima = entregable.fechaInicio || '';
  const fechaActual = entregable.fechaEntrega || '';

  const puedeGuardar = () => {
    if (!nuevaFecha) return false;
    if (nuevaFecha === fechaActual) return false;
    if (!justificacion.trim()) return false;
    if (!archivo) return false;
    return true;
  };

  const handleArchivoChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        setError('Solo se permiten archivos PDF.');
        setArchivo(null);
        setArchivoNombre('');
        return;
      }
      setError('');
      setArchivo(file);
      setArchivoNombre(file.name);
    }
  };

  const handleGuardar = async () => {
    if (!puedeGuardar()) return;
    setError('');
    setEnviando(true);

    try {
      const formData = new FormData();
      const request = { nuevaFecha, justificacion: justificacion.trim() };
      formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
      formData.append('evidencia', archivo);

      const apiClient = (await import('../../../api/axiosConfig')).default;
      const { data } = await apiClient.post(
        `/proyectos/${proyectoId}/entregables/${entregable.id}/cambiar-fecha`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );

      onSaved(data?.data);
      onClose();
    } catch (err) {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || err.message || 'Error al cambiar la fecha';
      setError(msg);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="mfm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="mfm-header">
          <h3><Calendar size={16} /> Modificar Fecha Límite</h3>
          <button className="mfm-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="mfm-body">
          <div className="mfm-info">
            <strong>{entregable.nombre}</strong>
            <span>Fecha actual: <b>{fechaActual || 'No definida'}</b></span>
          </div>

          <div className="mfm-field">
            <label>Nueva fecha límite</label>
            <input
              type="date"
              value={nuevaFecha}
              min={fechaMinima}
              onChange={(e) => setNuevaFecha(e.target.value)}
            />
            {nuevaFecha && nuevaFecha === fechaActual && (
              <span className="mfm-hint mfm-hint--warn">La fecha debe ser diferente a la actual</span>
            )}
          </div>

          <div className="mfm-field">
            <label>Justificación del cambio</label>
            <textarea
              rows={4}
              value={justificacion}
              onChange={(e) => setJustificacion(e.target.value)}
              placeholder="Describa el motivo del aplazamiento o modificación de la fecha..."
            />
          </div>

          <div className="mfm-field">
            <label>PDF de soporte</label>
            <div className="mfm-file-input">
              <input
                type="file"
                accept=".pdf,application/pdf"
                id="mfm-pdf"
                onChange={handleArchivoChange}
              />
              <label htmlFor="mfm-pdf" className="mfm-file-label">
                <Upload size={14} /> {archivoNombre || 'Seleccionar archivo PDF'}
              </label>
            </div>
          </div>

          {error && <div className="mfm-error"><FileText size={14} /> {error}</div>}
        </div>

        <div className="mfm-footer">
          <button className="mfm-btn mfm-btn--cancel" onClick={onClose}>Cancelar</button>
          <button
            className="mfm-btn mfm-btn--save"
            onClick={handleGuardar}
            disabled={!puedeGuardar() || enviando}
          >
            {enviando ? <LoaderCircle size={16} className="spin" /> : <FileText size={16} />}
            {enviando ? 'Guardando...' : 'Guardar Cambios'}
          </button>
        </div>
      </div>
    </div>
  );
}
