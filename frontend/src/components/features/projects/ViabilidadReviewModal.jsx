import { useState } from 'react';
import { CheckCircle, XCircle, AlertTriangle, FileText, Download } from 'lucide-react';
import projectService from '../../../services/projectService';
import documentService from '../../../services/documentService';
import { emitToast } from '../../../utils/feedback';

const ViabilidadReviewModal = ({ proyecto, onClose, onReviewComplete }) => {
  const [showReturnModal, setShowReturnModal] = useState(false);
  const [observaciones, setObservaciones] = useState('');
  const [loading, setLoading] = useState(false);

  const handleApprove = async () => {
    setLoading(true);
    try {
      await projectService.aprobarViabilidad(proyecto.id);
      emitToast({
        tone: 'success',
        title: 'Viabilidad aprobada',
        message: 'El Documento de Viabilidad fue aprobado exitosamente. El Director sera notificado.',
      });
      onReviewComplete();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error al aprobar';
      emitToast({
        tone: 'error',
        title: 'Error al aprobar',
        message: detail,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleReturn = async () => {
    if (!observaciones.trim()) {
      emitToast({
        tone: 'warning',
        title: 'Observaciones requeridas',
        message: 'Debe ingresar las observaciones al devolver el documento.',
      });
      return;
    }
    setLoading(true);
    try {
      await projectService.devolverViabilidad(proyecto.id, observaciones);
      emitToast({
        tone: 'success',
        title: 'Viabilidad devuelta',
        message: 'El Documento de Viabilidad fue devuelto al Director con observaciones.',
      });
      onReviewComplete();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error al devolver';
      emitToast({
        tone: 'error',
        title: 'Error al devolver',
        message: detail,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadDocument = async () => {
    try {
      const blob = await documentService.descargarDocumento(proyecto.id, 'VIABILIZACION');
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Viabilidad_${proyecto.id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      emitToast({
        tone: 'error',
        title: 'Error al descargar',
        message: 'No fue posible descargar el documento.',
      });
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px' }}>
        <h3>Revisar Documento de Viabilidad</h3>
        
        <div style={{ margin: '1rem 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <FileText size={18} />
            <strong>Proyecto: {proyecto.nombre}</strong>
          </div>
          <p style={{ color: '#666', margin: '0.5rem 0' }}>
            Director: {proyecto.director}
          </p>
        </div>

        <div style={{ margin: '1rem 0', padding: '1rem', background: '#f8f9fa', borderRadius: '8px' }}>
          <p style={{ margin: '0 0 0.5rem 0' }}>
            <strong>Estado actual:</strong> Documento de Viabilidad cargado, pendiente de revision.
          </p>
          <button
            type="button"
            className="btn-ghost"
            onClick={handleDownloadDocument}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Download size={16} />
            Descargar documento
          </button>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.5rem' }}>
          <button
            type="button"
            className="btn-ghost"
            onClick={onClose}
            disabled={loading}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="btn-danger"
            onClick={() => setShowReturnModal(true)}
            disabled={loading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <XCircle size={16} />
            Devolver
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={handleApprove}
            disabled={loading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <CheckCircle size={16} />
            {loading ? 'Procesando...' : 'Aprobar'}
          </button>
        </div>

        {showReturnModal && (
          <div className="modal-overlay" onClick={() => setShowReturnModal(false)} style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
              <h3>Devolver Documento de Viabilidad</h3>
              
              <div style={{ margin: '1rem 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#dc3545' }}>
                  <AlertTriangle size={18} />
                  <strong>Observaciones obligatorias</strong>
                </div>
                <p style={{ color: '#666', margin: '0.5rem 0' }}>
                  Ingrese las observaciones que el Director debe subsanar en el Documento de Viabilidad.
                </p>
                <textarea
                  className="form-input form-textarea"
                  value={observaciones}
                  onChange={(e) => setObservaciones(e.target.value)}
                  placeholder="Describa las observaciones o hallazgos que requieren subsanacion..."
                  rows={4}
                  style={{ width: '100%', marginTop: '0.5rem' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => setShowReturnModal(false)}
                  disabled={loading}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn-danger"
                  onClick={handleReturn}
                  disabled={loading || !observaciones.trim()}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                >
                  <XCircle size={16} />
                  {loading ? 'Procesando...' : 'Devolver con observaciones'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ViabilidadReviewModal;
