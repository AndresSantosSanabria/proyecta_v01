import { useState, useRef, useCallback, useEffect } from 'react';
import { Upload, Download, X, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import documentService from '../../../services/documentService';
import { usePermission } from '../../../hooks/usePermission';
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
const ALLOWED_EXTENSIONS = ['.pdf'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
];

const DocumentUpload = ({ proyectoId, tipoDocumento, label, onUploadSuccess }) => {
  const canUpload = usePermission('DOCUMENTO:CARGAR');
  const [state, setState] = useState(STATE.IDLE);
  const [progress, setProgress] = useState(0);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoadingExisting, setIsLoadingExisting] = useState(true);
  const fileInputRef = useRef(null);
  const formatSize = useCallback((bytes) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }, []);

  useEffect(() => {
    let cancelled = false;

    const hydrateExistingDocument = async () => {
      if (!proyectoId || !tipoDocumento) {
        if (!cancelled) setIsLoadingExisting(false);
        return;
      }

      try {
        if (!cancelled) setIsLoadingExisting(true);
        const response = await documentService.listarDocumentos(proyectoId);
        const documentos = response?.data?.documentos ?? response?.data?.data?.documentos ?? response?.data?.data ?? [];
        const existing = Array.isArray(documentos)
          ? documentos.find((doc) => String(doc.tipoDocumento || doc.tipo_documento || '').toUpperCase() === String(tipoDocumento).toUpperCase())
          : null;

        if (cancelled) return;

        if (existing) {
          const bytes = existing.tamanoBytes || existing.tamano_bytes || 0;
          setUploadedFile({
            id: existing.id,
            tipoDocumento: existing.tipoDocumento || existing.tipo_documento || tipoDocumento,
            nombreOriginal: existing.nombreOriginal || existing.nombre_original || existing.nombreAlmacenado || existing.nombre_almacenado || 'Documento cargado',
            nombreAlmacenado: existing.nombreAlmacenado || existing.nombre_almacenado,
            mimeType: existing.mimeType || existing.mime_type,
            tamanoBytes: bytes,
            tamanoFormateado: existing.tamanoFormateado || existing.tamano_formateado || formatSize(bytes),
            urlDescarga: existing.urlDescarga || existing.url_descarga,
            fechaCarga: existing.fechaCarga || existing.fecha_carga,
          });
          setState(STATE.SUCCESS);
        } else {
          setState(STATE.IDLE);
        }
      } catch (error) {
        console.error('Error cargando documento existente:', error);
        if (!cancelled) setState(STATE.IDLE);
      } finally {
        if (!cancelled) setIsLoadingExisting(false);
      }
    };

    hydrateExistingDocument();

    return () => {
      cancelled = true;
    };
  }, [formatSize, proyectoId, tipoDocumento]);

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
    setIsLoadingExisting(false);
  }, []);

  const isIdle = state === STATE.IDLE || state === STATE.DRAGGING;
  const isUploading = state === STATE.UPLOADING;
  const isSuccess = state === STATE.SUCCESS;
  const isError = state === STATE.ERROR;

  if (!canUpload) {
    return null;
  }

  if (isLoadingExisting) {
    return (
      <div className="document-upload">
        <div className="document-upload-header">
          <h3 className="document-upload-title">{label || 'Subir documento'}</h3>
        </div>
        <div className="upload-progress">
          <Loader2 className="upload-progress-spinner animate-spin" size={32} />
          <div className="upload-progress-info">
            <p className="upload-progress-text">Verificando documento existente...</p>
            <div className="progress-bar-container">
              <div className="progress-bar-fill" style={{ width: '100%' }} />
            </div>
          </div>
        </div>
      </div>
    );
  }

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
