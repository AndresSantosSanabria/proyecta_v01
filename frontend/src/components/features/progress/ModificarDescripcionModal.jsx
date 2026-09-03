import { useState } from 'react';
import { FileEdit, FileText, Upload, X, LoaderCircle } from 'lucide-react';
import SpellCheckerTextarea from '../../common/SpellCheckerTextarea';
import './ModificarDescripcionModal.css';

export default function ModificarDescripcionModal({ entregable, proyectoId, onClose, onSaved }) {
  const [nuevaDescripcion, setNuevaDescripcion] = useState(entregable?.descripcion || '');
  const [justificacion, setJustificacion] = useState('');
  const [archivo, setArchivo] = useState(null);
  const [archivoNombre, setArchivoNombre] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const descripcionActual = entregable?.descripcion || '';

  const puedeGuardar = () => {
    if (!nuevaDescripcion.trim()) return false;
    if (nuevaDescripcion.trim() === descripcionActual.trim()) return false;
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
      const request = {
        nuevaDescripcion: nuevaDescripcion.trim(),
        justificacion: justificacion.trim(),
      };
      formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
      formData.append('evidencia', archivo);

      const apiClient = (await import('../../../api/axiosConfig')).default;
      const { data } = await apiClient.post(
        `/proyectos/${proyectoId}/entregables/${entregable.id}/cambiar-descripcion`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );

      onSaved(data?.data);
      onClose();
    } catch (err) {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || err.message || 'Error al cambiar la descripción';
      setError(msg);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="mdm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="mdm-header">
          <h3><FileEdit size={16} /> Modificar Descripción de Entregable</h3>
          <button className="mdm-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="mdm-body">
          <div className="mdm-info">
            <strong>{entregable?.nombre}</strong>
            <span>Descripción actual: <b>{descripcionActual || 'Sin descripción'}</b></span>
          </div>

          <div className="mdm-field">
            <label>Nueva Descripción</label>
            <SpellCheckerTextarea
              rows={3}
              value={nuevaDescripcion}
              onChange={(e) => setNuevaDescripcion(e.target.value)}
              placeholder="Ingrese la nueva descripción del entregable..."
            />
            {nuevaDescripcion.trim() && nuevaDescripcion.trim() === descripcionActual.trim() && (
              <span className="mdm-hint mdm-hint--warn">La nueva descripción debe ser diferente a la actual</span>
            )}
          </div>

          <div className="mdm-field">
            <label>Justificación del Cambio</label>
            <SpellCheckerTextarea
              rows={3}
              value={justificacion}
              onChange={(e) => setJustificacion(e.target.value)}
              placeholder="Describa el motivo por el cual se realiza la modificación de la descripción..."
            />
          </div>

          <div className="mdm-field">
            <label>PDF de Soporte (Obligatorio)</label>
            <div className="mdm-file-input">
              <input
                type="file"
                accept=".pdf,application/pdf"
                id="mdm-pdf"
                onChange={handleArchivoChange}
              />
              <label htmlFor="mdm-pdf" className="mdm-file-label">
                <Upload size={14} /> {archivoNombre || 'Seleccionar archivo PDF'}
              </label>
            </div>
          </div>

          {error && <div className="mdm-error"><FileText size={14} /> {error}</div>}
        </div>

        <div className="mdm-footer">
          <button className="mdm-btn mdm-btn--cancel" onClick={onClose}>Cancelar</button>
          <button
            className="mdm-btn mdm-btn--save"
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
