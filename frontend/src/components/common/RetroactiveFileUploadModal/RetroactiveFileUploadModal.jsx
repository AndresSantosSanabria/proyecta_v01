import { useState, useRef, useEffect } from 'react';
import { X, Upload, FileText, AlertCircle } from 'lucide-react';
import './RetroactiveFileUploadModal.css';

const MAX_SIZE_MB = 20;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

const RetroactiveFileUploadModal = ({ open, entregableNombre, currentFile, onConfirm, onClose }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (open) {
      setSelectedFile(null);
      setErrorMessage('');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }, [open, entregableNombre]);

  if (!open) return null;

  const validateFile = (file) => {
    if (!file) return 'No se selecciono ningun archivo.';
    const ext = '.' + file.name.split('.').pop().toLowerCase();
    if (ext !== '.pdf') return 'Solo se permiten archivos PDF para soporte.';
    if (file.size > MAX_SIZE_BYTES) return `El archivo excede el tamano maximo de ${MAX_SIZE_MB} MB.`;
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

  const handleConfirm = () => {
    if (!selectedFile) return;
    onConfirm(selectedFile);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file) {
      const err = validateFile(file);
      if (err) {
        setErrorMessage(err);
        return;
      }
      setSelectedFile(file);
      setErrorMessage('');
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  return (
    <div className="retroactive-upload-overlay" onClick={onClose}>
      <div className="retroactive-upload-modal" onClick={(e) => e.stopPropagation()}>
        <div className="retroactive-upload-header">
          <div>
            <h3>Cargar soporte - {entregableNombre || 'Entregable'}</h3>
            <p className="retroactive-upload-subtitle">Archivo de soporte obligatorio (entregable retroactivo)</p>
          </div>
          <button type="button" className="retroactive-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="retroactive-upload-body">
          <div
            className="retroactive-dropzone"
            onClick={() => fileInputRef.current?.click()}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileSelect}
              accept=".pdf"
              style={{ display: 'none' }}
            />
            <Upload size={32} className="retroactive-dropzone-icon" />
            <p className="retroactive-dropzone-text">
              {selectedFile ? selectedFile.name : 'Arrastra un PDF o haz clic para seleccionar'}
            </p>
            <p className="retroactive-dropzone-hint">Solo PDF . Max. {MAX_SIZE_MB} MB</p>
          </div>

          {errorMessage && (
            <div className="retroactive-error-inline">
              <AlertCircle size={14} />
              <span>{errorMessage}</span>
            </div>
          )}

          {selectedFile && (
            <div className="retroactive-file-info">
              <FileText size={16} />
              <span>{selectedFile.name}</span>
              <span className="retroactive-file-size">{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</span>
            </div>
          )}
        </div>

        <div className="retroactive-upload-actions">
          <button type="button" className="retroactive-cancel-btn" onClick={onClose}>
            Cancelar
          </button>
          <button
            type="button"
            className="retroactive-confirm-btn"
            onClick={handleConfirm}
            disabled={!selectedFile}
          >
            <Upload size={15} />
            Cargar soporte
          </button>
        </div>
      </div>
    </div>
  );
};

export default RetroactiveFileUploadModal;
