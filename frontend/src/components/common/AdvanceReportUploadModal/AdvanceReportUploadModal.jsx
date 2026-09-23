import { useState, useCallback, useRef } from 'react';
import { Upload, FileText, X, LoaderCircle, Clock, CheckCircle2 } from 'lucide-react';
import advanceReportService from '../../../services/advanceReportService';
import { emitToast } from '../../../utils/feedback';
import './AdvanceReportUploadModal.css';

const ALLOWED_EXTENSIONS = ['pdf', 'pptx'];
const MAX_SIZE_MB = 20;

const AdvanceReportUploadModal = ({ project, isOpen, onClose, onUploaded }) => {
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  const fileInputRef = useRef(null);

  const validateFile = (file) => {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      emitToast({
        title: 'Extensión no permitida',
        message: `Solo se permiten: ${ALLOWED_EXTENSIONS.join(', ')}`,
        tone: 'error',
      });
      return false;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      emitToast({
        title: 'Archivo demasiado grande',
        message: `El tamaño máximo es ${MAX_SIZE_MB} MB.`,
        tone: 'error',
      });
      return false;
    }
    return true;
  };

  const handleUpload = useCallback(async (file) => {
    if (!file || !project?.projectId) return;
    if (!validateFile(file)) return;
    try {
      setUploading(true);
      await advanceReportService.uploadReport(project.projectId, file);
      setUploadedFile(file.name);
      emitToast({
        title: 'Informe cargado',
        message: `${file.name} subido exitosamente.`,
        tone: 'success',
      });
      onUploaded?.(project.projectId);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.detail || 'No fue posible cargar el informe.';
      emitToast({ title: 'Error al cargar', message: msg, tone: 'error' });
    } finally {
      setUploading(false);
    }
  }, [project?.projectId, onUploaded]);

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

  if (!isOpen || !project) return null;

  const dueDate = project.dueDate;
  const isOverdue = project.isOverdue;

  return (
    <div className="arum-overlay" onClick={onClose}>
      <div className="arum-modal" onClick={(e) => e.stopPropagation()}>
        <div className="arum-header">
          <div className="arum-header__icon">
            <Upload size={20} />
          </div>
          <div className="arum-header__text">
            <h3>Cargar informe de avance</h3>
            <p>{project.projectName || project.projectId}</p>
          </div>
          <button className="arum-close" onClick={onClose} aria-label="Cerrar">
            <X size={16} />
          </button>
        </div>

        <div className="arum-body">
          {dueDate && (
            <div className={`arum-due ${isOverdue ? 'arum-due--overdue' : ''}`}>
              <Clock size={14} />
              <span>
                {isOverdue
                  ? `Vencido desde ${dueDate}`
                  : `Fecha límite: ${dueDate}`}
                {project.periodo ? ` — ${project.periodo}` : ''}
              </span>
            </div>
          )}

          {uploadedFile ? (
            <div className="arum-success">
              <CheckCircle2 size={18} />
              <div>
                <strong>Informe cargado</strong>
                <span>{uploadedFile}</span>
              </div>
            </div>
          ) : (
            <div
              className={`arum-dropzone ${dragOver ? 'arum-dropzone--active' : ''}`}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploading ? (
                <div className="arum-loading">
                  <LoaderCircle size={22} className="arum-spin" />
                  <span>Subiendo informe...</span>
                </div>
              ) : (
                <>
                  <Upload size={22} className="arum-dropzone__icon" />
                  <span className="arum-dropzone__label">
                    Arrastra el informe aquí o <strong>selecciona archivo</strong>
                  </span>
                  <span className="arum-dropzone__hint">
                    PDF o PPTX — Máximo {MAX_SIZE_MB} MB
                  </span>
                </>
              )}
            </div>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.pptx"
            onChange={handleFileInput}
            style={{ display: 'none' }}
          />
        </div>

        <div className="arum-footer">
          <button className="arum-btn arum-btn--secondary" onClick={onClose}>
            {uploadedFile ? 'Cerrar' : 'Cancelar'}
          </button>
          {uploadedFile && (
            <button
              className="arum-btn arum-btn--primary"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
            >
              <FileText size={14} />
              Reemplazar
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdvanceReportUploadModal;
