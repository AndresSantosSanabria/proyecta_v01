import React, { useRef, useState } from 'react';
import { Upload, FileText, X } from 'lucide-react';

const Paso6GestionDocumental = ({ data, onChange, errors }) => {
  const [dragging, setDragging] = useState(null);

  const handleFileSelect = (field) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf';
    input.onchange = (e) => {
      const file = e.target.files[0];
      if (file) {
        onChange({ [field]: file });
      }
    };
    input.click();
  };

  const handleRemove = (field) => {
    onChange({ [field]: null });
  };

  const renderFileUpload = (field, label, required = false, note = null) => {
    const file = data[field];
    const hasError = errors[field];

    return (
      <div className={`document-upload-card ${hasError ? 'upload-error' : ''}`}>
        <div className="upload-card-header">
          <h4>{label} {required && <span className="required-mark">*</span>}</h4>
          {note && <p className="upload-note">{note}</p>}
        </div>

        {file ? (
          <div className="file-selected">
            <FileText size={20} />
            <span className="file-name">{file.name}</span>
            <span className="file-size">({(file.size / 1024).toFixed(1)} KB)</span>
            <button type="button" className="btn-icon-danger" onClick={() => handleRemove(field)}>
              <X size={16} />
            </button>
          </div>
        ) : (
          <div
            className={`drop-zone ${dragging === field ? 'dragging' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragging(field); }}
            onDragLeave={() => setDragging(null)}
            onDrop={(e) => {
              e.preventDefault();
              setDragging(null);
              const f = e.dataTransfer.files[0];
              if (f && f.type === 'application/pdf') {
                onChange({ [field]: f });
              }
            }}
            onClick={() => handleFileSelect(field)}
          >
            <Upload size={24} />
            <p>Arrastre el archivo PDF aquí o <span className="upload-link">haga clic para seleccionar</span></p>
            <span className="upload-hint">Solo archivos PDF</span>
          </div>
        )}
        {hasError && <span className="error-text">{errors[field]}</span>}
      </div>
    );
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Gestión Documental</h3>
      <p className="help-text">Cargue los documentos requeridos en formato PDF para formalizar la creación del proyecto.</p>

      <div className="document-grid">
        {renderFileUpload('viabilizacionPdf', 'Documento de Viabilización', true)}

        {renderFileUpload(
          'actaConstitucionPdf',
          'Acta de Constitución',
          false,
          'El acta de constitución debe generarse dentro de los 6 meses siguientes a la viabilización del proyecto.'
        )}

        {renderFileUpload('cronogramaPdf', 'Cronograma del Proyecto', false)}

        {data.tienePlanComunicaciones === true && (
          renderFileUpload(
            'planComunicacionesPdf',
            'Plan de Comunicaciones',
            true,
            'Este documento es obligatorio porque indicó que el proyecto cuenta con un Plan de Comunicaciones.'
          )
        )}
      </div>
    </div>
  );
};

export default Paso6GestionDocumental;
