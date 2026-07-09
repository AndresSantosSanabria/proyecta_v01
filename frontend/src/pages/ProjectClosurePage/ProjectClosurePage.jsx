import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlertTriangle, Calendar, CheckCircle2, Download, FileText, ListTodo, Lock, Send, ShieldAlert, Clock } from 'lucide-react';
import projectService from '../../services/projectService';
import authzService from '../../services/authzService';
import securityService from '../../services/securityService';
import './ProjectClosurePage.css';

const triggerBlobDownload = (blob, fileName) => {
  if (!(blob instanceof Blob)) return;
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
  if (!disposition) return fallback;
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try { return decodeURIComponent(utf8Match[1]); } catch { return utf8Match[1]; }
  }
  const asciiMatch = disposition.match(/filename="?([^"]+)"?/i);
  return asciiMatch?.[1] || fallback;
};

const ProjectClosurePage = () => {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [downloadingActa, setDownloadingActa] = useState(false);
  const [actaFileName, setActaFileName] = useState(null);
  const [requestingClosure, setRequestingClosure] = useState(false);
  const [userRole, setUserRole] = useState(null);
  const [cierreSolicitado, setCierreSolicitado] = useState(false);

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

  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [questionsLoading, setQuestionsLoading] = useState(true);
  const [resolvedTemplate, setResolvedTemplate] = useState(null);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        setLoading(true);
        const [response, meResponse] = await Promise.all([
          projectService.getSummary(id),
          authzService.getMe(),
        ]);
        const apiData = response.data || response;
        const meData = meResponse.data || meResponse;

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
          setCierreSolicitado(apiData.cierre_solicitado || false);
        }

        if (meData) {
          const roles = meData.roles || [];
          setUserRole(roles);
        }
      } catch (err) {
        console.error('Error fetching project summary:', err);
        setError('No se pudo cargar el resumen del proyecto.');
      } finally {
        setLoading(false);
      }
    };

    fetchAll();
  }, [id]);

  useEffect(() => {
    const fetchQuestionsAndAnswers = async () => {
      try {
        setQuestionsLoading(true);
        const [qRes, aRes, tRes] = await Promise.all([
          securityService.listActiveClosureQuestions().catch(() => ({ data: [] })),
          securityService.getClosureAnswers(id).catch(() => ({ data: [] })),
          securityService.getResolvedClosureTemplate(id).catch(() => ({ data: null })),
        ]);
        const qData = qRes?.data?.data ?? qRes?.data ?? qRes;
        setQuestions(Array.isArray(qData) ? qData : []);

        const aData = aRes?.data?.data ?? aRes?.data ?? aRes;
        if (Array.isArray(aData)) {
          const map = {};
          aData.forEach((a) => { map[a.questionId] = a.respuesta || ''; });
          setAnswers(map);
        }

        const tData = tRes?.data?.data ?? tRes?.data ?? tRes;
        if (tData) {
          try {
            const parsed = typeof tData === 'string' ? JSON.parse(tData) : tData;
            setResolvedTemplate(parsed);
          } catch { setResolvedTemplate(null); }
        }
      } catch (err) {
        console.error('Error loading questions/answers:', err);
      } finally {
        setQuestionsLoading(false);
      }
    };
    if (id) fetchQuestionsAndAnswers();
  }, [id]);

  const updateAnswer = (questionId, value) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
  };

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

  const handleSolicitarCierre = async () => {
    if (!window.confirm('Esta seguro que desea solicitar el cierre del proyecto? El Gestor sera notificado.')) return;

    try {
      setRequestingClosure(true);
      setError(null);
      await saveAnswersIfDynamic();
      const response = await projectService.solicitarCierre(id);

      if (response.success) {
        setCierreSolicitado(true);
        setSuccessMsg(response.message || 'Solicitud de cierre enviada exitosamente al Gestor.');
      } else {
        setError(response.errorBanner || response.message || 'No se pudo enviar la solicitud de cierre.');
      }
    } catch (err) {
      console.error('Error requesting closure:', err);
      const errData = err.response?.data;
      setError(errData?.errorBanner || errData?.message || 'No se pudo enviar la solicitud de cierre.');
    } finally {
      setRequestingClosure(false);
    }
  };

  const saveAnswersIfDynamic = async () => {
    if (questions.length === 0) return;
    const payload = questions.map((q) => ({
      questionId: q.id,
      respuesta: answers[q.id] || '',
    }));
    await securityService.saveClosureAnswers(id, payload);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!summaryData.puedeCerrar) return;

    try {
      setSubmitting(true);
      setError(null);
      await saveAnswersIfDynamic();

      const closurePayload = {};
      const response = await projectService.closeProject(id, closurePayload);

      if (response.success) {
        setSuccessMsg(response.message || 'Proyecto cerrado formalmente con exito.');
        setActaFileName(response.archivoPdf || `acta_cierre_${id}.pdf`);
        setSummaryData((prev) => ({ ...prev, estado: 'CERRADO', puedeCerrar: false }));

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
      setError(errData?.errorBanner || errData?.message || errData?.detail || 'No se pudo completar el cierre del proyecto.');
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
  const roles = Array.isArray(userRole) ? userRole.map(r => r.toLowerCase()) : [];
  const isDirector = roles.some(r => r.includes('director'));
  const isGestorOrAdmin = roles.some(r => r.includes('gestor') || r.includes('administrador'));
  const canRequestClosure = isDirector && summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && !cierreSolicitado;
  const canCloseProject = isGestorOrAdmin && summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && cierreSolicitado;

  const renderQuestion = (q) => {
    const value = answers[q.id] || '';
    if (q.tipoRespuesta === 'fecha') {
      return (
        <div key={q.id} className="closure-dyn-field">
          <label className="closure-dyn-label">{q.texto}</label>
          <input type="date" className="closure-dyn-input" value={value} onChange={(e) => updateAnswer(q.id, e.target.value)} disabled={isDisabled} />
        </div>
      );
    }
    if (q.tipoRespuesta === 'numero') {
      return (
        <div key={q.id} className="closure-dyn-field">
          <label className="closure-dyn-label">{q.texto}</label>
          <input type="number" className="closure-dyn-input" value={value} onChange={(e) => updateAnswer(q.id, e.target.value)} disabled={isDisabled} />
        </div>
      );
    }
    if (q.tipoRespuesta === 'seleccion_unica') {
      const opts = Array.isArray(q.opciones) ? q.opciones : [];
      return (
        <div key={q.id} className="closure-dyn-field">
          <label className="closure-dyn-label">{q.texto}</label>
          <div className="closure-dyn-options">
            {opts.map((opt, i) => (
              <label key={i} className="closure-dyn-option">
                <input type="radio" name={`q_${q.id}`} value={opt} checked={value === opt} onChange={(e) => updateAnswer(q.id, e.target.value)} disabled={isDisabled} />
                <span>{opt}</span>
              </label>
            ))}
          </div>
        </div>
      );
    }
    if (q.tipoRespuesta === 'seleccion_multiple') {
      const opts = Array.isArray(q.opciones) ? q.opciones : [];
      const selected = value ? value.split(',').filter(Boolean) : [];
      const toggleOpt = (opt) => {
        const next = selected.includes(opt) ? selected.filter((s) => s !== opt) : [...selected, opt];
        updateAnswer(q.id, next.join(', '));
      };
      return (
        <div key={q.id} className="closure-dyn-field">
          <label className="closure-dyn-label">{q.texto}</label>
          <div className="closure-dyn-options">
            {opts.map((opt, i) => (
              <label key={i} className="closure-dyn-option">
                <input type="checkbox" checked={selected.includes(opt)} onChange={() => toggleOpt(opt)} disabled={isDisabled} />
                <span>{opt}</span>
              </label>
            ))}
          </div>
        </div>
      );
    }
    return (
      <div key={q.id} className="closure-dyn-field">
        <label className="closure-dyn-label">{q.texto}</label>
        <textarea className="closure-dyn-textarea" value={value} onChange={(e) => updateAnswer(q.id, e.target.value)} disabled={isDisabled} maxLength={2000} rows={4} />
      </div>
    );
  };

  return (
    <div className="closure-page-container">
      <header className="closure-header">
        <div className="title-group">
          <h1>Cierre del Proyecto</h1>
          <p className="subtitle">{summaryData.id} - {summaryData.nombre || 'Proyecto'}</p>
        </div>
      </header>

      {successMsg && (
        <div className="success-banner">
          <CheckCircle2 className="success-icon" size={20} />
          <div className="banner-content">
            <strong>Exito en el cierre</strong>
            <p>{successMsg}</p>
            {actaFileName && <p>Archivo generado: {actaFileName}</p>}
            <button type="button" className="btn-primary-closure" onClick={handleDownloadActa} disabled={downloadingActa} style={{ marginTop: '12px' }}>
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
            <p>No es posible cerrar el proyecto aun. Todos los entregables deben estar aprobados y con su evidencia cargada para habilitar la solicitud de cierre.</p>
          </div>
        </div>
      )}

      {summaryData.estado === 'CERRADO' && !successMsg && (
        <div className="info-closed-banner">
          <CheckCircle2 className="closed-icon" size={22} />
          <div className="banner-content">
            <p><strong>Proyecto cerrado.</strong> Este proyecto ha finalizado su ciclo de vida y cuenta con acta de cierre aprobada.</p>
            <button type="button" className="btn-primary-closure" onClick={handleDownloadActa} disabled={downloadingActa} style={{ marginTop: '12px' }}>
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
          <span className={`status-badge ${summaryData.estado.toLowerCase()}`}>{summaryData.estado}</span>
        </div>

        <div className="form-group">
          <label>Informacion base del acta</label>
          <div className="deliverables-container-readonly">
            <p><strong>Patrocinador:</strong> {summaryData.patrocinadorNombre || 'No registrado'} {summaryData.patrocinadorCargo ? `- ${summaryData.patrocinadorCargo}` : ''} {summaryData.patrocinadorEntidad ? `- ${summaryData.patrocinadorEntidad}` : ''}</p>
            <p><strong>Director:</strong> {summaryData.director || 'No registrado'} {summaryData.directorCargo ? `- ${summaryData.directorCargo}` : ''} {summaryData.directorEntidad ? `- ${summaryData.directorEntidad}` : ''}</p>
            <p><strong>Objetivo general:</strong> {summaryData.objetivoGeneral || 'No registrado'}</p>
            <p><strong>Objetivos especificos:</strong> {Array.isArray(summaryData.objetivosEspecificos) ? summaryData.objetivosEspecificos.length : 0}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="closure-form">
          {questionsLoading ? (
            <div className="closure-dyn-loading"><span>Cargando preguntas...</span></div>
          ) : resolvedTemplate?.secciones ? (
            <div className="closure-dyn-questions">
              {resolvedTemplate.secciones.map((seccion) => (
                <div key={seccion.id || seccion.titulo} className="closure-dyn-section">
                  <h3 className="closure-dyn-section-title">{seccion.titulo}</h3>
                  {seccion.tipo_seccion === 'formulario' && seccion.campos?.map((campo) => {
                    if (campo.activo === false) return null;
                    const value = campo.resolvedValue || answers[campo.questionId] || '';
                    const isLinked = !!campo.questionId;
                    return (
                      <div key={campo.id} className="closure-dyn-field">
                        <label className="closure-dyn-label">
                          {campo.label}
                          {isLinked && <span className="closure-dyn-linked-badge">VINCULADO</span>}
                        </label>
                        {campo.tipo_input === 'texto_largo' ? (
                          <textarea className="closure-dyn-textarea" value={value} onChange={(e) => {
                            if (campo.questionId) updateAnswer(campo.questionId, e.target.value);
                          }} disabled={isDisabled || isLinked} maxLength={2000} rows={4} />
                        ) : campo.tipo_input === 'fecha' ? (
                          <input type="date" className="closure-dyn-input" value={value} onChange={(e) => {
                            if (campo.questionId) updateAnswer(campo.questionId, e.target.value);
                          }} disabled={isDisabled || isLinked} />
                        ) : (
                          <input type="text" className="closure-dyn-input" value={value} onChange={(e) => {
                            if (campo.questionId) updateAnswer(campo.questionId, e.target.value);
                          }} disabled={isDisabled || isLinked} maxLength={500} />
                        )}
                        {isLinked && !value && <span className="closure-dyn-empty-hint">Sin respuesta en el banco de preguntas</span>}
                      </div>
                    );
                  })}
                </div>
              ))}
            </div>
          ) : questions.length > 0 ? (
            <div className="closure-dyn-questions">
              {questions.map(renderQuestion)}
            </div>
          ) : (
            <div className="closure-dyn-empty"><p>No hay preguntas configuradas para el acta de cierre.</p></div>
          )}

          <div className="form-group">
            <label>
              <ListTodo size={16} className="label-icon" />
              Lista de Entregables Clave
            </label>
            <div className="deliverables-container-readonly" id="deliverables-list">
              {summaryData.entregables && summaryData.entregables.length > 0 ? (
                summaryData.entregables.map((entregable, index) => (
                  <div key={index} className="deliverable-item">
                    <span className="bullet">&bull;</span>
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
                <input type="text" id="avance-final" value={`${summaryData.avanceTotal}% - calculado automaticamente`} disabled />
              </div>
            </div>
          </div>

          {summaryData.puedeCerrar && summaryData.estado !== 'CERRADO' && cierreSolicitado && !isGestorOrAdmin && (
            <div className="form-actions">
              <div className="closure-ready-note">
                <Clock size={18} />
                <span>Solicitud de cierre enviada. Esperando respuesta del Gestor.</span>
              </div>
            </div>
          )}

          {canRequestClosure && (
            <div className="form-actions">
              <div className="closure-ready-note">
                <CheckCircle2 size={18} />
                <span>El proyecto esta listo para solicitar cierre.</span>
              </div>
              <button type="button" className="btn-primary-closure" onClick={handleSolicitarCierre} disabled={requestingClosure}>
                <Send size={16} style={{ marginRight: '8px' }} />
                {requestingClosure ? 'Enviando solicitud...' : 'Solicitar Cierre al Gestor'}
              </button>
            </div>
          )}

          {canCloseProject && (
            <div className="form-actions">
              <div className="closure-ready-note">
                <CheckCircle2 size={18} />
                <span>El Director ha solicitado el cierre. Puede proceder a cerrar el proyecto.</span>
              </div>
              <button type="submit" className="btn-primary-closure" disabled={submitting}>
                {submitting ? 'Procesando cierre...' : 'Cerrar Proyecto'}
              </button>
            </div>
          )}

          {summaryData.estado === 'CERRADO' && !successMsg && (
            <div className="form-actions">
              <button type="button" className="btn-primary-closure" onClick={handleDownloadActa} disabled={downloadingActa}>
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
