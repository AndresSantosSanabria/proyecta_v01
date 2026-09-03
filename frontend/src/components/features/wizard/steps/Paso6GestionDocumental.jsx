import { useState } from 'react';
import { Upload, FileText, X, FileCheck, AlertCircle } from 'lucide-react';
import './Paso6GestionDocumental.css';

const DOC_CONFIG = [
  {
    field: 'viabilizacionPdf',
    label: 'Documento de Viabilización',
    icon: FileCheck,
    required: true,
    note: null,
  },
  {
    field: 'actaConstitucionPdf',
    label: 'Acta de Constitución',
    icon: FileText,
    required: false,
    note: 'El acta debe generarse dentro de los 6 meses siguientes a la viabilización del proyecto.',
  },
  {
    field: 'cronogramaPdf',
    label: 'Cronograma del Proyecto',
    icon: FileText,
    required: false,
    note: null,
  },
];

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

  const handleDrop = (field, e) => {
    e.preventDefault();
    setDragging(null);
    const f = e.dataTransfer.files[0];
    if (f && f.type === 'application/pdf') {
      onChange({ [field]: f });
    }
  };

  const docs = [
    ...DOC_CONFIG,
    {
      field: 'planComunicacionesPdf',
      label: 'Plan de Comunicaciones',
      icon: FileText,
      required: true,
      note: null,
    },
  ];

  return (
    <div className="step-form">
      <h3 className="step-title">Gestión Documental</h3>
      <p className="help-text">
        Cargue los documentos requeridos en formato PDF para formalizar la creación del proyecto.
      </p>

      <div className="doc-grid">
        {docs.map(({ field, label, icon: Icon, required, note }) => {
          const file = data[field];
          const hasError = errors[field];

          return (
            <div
              key={field}
              className={[
                'doc-card',
                hasError ? 'has-error' : '',
                file ? 'has-file' : '',
              ].filter(Boolean).join(' ')}
            >
              <div className="doc-card-header">
                <div className="doc-card-icon">
                  <Icon size={17} strokeWidth={2} />
                </div>
                <div className="doc-card-title">
                  <h4>
                    {label}
                    {required && <span className="doc-required">Requerido</span>}
                    {!required && <span className="doc-optional">Opcional</span>}
                  </h4>
                  {note && <p className="doc-card-note">{note}</p>}
                </div>
              </div>

              {file ? (
                <div className="doc-file-selected">
                  <div className="doc-file-icon">
                    <FileText size={16} strokeWidth={2.2} />
                  </div>
                  <div className="doc-file-info">
                    <span className="doc-file-name">{file.name}</span>
                    <span className="doc-file-size">
                      {(file.size / 1024).toFixed(1)} KB
                    </span>
                  </div>
                  <button
                    type="button"
                    className="doc-file-remove"
                    onClick={() => handleRemove(field)}
                    aria-label={`Eliminar ${label}`}
                  >
                    <X size={14} strokeWidth={2.5} />
                  </button>
                </div>
              ) : (
                <div
                  className={`doc-dropzone ${dragging === field ? 'dragging' : ''}`}
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDragging(field);
                  }}
                  onDragLeave={() => setDragging(null)}
                  onDrop={(e) => handleDrop(field, e)}
                  onClick={() => handleFileSelect(field)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleFileSelect(field);
                    }
                  }}
                >
                  <div className="doc-dropzone-icon">
                    <Upload size={19} strokeWidth={2} />
                  </div>
                  <p className="doc-dropzone-text">
                    Arrastre el archivo aquí o <span>seleccione</span>
                  </p>
                  <span className="doc-dropzone-hint">Solo archivos PDF</span>
                </div>
              )}

              {hasError && (
                <p className="doc-error-text">
                  <AlertCircle size={12} strokeWidth={2.5} />
                  {errors[field]}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Paso6GestionDocumental;
