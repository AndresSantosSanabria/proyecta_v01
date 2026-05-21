import React, { useState, useRef, useCallback } from 'react';
import { Upload, Download, X, FileText, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import documentService from '../../../services/documentService';
import './DocumentUpload.css';

const STATE = {
  IDLE: 'idle',
  DRAGGING: 'dragging',
  UPLOADING: 'uploading',
  SUCCESS: 'success',
  ERROR: 'error',
};

const MAX_SIZE_MB = 20;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['.pdf', '.png', '.jpg', '.jpeg', '.doc', '.docx', '.xls', '.xlsx'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
];

const DocumentUpload = ({ proyectoId, tipoDocumento, label, onUploadSuccess }) => {
  const [state, setState] = useState(STATE.IDLE);
  const [progress, setProgress] = useState(0);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const fileInputRef = useRef(null);

  const validateFile = useCallback((file) => {
    if (!file) return 'No se seleccionó ningún archivo.';

    const extension = '.' + file.name.split('.').pop().toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      return `Extensión no permitida: ${extension}. Tipos aceptados: ${ALLOWED_EXTENSIONS.join(', ')}`;
    }

    if (!ALLOWED_MIME_TYPES.includes(file.type)) {
      return `Tipo de archivo no válido: ${file.type || 'desconocido'}.`;
    }

    if (file.size > MAX_SIZE_BYTES) {
      return `El archivo excede el tamaño máximo de ${MAX_SIZE_MB} MB.`;
    }

    return null;
  }, []);

  const handleUpload = useCallback(async (file) => {
    const validationError = validateFile(file);
    if (validationError) {
      setState(STATE.ERROR);
      setErrorMessage(validationError);
      return;
    }

    setState(STATE.UPLOADING);
    setProgress(0);
    setErrorMessage('');

    try {
      const result = await documentService.cargarDocumento(
        proyectoId,
        tipoDocumento,
        file,
        (percent) => setProgress(percent)
      );

      setUploadedFile(result.data);
      setState(STATE.SUCCESS);
      setProgress(100);

      if (onUploadSuccess) {
        onUploadSuccess(result.data);
      }
    } catch (error) {
      const msg = error.response?.data?.message || error.response?.data?.detail || 'Error al subir el documento. Inténtalo de nuevo.';
      setState(STATE.ERROR);
      setErrorMessage(msg);
    }
  }, [proyectoId, tipoDocumento, validateFile, onUploadSuccess]);

  const handleFileChange = useCallback((e) => {
    const file = e.target.files?.[0];
    if (file) {
      handleUpload(file);
    }
    e.target.value = '';
  }, [handleUpload]);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setState(STATE.IDLE);

    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleUpload(file);
    }
  }, [handleUpload]);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setState(STATE.DRAGGING);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setState(STATE.IDLE);
  }, []);

  const handleDownload = useCallback(async () => {
    try {
      const blob = await documentService.descargarDocumento(proyectoId, tipoDocumento);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', uploadedFile?.nombreOriginal || `${tipoDocumento.toLowerCase()}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      setState(STATE.ERROR);
      setErrorMessage('Error al descargar el documento.');
    }
  }, [proyectoId, tipoDocumento, uploadedFile]);

  const handleReset = useCallback(() => {
    setState(STATE.IDLE);
    setProgress(0);
    setUploadedFile(null);
    setErrorMessage('');
  }, []);

  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const isIdle = state === STATE.IDLE || state === STATE.DRAGGING;
  const isUploading = state === STATE.UPLOADING;
  const isSuccess = state === STATE.SUCCESS;
  const isError = state === STATE.ERROR;

  return (
    <div className="document-upload">
      <div className="document-upload-header">
        <h3 className="document-upload-title">{label || 'Subir documento'}</h3>
        {isSuccess && uploadedFile && (
          <span className="document-upload-badge success">
            <CheckCircle2 size={14} /> Cargado
          </span>
        )}
      </div>

      {isIdle && (
        <div
          className={`upload-dropzone ${state === STATE.DRAGGING ? 'drag-over' : ''}`}
          onClick={() => fileInputRef.current?.click()}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter') fileInputRef.current?.click(); }}
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept={ALLOWED_EXTENSIONS.join(',')}
            style={{ display: 'none' }}
          />
          <Upload className="upload-dropzone-icon" size={40} />
          <p className="upload-dropzone-text">Arrastra y suelta un archivo aquí</p>
          <p className="upload-dropzone-subtext">o haz clic para seleccionar</p>
          <p className="upload-dropzone-hint">
            PDF, PNG, JPG, DOC, XLS · Máx. {MAX_SIZE_MB} MB
          </p>
        </div>
      )}

      {isUploading && (
        <div className="upload-progress">
          <Loader2 className="upload-progress-spinner animate-spin" size={32} />
          <div className="upload-progress-info">
            <p className="upload-progress-text">Subiendo documento...</p>
            <div className="progress-bar-container">
              <div
                className="progress-bar-fill"
                style={{ width: `${progress}%` }}
              />
            </div>
            <span className="progress-bar-percent">{progress}%</span>
          </div>
        </div>
      )}

      {isSuccess && uploadedFile && (
        <div className="upload-success">
          <div className="upload-success-icon">
            <CheckCircle2 size={28} />
          </div>
          <div className="upload-success-details">
            <p className="upload-success-name" title={uploadedFile.nombreOriginal}>
              {uploadedFile.nombreOriginal}
            </p>
            <p className="upload-success-meta">
              {uploadedFile.tamanoFormateado} · {uploadedFile.fechaCarga}
            </p>
          </div>
          <div className="upload-success-actions">
            <button className="btn-download" onClick={handleDownload} title="Descargar">
              <Download size={16} />
            </button>
            <button className="btn-reset" onClick={handleReset} title="Reemplazar">
              <Upload size={16} />
            </button>
          </div>
        </div>
      )}

      {isError && (
        <div className="upload-error">
          <AlertCircle size={20} className="upload-error-icon" />
          <div className="upload-error-content">
            <p className="upload-error-title">Error al cargar</p>
            <p className="upload-error-message">{errorMessage}</p>
          </div>
          <button className="btn-error-close" onClick={handleReset} title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}
    </div>
  );
};

export default DocumentUpload;
