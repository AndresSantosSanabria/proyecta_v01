import { useState, useRef } from 'react';
import { X, Upload, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import projectService from '../../../services/projectService';
import { usePermission } from '../../../hooks/usePermission';
import './EvidenceUpload.css';

const STATE = {
  IDLE: 'idle',
  UPLOADING: 'uploading',
  SUCCESS: 'success',
  ERROR: 'error',
};

const MAX_SIZE_MB = 20;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

const EvidenceUpload = ({ proyectoId, entregableId, mode = 'cargar', onClose, onSuccess }) => {
  const canUploadEvidence = usePermission('EVIDENCIA:CARGAR');
  const [state, setState] = useState(STATE.IDLE);
  const [progress, setProgress] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);

  if (!canUploadEvidence) {
    return null;
  }

  const isCorrectionMode = mode === 'subsanar';
  const title = isCorrectionMode
    ? `Subsanar Evidencia - Entregable #${entregableId}`
    : `Subir Evidencia - Entregable #${entregableId}`;
  const submitLabel = isCorrectionMode ? 'Cargar Subsanacion' : 'Subir Evidencia';
  const successMessage = isCorrectionMode
    ? 'Subsanacion cargada exitosamente. Queda nuevamente en revision del gestor.'
    : 'Evidencia cargada exitosamente. Queda pendiente de aprobación del gestor.';

  const validateFile = (file) => {
    if (!file) return 'No se seleccionó ningún archivo.';
    const ext = '.' + file.name.split('.').pop().toLowerCase();
    if (ext !== '.pdf') return 'Solo se permiten archivos PDF para evidencias.';
    if (file.size > MAX_SIZE_BYTES) return `El archivo excede el tamaño máximo de ${MAX_SIZE_MB} MB.`;
    return null;
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      const err = validateFile(file);
      if (err) {
        setErrorMessage(err);
        return;
      }
      setSelectedFile(file);
      setErrorMessage('');
    }
    e.target.value = '';
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    const validationError = validateFile(selectedFile);
    if (validationError) {
      setState(STATE.ERROR);
      setErrorMessage(validationError);
      return;
    }

    setState(STATE.UPLOADING);
    setProgress(0);

    try {
      const now = new Date();
      const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
        .toISOString()
        .split('T')[0];

      await projectService.uploadEvidencia(
        proyectoId,
        entregableId,
        selectedFile,
        localDate,
        (percent) => setProgress(percent)
      );

      setState(STATE.SUCCESS);
      setProgress(100);

      setTimeout(() => {
        if (onSuccess) onSuccess();
      }, 1200);
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'Error al subir la evidencia.';
      setState(STATE.ERROR);
      setErrorMessage(msg);
      console.error('Upload error:', error.response?.data);
    }
  };

  return (
    <div className="evidence-upload-overlay" onClick={onClose}>
      <div className="evidence-upload-modal" onClick={(e) => e.stopPropagation()}>
        <div className="evidence-upload-header">
          <h3>{title}</h3>
          <h3>Subir Evidencia — Entregable #{entregableId}</h3>
          <button type="button" className="evidence-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="evidence-upload-body">
          {state === STATE.IDLE && (
            <>
              <div
                className="evidence-dropzone"
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileSelect}
                  accept=".pdf"
                  style={{ display: 'none' }}
                />
                <Upload size={36} className="evidence-dropzone-icon" />
                <p className="evidence-dropzone-text">
                  {selectedFile ? selectedFile.name : 'Seleccionar archivo PDF'}
                </p>
                <p className="evidence-dropzone-hint">Solo PDF · Máx. {MAX_SIZE_MB} MB</p>
              </div>

              {errorMessage && (
                <div className="evidence-error-inline">
                  <AlertCircle size={14} />
                  <span>{errorMessage}</span>
                </div>
              )}

              <button
                type="button"
                className="evidence-submit-btn"
                onClick={handleUpload}
                disabled={!selectedFile}
              >
                <Upload size={16} /> {submitLabel}
              </button>
            </>
          )}

          {state === STATE.UPLOADING && (
            <div className="evidence-progress">
              <Loader2 className="animate-spin" size={32} />
              <div className="evidence-progress-info">
                <p>Subiendo evidencia...</p>
                <div className="progress-bar-container">
                  <div className="progress-bar-fill" style={{ width: `${progress}%` }} />
                </div>
                <span className="progress-bar-percent">{progress}%</span>
              </div>
            </div>
          )}

          {state === STATE.SUCCESS && (
            <div className="evidence-success">
              <CheckCircle2 size={48} className="evidence-success-icon" />
              <p>{successMessage}</p>
            </div>
          )}

          {state === STATE.ERROR && (
            <div className="evidence-error">
              <AlertCircle size={24} />
              <p>{errorMessage}</p>
              <button type="button" className="evidence-retry-btn" onClick={() => { setState(STATE.IDLE); setErrorMessage(''); }}>
                Reintentar
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default EvidenceUpload;
