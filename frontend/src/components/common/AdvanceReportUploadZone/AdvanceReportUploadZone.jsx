import { useState, useCallback, useRef, useEffect } from 'react';
import { Upload, FileText, CheckCircle2, AlertTriangle, Clock, X } from 'lucide-react';
import advanceReportService from '../../../services/advanceReportService';
import { emitToast } from '../../../utils/feedback';
import './AdvanceReportUploadZone.css';

const ALLOWED_EXTENSIONS = ['pdf', 'pptx'];
const MAX_SIZE_MB = 20;

const AdvanceReportUploadZone = ({ projectId }) => {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const dropRef = useRef(null);

  const loadStatus = useCallback(async () => {
    if (!projectId) return;
    try {
      setLoading(true);
      const data = await advanceReportService.getStatus(projectId);
      setStatus(data);
    } catch (err) {
      console.error('Error loading advance report status:', err);
      setStatus(null);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const validateFile = (file) => {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      emitToast({ title: 'Extensión no permitida', message: `Solo se permiten: ${ALLOWED_EXTENSIONS.join(', ')}`, tone: 'error' });
      return false;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      emitToast({ title: 'Archivo demasiado grande', message: `El tamaño máximo es ${MAX_SIZE_MB} MB.`, tone: 'error' });
      return false;
    }
    return true;
  };

  const handleUpload = useCallback(async (file) => {
    if (!file || !projectId) return;
    if (!validateFile(file)) return;
    try {
      setUploading(true);
      const result = await advanceReportService.uploadReport(projectId, file);
      setStatus(result);
      emitToast({ title: 'Informe cargado', message: `${file.name} subido exitosamente.`, tone: 'success' });
    } catch (err) {
      console.error('Error uploading advance report:', err);
      const msg = err?.response?.data?.message || 'No fue posible cargar el informe.';
      emitToast({ title: 'Error al cargar', message: msg, tone: 'error' });
    } finally {
      setUploading(false);
    }
  }, [projectId]);

  const handleFileInput = (e) => {
    const file = e.target.files?.[0];
    if (file) handleUpload(file);
    e.target.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleUpload(file);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragOver(false);
  };

  if (loading) {
    return (
      <div className="aru-container aru-container--loading">
        <div className="aru-loading-text">Verificando informe de avance...</div>
      </div>
    );
  }

  if (!status) return null;

  const { isPending, isUploaded, dueDate, isOverdue, daysUntilDue, fileName, uploadedAt } = status;

  if (!isPending) {
    return (
      <div className="aru-container aru-container--uploaded">
        <div className="aru-success">
          <CheckCircle2 size={18} />
          <div className="aru-success__text">
            <strong>Informe de avance cargado</strong>
            <span>{fileName} — {uploadedAt}</span>
          </div>
        </div>
        <button className="aru-reupload-btn" onClick={() => fileInputRef.current?.click()}>
          <Upload size={14} />
          Reemplazar
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.pptx"
          onChange={handleFileInput}
          style={{ display: 'none' }}
        />
      </div>
    );
  }

  return (
    <div className="aru-container aru-container--pending">
      <div className={`aru-due-info ${isOverdue ? 'aru-due-info--overdue' : ''}`}>
        <Clock size={14} />
        <span>
          {isOverdue
            ? `Vencido hace ${Math.abs(daysUntilDue)} día(s) — fecha límite: ${dueDate}`
            : `Faltan ${daysUntilDue} día(s) — fecha límite: ${dueDate}`}
        </span>
      </div>

      <div
        ref={dropRef}
        className={`aru-dropzone ${dragOver ? 'aru-dropzone--active' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current?.click()}
      >
        {uploading ? (
          <div className="aru-dropzone__loading">
            <div className="aru-spinner" />
            <span>Subiendo informe...</span>
          </div>
        ) : (
          <>
            <Upload size={20} className="aru-dropzone__icon" />
            <span className="aru-dropzone__label">
              Arrastra el informe de avance aquí o <strong>selecciona archivo</strong>
            </span>
            <span className="aru-dropzone__hint">
              PDF o PPTX — Máximo {MAX_SIZE_MB} MB
            </span>
          </>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.pptx"
        onChange={handleFileInput}
        style={{ display: 'none' }}
      />
    </div>
  );
};

export default AdvanceReportUploadZone;
