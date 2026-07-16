import { useState, useEffect } from 'react';
import { Calendar, Clock, Download, X, LoaderCircle } from 'lucide-react';
import './HistorialCambiosFecha.css';

export default function HistorialCambiosFecha({ entregable, proyectoId, onClose }) {
  const [historial, setHistorial] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const apiClient = (await import('../../../api/axiosConfig')).default;
        const { data } = await apiClient.get(
          `/proyectos/${proyectoId}/entregables/${entregable.id}/historial-fechas`
        );
        setHistorial(data?.data || []);
      } catch (err) {
        setError(err?.response?.data?.detail || 'Error al cargar el historial');
      } finally {
        setLoading(false);
      }
    })();
  }, [entregable.id, proyectoId]);

  const descargarPdf = async (cambioId, nombreOriginal) => {
    try {
      const apiClient = (await import('../../../api/axiosConfig')).default;
      const response = await apiClient.get(
        `/proyectos/${proyectoId}/entregables/cambios-fecha/${cambioId}/descargar`,
        { responseType: 'blob' }
      );
      const url = URL.createObjectURL(response.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = nombreOriginal || `soporte-cambio-${cambioId}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error al descargar PDF:', err);
    }
  };

  const formatFecha = (fecha) => {
    if (!fecha) return '-';
    const d = new Date(fecha + 'T00:00:00');
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const formatFechaHora = (fechaHora) => {
    if (!fechaHora) return '-';
    const d = new Date(fechaHora);
    return d.toLocaleDateString('es-CO', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="hcf-modal" onClick={(e) => e.stopPropagation()}>
        <div className="hcf-header">
          <h3><Clock size={16} /> Historial de Cambios de Fecha</h3>
          <button className="hcf-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="hcf-body">
          <div className="hcf-info">
            <strong>{entregable.nombre}</strong>
            <span>Fecha actual: <b>{formatFecha(entregable.fechaEntrega)}</b></span>
          </div>

          {loading && (
            <div className="hcf-loading">
              <LoaderCircle size={24} className="spin" />
              <span>Cargando historial...</span>
            </div>
          )}

          {error && <div className="hcf-error">{error}</div>}

          {!loading && !error && historial.length === 0 && (
            <div className="hcf-empty">
              <Calendar size={32} />
              <span>No hay cambios de fecha registrados para este entregable.</span>
            </div>
          )}

          {!loading && historial.length > 0 && (
            <div className="hcf-list">
              {historial.map((item) => (
                <div key={item.id} className="hcf-item">
                  <div className="hcf-item-header">
                    <span className="hcf-item-date">{formatFechaHora(item.creadoEn)}</span>
                    <span className="hcf-item-user">{item.usuario}</span>
                  </div>
                  <div className="hcf-item-fechas">
                    <span className="hcf-old">{formatFecha(item.fechaAnterior)}</span>
                    <span className="hcf-arrow">&rarr;</span>
                    <span className="hcf-new">{formatFecha(item.fechaNueva)}</span>
                  </div>
                  <div className="hcf-item-justificacion">
                    <em>{item.justificacion}</em>
                  </div>
                  <div className="hcf-item-actions">
                    {item.archivoPdf && (
                      <button
                        className="hcf-download-btn"
                        onClick={() => descargarPdf(item.id, item.nombreOriginal)}
                      >
                        <Download size={14} /> PDF de soporte
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
