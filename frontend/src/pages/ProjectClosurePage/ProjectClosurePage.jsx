import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Lock, AlertTriangle, Calendar, FileText, ListTodo, ShieldAlert, CheckCircle2 } from 'lucide-react';
import projectService from '../../services/projectService';
import './ProjectClosurePage.css';

/**
 * ProjectClosurePage Component
 * Vista de Cierre de Proyecto. Muestra el estado del cierre, valida requisitos y genera actas.
 */
const ProjectClosurePage = () => {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Datos del proyecto y estado de cierre
  const [summaryData, setSummaryData] = useState({
    id: id,
    nombre: '',
    director: '',
    fechaInicio: '',
    avanceTotal: 0,
    estado: 'ACTIVO',
    totalFases: 0,
    totalHitos: 0,
    entregablesConformes: 0,
    totalEntregables: 0,
    puedeCerrar: false,
    entregables: []
  });

  // Campos del formulario
  const [resumenEjecutivo, setResumenEjecutivo] = useState('');
  const [fechaCierre, setFechaCierre] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        setLoading(true);
        const response = await projectService.getSummary(id);
        
        // El backend responde con { success, data, message }
        const apiData = response.data || response;
        
        if (apiData) {
          setSummaryData({
            id: apiData.id || id,
            nombre: apiData.nombre || '',
            director: apiData.director || '',
            fechaInicio: apiData.fechaInicio || '',
            avanceTotal: apiData.avanceTotal || apiData.avance_total || apiData.progresoEjecutado || 0,
            estado: apiData.estado || 'ACTIVO',
            totalFases: apiData.totalFases || 0,
            totalHitos: apiData.totalHitos || 0,
            entregablesConformes: apiData.entregablesConformes || apiData.entregablesConformidad || 0,
            totalEntregables: apiData.totalEntregables || 0,
            puedeCerrar: apiData.puede_cerrar !== undefined ? apiData.puede_cerrar : false,
            entregables: apiData.entregables || []
          });
        }
      } catch (err) {
        console.error('Error fetching project summary:', err);
        setError('No se pudo cargar el resumen del proyecto. Verifica si el backend está activo.');
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, [id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!summaryData.puedeCerrar) return;

    if (resumenEjecutivo.length < 100) {
      alert('El resumen ejecutivo debe contener al menos 100 caracteres.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      // El backend recibe CierreProyectoRequest con { resumenEjecutivo }
      const response = await projectService.closeProject(id, { resumenEjecutivo });
      if (response.success) {
        setSuccessMsg(response.message || '¡Proyecto cerrado formalmente con éxito!');
        setSummaryData(prev => ({
          ...prev,
          estado: 'CERRADO',
          puedeCerrar: false
        }));
      } else {
        // Usar errorBanner del backend si existe (mensaje de validación de negocio)
        setError(response.errorBanner || response.message || 'Ocurrió un error al procesar el cierre.');
      }
    } catch (err) {
      console.error('Error closing project:', err);
      // Extraer mensaje de validación de Spring (@Valid) o mensaje genérico
      const errData = err.response?.data;
      const errMsg = errData?.errorBanner || errData?.message || errData?.detail || 'No se pudo completar el cierre del proyecto.';
      setError(errMsg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="closure-page-loading">
        <div className="spinner"></div>
        <p>Cargando información de cierre del proyecto...</p>
      </div>
    );
  }

  // Comportamiento de campos editables deshabilitados si puedeCerrar es false
  const isDisabled = !summaryData.puedeCerrar || summaryData.estado === 'CERRADO' || submitting;

  return (
    <div className="closure-page-container">
      {/* Encabezado del Módulo */}
      <header className="closure-header">
        <div className="title-group">
          <h1>Cierre del Proyecto</h1>
          <p className="subtitle">
            {summaryData.id} — {summaryData.nombre || 'Fortalecimiento de Talento TI en Cundinamarca'}
          </p>
        </div>
      </header>

      {/* Mensaje de Éxito al cerrar */}
      {successMsg && (
        <div className="success-banner">
          <CheckCircle2 className="success-icon" size={20} />
          <div className="banner-content">
            <strong>¡Cierre Exitoso!</strong>
            <p>{successMsg}</p>
          </div>
        </div>
      )}

      {/* Banner de Alerta de Validación (Condicional) */}
      {!summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && (
        <div className="validation-warning-banner" id="warning-closure-banner">
          <AlertTriangle className="warning-icon" size={22} />
          <div className="banner-content">
            <p>
              No es posible cerrar el proyecto aún. Todos los hitos deben estar al 100% de cumplimiento y haber sido revisados por el Gestor de Proyectos TIC.
            </p>
          </div>
        </div>
      )}

      {/* Si el proyecto ya está cerrado */}
      {summaryData.estado === 'CERRADO' && !successMsg && (
        <div className="info-closed-banner">
          <CheckCircle2 className="closed-icon" size={22} />
          <div className="banner-content">
            <p><strong>Proyecto Cerrado.</strong> Este proyecto ha finalizado su ciclo de vida y cuenta con acta de cierre aprobada.</p>
          </div>
        </div>
      )}

      {/* Tarjeta 'Acta de Cierre del Proyecto' */}
      <div className="closure-card">
        <div className="card-header">
          <div className="card-title">
            <FileText size={20} className="header-icon" />
            <h2>Acta de Cierre del Proyecto</h2>
          </div>
          <span className={`status-badge ${summaryData.estado.toLowerCase()}`}>
            {summaryData.estado}
          </span>
        </div>

        <form onSubmit={handleSubmit} className="closure-form">
          {/* Campo 'Resumen Ejecutivo' */}
          <div className="form-group">
            <label htmlFor="resumen-ejecutivo">
              Resumen Ejecutivo <span className="required-asterisk">*</span>
            </label>
            <div className="textarea-container">
              <textarea
                id="resumen-ejecutivo"
                className="resumen-textarea"
                placeholder="Resumen de los logros obtenidos al finalizar el proyecto. Indica qué se comprometió en el Plan para la dirección del proyecto..."
                value={resumenEjecutivo}
                onChange={(e) => setResumenEjecutivo(e.target.value)}
                disabled={isDisabled}
                maxLength={2000}
                required
              />
              <div className={`char-count ${resumenEjecutivo.length >= 100 ? 'char-ok' : resumenEjecutivo.length > 0 ? 'char-warn' : ''}`}>
                {resumenEjecutivo.length} / 100 caracteres mínimos
                {resumenEjecutivo.length >= 100 && ' ✓'}
              </div>
            </div>
          </div>

          {/* Campo 'Lista de Entregables' */}
          <div className="form-group">
            <label>
              <ListTodo size={16} className="label-icon" />
              Lista de Entregables Clave
            </label>
            <div className="deliverables-container-readonly" id="deliverables-list">
              {summaryData.entregables && summaryData.entregables.length > 0 ? (
                summaryData.entregables.map((entregable, index) => (
                  <div key={index} className="deliverable-item">
                    <span className="bullet">•</span>
                    <span className="deliverable-text">{entregable}</span>
                  </div>
                ))
              ) : (
                <p className="no-deliverables">No hay entregables clave registrados en este proyecto.</p>
              )}
            </div>
            <p className="field-hint">Esta lista se obtiene automáticamente del cronograma del proyecto.</p>
          </div>

          {/* Fila de Control Inferior (Dos columnas) */}
          <div className="form-control-row">
            {/* Columna Izquierda: Fecha de Cierre */}
            <div className="form-group col-half">
              <label htmlFor="fecha-cierre">
                Fecha de Cierre
              </label>
              <div className="input-with-icon">
                <Calendar size={18} className="input-icon" />
                <input
                  type="date"
                  id="fecha-cierre"
                  value={fechaCierre}
                  onChange={(e) => setFechaCierre(e.target.value)}
                  disabled={isDisabled}
                  required
                />
              </div>
            </div>

            {/* Columna Derecha: Avance Final */}
            <div className="form-group col-half">
              <label htmlFor="avance-final">
                Avance Final (calculado)
              </label>
              <div className="input-with-icon disabled-input-wrapper">
                <Lock size={16} className="input-icon locked-icon" />
                <input
                  type="text"
                  id="avance-final"
                  value={`${summaryData.avanceTotal}%  —  calculado automáticamente`}
                  disabled
                />
              </div>
            </div>
          </div>

          {/* Acciones del formulario (Solo visibles si es apto para cierre) */}
          {summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && (
            <div className="form-actions">
              <button
                type="submit"
                className="btn-primary-closure"
                disabled={submitting || resumenEjecutivo.length < 100}
              >
                {submitting ? 'Procesando Cierre...' : 'Cerrar Proyecto'}
              </button>
            </div>
          )}

          {/* Mensajes de error del servidor */}
          {error && (
            <div className="error-message-box">
              <ShieldAlert size={18} />
              <span>{error}</span>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};

export default ProjectClosurePage;
