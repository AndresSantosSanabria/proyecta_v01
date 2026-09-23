import { useState } from 'react';
import { ChevronDown, ChevronUp, Download, Eye, FileText } from 'lucide-react';
import riskService from '../../../services/riskService';

const formatBytes = (bytes) => {
  if (!bytes) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const formatFecha = (fecha) => {
  if (!fecha) return 'Sin fecha';
  return new Date(fecha).toLocaleDateString('es-CO', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Bogota',
  });
};

const TratamientoHistory = ({ tratamientos = [], proyectoId, riesgoId }) => {
  const [expandedId, setExpandedId] = useState(null);
  const [previewBlob, setPreviewBlob] = useState(null);
  const [previewName, setPreviewName] = useState('');

  const toggleExpand = (id) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  const handlePreview = async (adjunto, tratamientoId) => {
    try {
      const blob = await riskService.descargarAdjuntoTratamiento(proyectoId, riesgoId, tratamientoId, adjunto.id);
      const url = window.URL.createObjectURL(blob);
      setPreviewBlob(url);
      setPreviewName(adjunto.nombreOriginal);
    } catch {
      console.error('Error al obtener vista previa del adjunto');
    }
  };

  const handleDownload = async (adjunto, tratamientoId) => {
    try {
      const blob = await riskService.descargarAdjuntoTratamiento(proyectoId, riesgoId, tratamientoId, adjunto.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = adjunto.nombreOriginal || `tratamiento_${tratamientoId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      console.error('Error al descargar adjunto');
    }
  };

  const closePreview = () => {
    if (previewBlob) {
      window.URL.revokeObjectURL(previewBlob);
    }
    setPreviewBlob(null);
    setPreviewName('');
  };

  if (!tratamientos || tratamientos.length === 0) {
    return (
      <div className="tratamiento-history-empty">
        <FileText size={20} />
        <p>No hay tratamientos registrados para este riesgo.</p>
      </div>
    );
  }

  return (
    <div className="tratamiento-history">
      {tratamientos.map((tratamiento) => {
        const isExpanded = expandedId === tratamiento.id;
        const fechaCorta = tratamiento.fechaCreacion
          ? new Date(tratamiento.fechaCreacion).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'America/Bogota' })
          : '';
        const adjuntosCount = tratamiento.adjuntos?.length || 0;

        return (
          <div key={tratamiento.id} className={`tratamiento-card ${isExpanded ? 'expanded' : ''}`}>
            <button
              type="button"
              className="tratamiento-card-header"
              onClick={() => toggleExpand(tratamiento.id)}
            >
              <div className="tratamiento-card-title">
                <span className="tratamiento-iteracion">Tratamiento {tratamiento.iteracion}</span>
                <span className="tratamiento-fecha">{fechaCorta}</span>
                {adjuntosCount > 0 && (
                  <span className="tratamiento-adjuntos-badge">
                    <FileText size={12} /> {adjuntosCount} PDF{adjuntosCount !== 1 ? 's' : ''}
                  </span>
                )}
              </div>
              {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
            </button>

            {isExpanded && (
              <div className="tratamiento-card-body">
                <p className="tratamiento-comentario">{tratamiento.comentario}</p>

                {tratamiento.adjuntos && tratamiento.adjuntos.length > 0 && (
                  <div className="tratamiento-adjuntos">
                    {tratamiento.adjuntos.map((adjunto) => (
                      <article className="tratamiento-adjunto-item" key={adjunto.id}>
                        <div className="tratamiento-adjunto-info">
                          <strong>{adjunto.nombreOriginal}</strong>
                          <span>{formatBytes(adjunto.tamanoBytes)} · {formatFecha(adjunto.fechaCarga)}</span>
                        </div>
                        <div className="tratamiento-adjunto-actions">
                          <button
                            type="button"
                            className="btn-secondary compact"
                            onClick={() => handlePreview(adjunto, tratamiento.id)}
                            title="Vista previa"
                          >
                            <Eye size={14} />
                          </button>
                          <button
                            type="button"
                            className="btn-secondary compact"
                            onClick={() => handleDownload(adjunto, tratamiento.id)}
                            title="Descargar PDF"
                          >
                            <Download size={14} />
                          </button>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}

      {previewBlob && (
        <div className="pdf-preview-backdrop" onClick={closePreview}>
          <div className="pdf-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pdf-preview-header">
              <span className="pdf-preview-title">{previewName}</span>
              <button className="pdf-preview-close" onClick={closePreview}>
                <X size={18} />
              </button>
            </div>
            <iframe src={previewBlob} className="pdf-preview-iframe" title={previewName} />
          </div>
        </div>
      )}
    </div>
  );
};

export default TratamientoHistory;
