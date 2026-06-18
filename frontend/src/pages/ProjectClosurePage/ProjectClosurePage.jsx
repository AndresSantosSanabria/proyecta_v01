import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlertTriangle, Calendar, CheckCircle2, Download, FileText, ListTodo, Lock, ShieldAlert } from 'lucide-react';
import projectService from '../../services/projectService';
import './ProjectClosurePage.css';

const triggerBlobDownload = (blob, fileName) => {
  if (!(blob instanceof Blob)) {
    return;
  }

  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'acta-cierre.pdf';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

const getFilenameFromDisposition = (disposition, fallback) => {
  if (!disposition) {
    return fallback;
  }

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  const asciiMatch = disposition.match(/filename="?([^"]+)"?/i);
  return asciiMatch?.[1] || fallback;
};

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
  const [downloadingActa, setDownloadingActa] = useState(false);
  const [actaFileName, setActaFileName] = useState(null);

  const [summaryData, setSummaryData] = useState({
    id,
    nombre: '',
    director: '',
    directorCargo: '',
    directorEntidad: '',
    patrocinadorNombre: '',
    patrocinadorCargo: '',
    patrocinadorEntidad: '',
    fechaInicio: '',
    objetivoGeneral: '',
    objetivosEspecificos: [],
    avanceTotal: 0,
    estado: 'ACTIVO',
    totalFases: 0,
    totalHitos: 0,
    entregablesConformes: 0,
    totalEntregables: 0,
    puedeCerrar: false,
    entregables: [],
  });

  const [resumenEjecutivo, setResumenEjecutivo] = useState('');
  const [leccionesPositivas, setLeccionesPositivas] = useState('');
  const [leccionesMejorar, setLeccionesMejorar] = useState('');
  const [recomendaciones, setRecomendaciones] = useState('');
  const [transferenciaActividad, setTransferenciaActividad] = useState('');
  const [transferenciaFecha, setTransferenciaFecha] = useState(() => new Date().toISOString().split('T')[0]);
  const [transferenciaUbicacionEvidencia, setTransferenciaUbicacionEvidencia] = useState('');
  const [fechaCierre, setFechaCierre] = useState(() => new Date().toISOString().split('T')[0]);

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        setLoading(true);
        const response = await projectService.getSummary(id);
        const apiData = response.data || response;

        if (apiData) {
          setSummaryData({
            id: apiData.id || id,
            nombre: apiData.nombre || '',
            director: apiData.director || '',
            directorCargo: apiData.directorCargo || '',
            directorEntidad: apiData.directorEntidad || '',
            patrocinadorNombre: apiData.patrocinadorNombre || '',
            patrocinadorCargo: apiData.patrocinadorCargo || '',
            patrocinadorEntidad: apiData.patrocinadorEntidad || '',
            fechaInicio: apiData.fechaInicio || '',
            objetivoGeneral: apiData.objetivoGeneral || '',
            objetivosEspecificos: Array.isArray(apiData.objetivosEspecificos) ? apiData.objetivosEspecificos : [],
            avanceTotal: apiData.avanceTotal || apiData.avance_total || apiData.progresoEjecutado || 0,
            estado: apiData.estado || 'ACTIVO',
            totalFases: apiData.totalFases || 0,
            totalHitos: apiData.totalHitos || 0,
            entregablesConformes: apiData.entregablesConformes || apiData.entregablesConformidad || 0,
            totalEntregables: apiData.totalEntregables || 0,
            puedeCerrar: apiData.puede_cerrar !== undefined ? apiData.puede_cerrar : false,
            entregables: apiData.entregables || [],
          });
        }
      } catch (err) {
        console.error('Error fetching project summary:', err);
        setError('No se pudo cargar el resumen del proyecto. Verifica si el backend esta activo.');
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, [id]);

  const handleDownloadActa = async () => {
    try {
      setDownloadingActa(true);
      const downloadResponse = await projectService.downloadClosureActa(id);
      const blob = downloadResponse.data;
      const disposition = downloadResponse.headers?.['content-disposition'];
      const fallbackName = actaFileName || `acta_cierre_${id}.pdf`;
      const fileName = getFilenameFromDisposition(disposition, fallbackName);
      triggerBlobDownload(blob, fileName);
      setActaFileName(fileName);
    } catch (downloadError) {
      console.error('Error descargando acta de cierre:', downloadError);
      setError('No se pudo descargar el acta de cierre.');
    } finally {
      setDownloadingActa(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!summaryData.puedeCerrar) return;

    if (resumenEjecutivo.length < 100) {
      alert('El resumen ejecutivo debe contener al menos 100 caracteres.');
      return;
    }

    if (!leccionesPositivas.trim() || !leccionesMejorar.trim() || !recomendaciones.trim() || !transferenciaActividad.trim() || !transferenciaUbicacionEvidencia.trim()) {
      alert('Completa todos los campos obligatorios del acta antes de cerrar.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const closurePayload = {
        resumenEjecutivo,
        leccionesPositivas,
        leccionesMejorar,
        recomendaciones,
        transferenciaActividad,
        transferenciaFecha,
        transferenciaUbicacionEvidencia,
        fechaCierre,
      };

      const response = await projectService.closeProject(id, closurePayload);

      if (response.success) {
        setSuccessMsg(response.message || 'Proyecto cerrado formalmente con exito.');
        setActaFileName(response.archivoPdf || `acta_cierre_${id}.pdf`);
        setSummaryData((prev) => ({
          ...prev,
          estado: 'CERRADO',
          puedeCerrar: false,
        }));

        try {
          setDownloadingActa(true);
          const downloadResponse = await projectService.downloadClosureActa(id);
          const blob = downloadResponse.data;
          const disposition = downloadResponse.headers?.['content-disposition'];
          const fallbackName = response.archivoPdf || `acta_cierre_${id}.pdf`;
          const fileName = getFilenameFromDisposition(disposition, fallbackName);
          triggerBlobDownload(blob, fileName);
          setActaFileName(fileName);
        } catch (downloadError) {
          console.error('No se pudo descargar automaticamente el acta:', downloadError);
        } finally {
          setDownloadingActa(false);
        }
      } else {
        setError(response.errorBanner || response.message || 'Ocurrio un error al procesar el cierre.');
      }
    } catch (err) {
      console.error('Error closing project:', err);
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
        <p>Cargando informacion de cierre del proyecto...</p>
      </div>
    );
  }

  const isDisabled = !summaryData.puedeCerrar || summaryData.estado === 'CERRADO' || submitting;

  return (
    <div className="closure-page-container">
      <header className="closure-header">
        <div className="title-group">
          <h1>Cierre del Proyecto</h1>
          <p className="subtitle">
            {summaryData.id} - {summaryData.nombre || 'Proyecto'}
          </p>
        </div>
      </header>

      {successMsg && (
        <div className="success-banner">
          <CheckCircle2 className="success-icon" size={20} />
          <div className="banner-content">
            <strong>Exito en el cierre</strong>
            <p>{successMsg}</p>
            {actaFileName && <p>Archivo generado: {actaFileName}</p>}
            <button
              type="button"
              className="btn-primary-closure"
              onClick={handleDownloadActa}
              disabled={downloadingActa}
              style={{ marginTop: '12px' }}
            >
              <Download size={16} style={{ marginRight: '8px' }} />
              {downloadingActa ? 'Descargando acta...' : 'Descargar acta de cierre'}
            </button>
          </div>
        </div>
      )}

      {!summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && (
        <div className="validation-warning-banner" id="warning-closure-banner">
          <AlertTriangle className="warning-icon" size={22} />
          <div className="banner-content">
            <p>
              No es posible cerrar el proyecto aún. Todos los entregables deben estar aprobados y con su evidencia cargada para habilitar la solicitud de cierre.
            </p>
          </div>
        </div>
      )}

      {summaryData.estado === 'CERRADO' && !successMsg && (
        <div className="info-closed-banner">
          <CheckCircle2 className="closed-icon" size={22} />
          <div className="banner-content">
            <p><strong>Proyecto cerrado.</strong> Este proyecto ha finalizado su ciclo de vida y cuenta con acta de cierre aprobada.</p>
            <button
              type="button"
              className="btn-primary-closure"
              onClick={handleDownloadActa}
              disabled={downloadingActa}
              style={{ marginTop: '12px' }}
            >
              <Download size={16} style={{ marginRight: '8px' }} />
              {downloadingActa ? 'Descargando acta...' : 'Descargar acta de cierre'}
            </button>
          </div>
        </div>
      )}

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

        <div className="form-group">
          <label>Informacion base del acta</label>
          <div className="deliverables-container-readonly">
            <p><strong>Patrocinador:</strong> {summaryData.patrocinadorNombre || 'No registrado'} {summaryData.patrocinadorCargo ? `- ${summaryData.patrocinadorCargo}` : ''} {summaryData.patrocinadorEntidad ? `- ${summaryData.patrocinadorEntidad}` : ''}</p>
            <p><strong>Director:</strong> {summaryData.director || 'No registrado'} {summaryData.directorCargo ? `- ${summaryData.directorCargo}` : ''} {summaryData.directorEntidad ? `- ${summaryData.directorEntidad}` : ''}</p>
            <p><strong>Objetivo general:</strong> {summaryData.objetivoGeneral || 'No registrado'}</p>
            <p><strong>Objetivos especificos:</strong> {Array.isArray(summaryData.objetivosEspecificos) && summaryData.objetivosEspecificos.length > 0 ? summaryData.objetivosEspecificos.length : 0}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="closure-form">
          <div className="form-group">
            <label htmlFor="resumen-ejecutivo">
              Resumen Ejecutivo <span className="required-asterisk">*</span>
            </label>
            <div className="textarea-container">
              <textarea
                id="resumen-ejecutivo"
                className="resumen-textarea"
                placeholder="Resumen de los logros obtenidos al finalizar el proyecto."
                value={resumenEjecutivo}
                onChange={(e) => setResumenEjecutivo(e.target.value)}
                disabled={isDisabled}
                maxLength={2000}
                required
              />
              <div className={`char-count ${resumenEjecutivo.length >= 100 ? 'char-ok' : resumenEjecutivo.length > 0 ? 'char-warn' : ''}`}>
                {resumenEjecutivo.length} / 100 caracteres minimos
                {resumenEjecutivo.length >= 100 && ' ✓'}
              </div>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="lecciones-positivas">
              Lecciones aprendidas: aspectos positivos <span className="required-asterisk">*</span>
            </label>
            <textarea
              id="lecciones-positivas"
              className="resumen-textarea"
              placeholder="Describe que funciono bien durante el proyecto."
              value={leccionesPositivas}
              onChange={(e) => setLeccionesPositivas(e.target.value)}
              disabled={isDisabled}
              maxLength={2000}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="lecciones-mejorar">
              Lecciones aprendidas: aspectos a mejorar <span className="required-asterisk">*</span>
            </label>
            <textarea
              id="lecciones-mejorar"
              className="resumen-textarea"
              placeholder="Describe que se debe ajustar o mejorar en futuros proyectos."
              value={leccionesMejorar}
              onChange={(e) => setLeccionesMejorar(e.target.value)}
              disabled={isDisabled}
              maxLength={2000}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="recomendaciones">
              Recomendaciones para futuros proyectos <span className="required-asterisk">*</span>
            </label>
            <textarea
              id="recomendaciones"
              className="resumen-textarea"
              placeholder="Incluye recomendaciones operativas, tecnicas o de gestion."
              value={recomendaciones}
              onChange={(e) => setRecomendaciones(e.target.value)}
              disabled={isDisabled}
              maxLength={2000}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="transferencia-actividad">
              Transferencia de conocimiento: actividad ejecutada <span className="required-asterisk">*</span>
            </label>
            <textarea
              id="transferencia-actividad"
              className="resumen-textarea"
              placeholder="Describe la capacitacion, taller, video, curso o metodo aplicado."
              value={transferenciaActividad}
              onChange={(e) => setTransferenciaActividad(e.target.value)}
              disabled={isDisabled}
              maxLength={1500}
              required
            />
          </div>

          <div className="form-control-row">
            <div className="form-group col-half">
              <label htmlFor="transferencia-fecha">
                Fecha de transferencia <span className="required-asterisk">*</span>
              </label>
              <div className="input-with-icon">
                <Calendar size={18} className="input-icon" />
                <input
                  type="date"
                  id="transferencia-fecha"
                  value={transferenciaFecha}
                  onChange={(e) => setTransferenciaFecha(e.target.value)}
                  disabled={isDisabled}
                  required
                />
              </div>
            </div>

            <div className="form-group col-half">
              <label htmlFor="fecha-cierre">
                Fecha de Cierre <span className="required-asterisk">*</span>
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
          </div>

          <div className="form-group">
            <label htmlFor="transferencia-ubicacion">
              Ubicacion de la evidencia <span className="required-asterisk">*</span>
            </label>
            <textarea
              id="transferencia-ubicacion"
              className="resumen-textarea"
              placeholder="Ruta, enlace o ubicacion fisica de la evidencia de la transferencia."
              value={transferenciaUbicacionEvidencia}
              onChange={(e) => setTransferenciaUbicacionEvidencia(e.target.value)}
              disabled={isDisabled}
              maxLength={500}
              required
            />
          </div>

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
            <p className="field-hint">Esta lista se obtiene automaticamente del cronograma del proyecto.</p>
          </div>

          <div className="form-control-row">
            <div className="form-group col-half">
              <label htmlFor="avance-final">Avance Final (calculado)</label>
              <div className="input-with-icon disabled-input-wrapper">
                <Lock size={16} className="input-icon locked-icon" />
                <input
                  type="text"
                  id="avance-final"
                  value={`${summaryData.avanceTotal}% - calculado automaticamente`}
                  disabled
                />
              </div>
            </div>
          </div>

          {summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && (
            <div className="form-actions">
              <div className="closure-ready-note">
                <CheckCircle2 size={18} />
                <span>El proyecto ya está listo para solicitar cierre.</span>
              </div>
              <button
                type="submit"
                className="btn-primary-closure"
                disabled={submitting || resumenEjecutivo.length < 100}
              >
                {submitting ? 'Procesando cierre...' : 'Cerrar Proyecto'}
              </button>
            </div>
          )}

          {summaryData.estado === 'CERRADO' && !successMsg && (
            <div className="form-actions">
              <button
                type="button"
                className="btn-primary-closure"
                onClick={handleDownloadActa}
                disabled={downloadingActa}
              >
                <Download size={16} style={{ marginRight: '8px' }} />
                {downloadingActa ? 'Descargando acta...' : 'Descargar acta de cierre'}
              </button>
            </div>
          )}

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
