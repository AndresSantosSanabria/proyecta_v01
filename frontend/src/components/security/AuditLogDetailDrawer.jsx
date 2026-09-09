import {
  AlertTriangle,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock,
  Copy,
  FileText,
  Globe,
  Info,
  Layers,
  Server,
  Shield,
  Terminal,
  X,
} from 'lucide-react';
import { useCallback, useState } from 'react';

const formatDateTime = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(date);
};

const formatDateTimeShort = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(date);
};

const accionMeta = {
  CONSULTA: 'Consulta',
  CREACION: 'Creacion',
  ACTUALIZACION: 'Actualizacion',
  ELIMINACION: 'Eliminacion',
  LOGIN: 'Login',
  OTRO: 'Otro',
};

const accionBadgeClass = {
  CONSULTA: 'consulta',
  CREACION: 'creacion',
  ACTUALIZACION: 'actualizacion',
  ELIMINACION: 'eliminacion',
  LOGIN: 'login',
  OTRO: 'otro',
};

const estadoMeta = {
  SUCCESS: 'Exito',
  ERROR: 'Error',
};

const statusLabel = (code) => {
  if (code == null) return 'Sin respuesta';
  switch (code) {
    case 200: return 'OK';
    case 201: return 'Creado';
    case 204: return 'Sin contenido';
    case 301: case 302: case 307: case 308: return 'Redireccion';
    case 400: return 'Solicitud incorrecta';
    case 401: return 'No autenticado';
    case 403: return 'Acceso denegado';
    case 404: return 'No encontrado';
    case 409: return 'Conflicto';
    case 422: return 'No procesable';
    case 500: return 'Error interno del servidor';
    case 503: return 'Servicio no disponible';
    default: return `HTTP ${code}`;
  }
};

const parseJsonSafe = (str) => {
  if (!str) return null;
  try {
    return JSON.parse(str);
  } catch {
    return null;
  }
};

const extractJsonFields = (json) => {
  if (!json || typeof json !== 'object') return [];
  const result = [];
  for (const [key, value] of Object.entries(json)) {
    if (value !== null && value !== undefined && typeof value !== 'object') {
      result.push({ campo: key, valor: String(value) });
    } else if (value !== null && typeof value === 'object') {
      result.push({ campo: key, valor: JSON.stringify(value) });
    }
  }
  return result;
};

const buildFieldDiff = (requestBody, respuestaBody) => {
  const req = parseJsonSafe(requestBody);
  const res = parseJsonSafe(respuestaBody);
  if (!req || !res) return null;

  const diffs = [];
  const allKeys = new Set([...Object.keys(req), ...Object.keys(res)]);

  for (const key of allKeys) {
    const oldVal = req[key];
    const newVal = res[key];
    const oldStr = oldVal !== undefined && oldVal !== null ? String(oldVal) : '-';
    const newStr = newVal !== undefined && newVal !== null ? String(newVal) : '-';
    if (oldStr !== newStr) {
      diffs.push({ campo: key, anterior: oldStr, nuevo: newStr });
    }
  }
  return diffs.length > 0 ? diffs : null;
};

const parseDetailedError = (errorString) => {
  if (!errorString) return null;

  const result = {
    sqlstate: null,
    exceptionClass: null,
    message: errorString,
    rootCause: null,
    stackTrace: [],
    sourceFile: null,
    sqlQuery: null,
    sqlDetail: null,
    sqlHint: null,
    constraintName: null,
  };

  const sqlstateMatch = errorString.match(/SQLSTATE\[([^\]]+)\]/);
  if (sqlstateMatch) {
    result.sqlstate = sqlstateMatch[1];
  }

  const exceptionMatch = errorString.match(/Exception: ([\w.]+)/);
  if (exceptionMatch) {
    result.exceptionClass = exceptionMatch[1];
  } else if (errorString.includes('Constraint Violation:')) {
    result.exceptionClass = 'ConstraintViolationException';
    const constraintMatch = errorString.match(/Constraint Violation: (\w+)/);
    if (constraintMatch) result.constraintName = constraintMatch[1];
  } else if (errorString.includes('Data Integrity:')) {
    result.exceptionClass = 'DataIntegrityViolationException';
  }

  const causeMatch = errorString.match(/Causa ra[ií]z: ([^\n]+)/);
  if (causeMatch) {
    result.rootCause = causeMatch[1];
  }

  const sqlQueryMatch = errorString.match(/SQL: ([^\n]+)/);
  if (sqlQueryMatch) {
    result.sqlQuery = sqlQueryMatch[1];
  }

  const sqlDetailMatch = errorString.match(/DETAIL: ([^\n]+)/);
  if (sqlDetailMatch) {
    result.sqlDetail = sqlDetailMatch[1];
  }

  const sqlHintMatch = errorString.match(/HINT: ([^\n]+)/);
  if (sqlHintMatch) {
    result.sqlHint = sqlHintMatch[1];
  }

  const stackMatch = errorString.match(/Stack Trace T[eé]cnico:\n([\s\S]*)/);
  if (stackMatch) {
    const lines = stackMatch[1].split('\n').filter((line) => line.trim());
    result.stackTrace = lines;

    for (const line of lines) {
      if (line.includes('.java:') || line.includes('.php:')) {
        const fileMatch = line.match(/at\s+([\w.]+\.java:\d+)/);
        if (fileMatch) {
          result.sourceFile = fileMatch[1];
        }
        break;
      }
    }
  }

  return result;
};

const generateResumenEjecutivo = (log, isError, errorDetails) => {
  const user = log.usuarioNombre || log.usuarioId || 'SYSTEM';
  const accion = accionMeta[log.accion] || log.accion;
  const entidad = log.entidadTipo || log.modulo?.split('/').filter(Boolean).pop() || 'recurso';
  const metodo = log.metodoHttp || '-';
  const code = log.codigoEstado;
  const codeLabel = statusLabel(code);

  if (isError) {
    const excClass = errorDetails?.exceptionClass || 'Excepcion desconocida';
    const errMsg = errorDetails?.message?.split('\n')[0] || 'Sin mensaje';
    return {
      titulo: `Fallo detectado: ${accion} sobre ${entidad}`,
      descripcion: `El usuario ${user} ejecuto una operacion de tipo ${accion} (${metodo}) sobre el recurso ${entidad}. La operacion fallo con codigo HTTP ${code} (${codeLabel}).`,
      severidad: 'alta',
      exceptionResumen: `${excClass}: ${errMsg.substring(0, 200)}`,
    };
  }

  if (log.accion === 'CREACION') {
    return {
      titulo: `Registro creado exitosamente: ${entidad}`,
      descripcion: `El usuario ${user} creo un nuevo registro de tipo ${entidad} mediante una peticion ${metodo}. La operacion fue procesada correctamente con codigo HTTP ${code} (${codeLabel}).`,
      severidad: 'info',
    };
  }

  if (log.accion === 'ACTUALIZACION') {
    return {
      titulo: `Actualizacion exitosa: ${entidad}`,
      descripcion: `El usuario ${user} actualizo un registro de tipo ${entidad} mediante una peticion ${metodo}. Los cambios fueron persistidos correctamente con codigo HTTP ${code} (${codeLabel}).`,
      severidad: 'info',
    };
  }

  if (log.accion === 'ELIMINACION') {
    return {
      titulo: `Registro eliminado: ${entidad}`,
      descripcion: `El usuario ${user} elimino un registro de tipo ${entidad} mediante una peticion ${metodo}. La operacion se ejecuto con codigo HTTP ${code} (${codeLabel}).`,
      severidad: 'media',
    };
  }

  return {
    titulo: `${accion} ejecutada sobre ${entidad}`,
    descripcion: `El usuario ${user} ejecuto una operacion de tipo ${accion} (${metodo}) sobre ${entidad}. Resultado: HTTP ${code} (${codeLabel}).`,
    severidad: 'baja',
  };
};

const generateRecomendaciones = (log, isError, errorDetails) => {
  const recs = [];

  if (isError) {
    const excClass = errorDetails?.exceptionClass || '';

    if (excClass.includes('ConstraintViolation') || excClass.includes('DataIntegrity') || errorDetails?.sqlstate?.startsWith('23')) {
      recs.push({
        titulo: 'Validar integridad referencial',
        descripcion: 'Verificar que los datos enviados no violan restricciones de unicidad, claves foraneas o integridad referencial en la base de datos.',
        prioridad: 'alta',
      });
      if (errorDetails?.constraintName) {
        recs.push({
          titulo: `Revisar restriccion: ${errorDetails.constraintName}`,
          descripcion: `La constraint "${errorDetails.constraintName}" esta siendo violada. Revisar la definicion del indice en PostgreSQL y validar los datos en el formulario antes del envio.`,
          prioridad: 'alta',
        });
      }
    }

    if (excClass.includes('NullPointerException') || excClass.includes('IllegalArgument')) {
      recs.push({
        titulo: 'Corregir validaciones en el servidor',
        descripcion: 'Se detecto un error de validacion de datos. Agregar validaciones null-safe en el backend y validaciones de formulario en el frontend antes del envio.',
        prioridad: 'alta',
      });
    }

    if (excClass.includes('DataIntegrity') || errorDetails?.sqlstate?.startsWith('23')) {
      recs.push({
        titulo: 'Verificar datos duplicados',
        descripcion: 'El error sugiere que se esta intentando insertar un registro duplicado. Implementar validacion previa en el FormRequest o usar MERGE/UPSERT.',
        prioridad: 'media',
      });
    }

    if (log.codigoEstado === 500) {
      recs.push({
        titulo: 'Revisar logs del servidor',
        descripcion: `Examinar los logs completos de la aplicacion en el servidor para la traza completa del error. Buscar la excepcion raiz en los logs de Spring Boot.`,
        prioridad: 'media',
      });
    }

    if (errorDetails?.sqlQuery) {
      recs.push({
        titulo: 'Analizar consulta SQL',
        descripcion: `La consulta SQL que fallo: "${errorDetails.sqlQuery.substring(0, 150)}...". Revisar la consulta en un cliente SQL para diagnosticar el problema exacto.`,
        prioridad: 'media',
      });
    }

    if (log.codigoEstado === 401 || log.codigoEstado === 403) {
      recs.push({
        titulo: 'Verificar permisos y autenticacion',
        descripcion: 'El usuario no tiene los permisos necesarios para ejecutar esta operacion. Verificar la configuracion de roles y permisos en el sistema de seguridad.',
        prioridad: 'media',
      });
    }
  } else {
    if (log.accion === 'ACTUALIZACION') {
      recs.push({
        titulo: 'Auditar cambios periodicamente',
        descripcion: 'Realizar revision periodica de las actualizaciones realizadas para garantizar la consistencia de los datos y detectar cambios no autorizados.',
        prioridad: 'baja',
      });
    }
    if (log.accion === 'ELIMINACION') {
      recs.push({
        titulo: 'Verificar eliminacion intencional',
        descripcion: 'Confirmar que la eliminacion del registro fue intencional y que no afecta la integridad referencial de otros registros en el sistema.',
        prioridad: 'media',
      });
    }
    if (log.duracionMs && log.duracionMs > 5000) {
      recs.push({
        titulo: 'Optimizar rendimiento',
        descripcion: `La operacion tardo ${log.duracionMs}ms, lo cual excede el umbral recomendado de 5 segundos. Considerar optimizar la consulta SQL o implementar cache.`,
        prioridad: 'media',
      });
    }
  }

  if (recs.length === 0) {
    recs.push({
      titulo: 'Monitoreo continuo',
      descripcion: 'Continuar monitoreando el sistema para detectar patrones de errores o comportamientos inusuales.',
      prioridad: 'baja',
    });
  }

  return recs;
};

const CopyButton = ({ text }) => {
  const [copied, setCopied] = useState(false);
  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }, [text]);
  return (
    <button type="button" className="audit-copy-btn" onClick={handleCopy} title="Copiar">
      <Copy size={13} />
      {copied ? 'Copiado' : ''}
    </button>
  );
};

const AuditLogDetailDrawer = ({ log, onClose }) => {
  const [showStack, setShowStack] = useState(false);
  const [showCause, setShowCause] = useState(false);
  const [showRequestBody, setShowRequestBody] = useState(false);
  const [showResponseBody, setShowResponseBody] = useState(false);

  const accion = accionMeta[log.accion] || log.accion || '-';
  const accionBadge = accionBadgeClass[log.accion] || '';
  const estado = estadoMeta[log.estado] || log.estado || '-';
  const isError = String(log.estado || '').toUpperCase() === 'ERROR' || (log.codigoEstado != null && log.codigoEstado >= 400);
  const code = log.codigoEstado;
  const errorDetails = parseDetailedError(log.trazaError);

  const resumen = generateResumenEjecutivo(log, isError, errorDetails);
  const fieldDiff = buildFieldDiff(log.requestBody, log.respuestaBody);
  const requestBodyJson = parseJsonSafe(log.requestBody);
  const respuestaBodyJson = parseJsonSafe(log.respuestaBody);
  const recomendacones = generateRecomendaciones(log, isError, errorDetails);

  const entidadLabel = log.entidadTipo || log.modulo?.split('/').filter(Boolean).pop() || '-';

  return (
    <div className="audit-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="audit-modal audit-modal--report"
        role="dialog"
        aria-modal="true"
        aria-label="Reporte de Auditoria"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <header className="audit-modal-header audit-report-header">
          <div className="audit-modal-header-content">
            <div className="audit-modal-icon">
              <FileText size={20} />
            </div>
            <div>
              <h2 className="audit-modal-title">Reporte de Auditoria</h2>
              <p className="audit-modal-subtitle">
                ID: {log.id?.substring(0, 8)}... | {formatDateTimeShort(log.fechaCreacion)}
              </p>
            </div>
          </div>
          <button type="button" className="audit-modal-close" onClick={onClose} aria-label="Cerrar">
            <X size={18} />
          </button>
        </header>

        <div className="audit-modal-body audit-report-body">

          {/* ───────────────────────────────────────────── */}
          {/* 1. RESUMEN EJECUTIVO                          */}
          {/* ───────────────────────────────────────────── */}
          <section className="audit-report-section">
            <div className="audit-report-section-header">
              <div className={`audit-report-severity audit-report-severity--${resumen.severidad}`}>
                {isError ? <AlertTriangle size={16} /> : <CheckCircle2 size={16} />}
              </div>
              <div>
                <h3 className="audit-report-section-title">1. Resumen Ejecutivo</h3>
                <p className="audit-report-section-subtitle">Que sucedio en esta transaccion</p>
              </div>
            </div>

            <div className={`audit-report-resumen-card audit-report-resumen-card--${resumen.severidad}`}>
              <h4 className="audit-report-resumen-titulo">{resumen.titulo}</h4>
              <p className="audit-report-resumen-descripcion">{resumen.descripcion}</p>
              {resumen.exceptionResumen && (
                <div className="audit-report-resumen-error">
                  <AlertTriangle size={14} />
                  <span>{resumen.exceptionResumen}</span>
                </div>
              )}
            </div>

            <div className="audit-report-meta-grid">
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Globe size={13} /> ORIGEN IP
                </span>
                <span className="audit-report-meta-value">{log.ipOrigen || '-'}</span>
              </div>
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Shield size={13} /> USUARIO
                </span>
                <span className="audit-report-meta-value">
                  {log.usuarioNombre || log.usuarioId || 'SYSTEM'}
                  {log.usuarioRol && (
                    <span className="audit-report-role-pill">{log.usuarioRol}</span>
                  )}
                </span>
              </div>
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Layers size={13} /> ENTIDAD
                </span>
                <span className="audit-report-meta-value">
                  {entidadLabel}
                  {log.entidadId && (
                    <span className="audit-report-entity-id">{log.entidadId.substring(0, 8)}...</span>
                  )}
                </span>
              </div>
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Server size={13} /> RESPUESTA
                </span>
                <span className="audit-report-meta-value">
                  <span className={`audit-http-badge ${code < 300 ? 'ok' : code < 400 ? 'redir' : code < 500 ? 'client' : 'server'}`}>
                    {code} {statusLabel(code)}
                  </span>
                </span>
              </div>
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Clock size={13} /> DURACION
                </span>
                <span className="audit-report-meta-value">
                  {log.duracionMs != null ? `${log.duracionMs} ms` : '-'}
                </span>
              </div>
              <div className="audit-report-meta-item">
                <span className="audit-report-meta-label">
                  <Terminal size={13} /> USER AGENT
                </span>
                <span className="audit-report-meta-value audit-report-user-agent">
                  {log.userAgent || '-'}
                </span>
              </div>
            </div>
          </section>

          {/* ───────────────────────────────────────────── */}
          {/* 2. ANALISIS DE CAMBIOS / DETALLE TECNICO      */}
          {/* ───────────────────────────────────────────── */}
          <section className="audit-report-section">
            <div className="audit-report-section-header">
              <div className="audit-report-section-icon">
                <Layers size={16} />
              </div>
              <div>
                <h3 className="audit-report-section-title">2. Analisis de Cambios / Detalle Tecnico</h3>
                <p className="audit-report-section-subtitle">
                  {isError
                    ? 'Detalles tecnicos de la excepcion y operacion afectada'
                    : 'Comparativa de campos alterados en la transaccion'}
                </p>
              </div>
            </div>

            {isError && errorDetails && (
              <div className="audit-report-error-analysis">
                <div className="audit-report-error-summary">
                  {errorDetails.sqlstate && (
                    <div className="audit-report-error-chip audit-report-error-chip--sqlstate">
                      SQLSTATE: {errorDetails.sqlstate}
                    </div>
                  )}
                  {errorDetails.exceptionClass && (
                    <div className="audit-report-error-chip audit-report-error-chip--exception">
                      {errorDetails.exceptionClass}
                    </div>
                  )}
                  {errorDetails.constraintName && (
                    <div className="audit-report-error-chip audit-report-error-chip--constraint">
                      Constraint: {errorDetails.constraintName}
                    </div>
                  )}
                </div>

                <div className="audit-report-error-message">
                  <span className="audit-report-error-message-label">Mensaje de error:</span>
                  <p>{errorDetails.message?.split('\n')[0]}</p>
                </div>

                {errorDetails.sqlQuery && (
                  <div className="audit-report-sql-block">
                    <div className="audit-report-sql-header">
                      <span>Consulta SQL Afectada</span>
                      <CopyButton text={errorDetails.sqlQuery} />
                    </div>
                    <pre className="audit-report-sql-code">{errorDetails.sqlQuery}</pre>
                  </div>
                )}

                {errorDetails.sqlDetail && (
                  <div className="audit-report-sql-block audit-report-sql-block--detail">
                    <div className="audit-report-sql-header">
                      <span>Detalle PostgreSQL</span>
                    </div>
                    <pre className="audit-report-sql-code">{errorDetails.sqlDetail}</pre>
                  </div>
                )}

                {errorDetails.sqlHint && (
                  <div className="audit-report-sql-block audit-report-sql-block--hint">
                    <div className="audit-report-sql-header">
                      <span>Sugerencia (HINT)</span>
                    </div>
                    <pre className="audit-report-sql-code">{errorDetails.sqlHint}</pre>
                  </div>
                )}

                <div className="audit-report-error-meta-row">
                  <div className="audit-report-error-meta-cell">
                    <span className="audit-report-error-meta-label">ARCHIVO DE ORIGEN</span>
                    <span className="audit-report-error-meta-value">{errorDetails.sourceFile || '-'}</span>
                  </div>
                  <div className="audit-report-error-meta-cell">
                    <span className="audit-report-error-meta-label">CLASE EXCEPTION</span>
                    <span className="audit-report-error-meta-value">{errorDetails.exceptionClass || '-'}</span>
                  </div>
                </div>
              </div>
            )}

            {isError && !errorDetails && (
              <div className="audit-report-no-data">
                <Info size={18} />
                <span>No se capturaron detalles tecnicos de la excepcion.</span>
              </div>
            )}

            {!isError && fieldDiff && (
              <div className="audit-report-diff-section">
                <h4 className="audit-report-subtitle">Tabla Comparativa de Cambios</h4>
                <div className="audit-report-diff-table-wrap">
                  <table className="audit-report-diff-table">
                    <thead>
                      <tr>
                        <th>Campo</th>
                        <th>Valor Anterior (Request)</th>
                        <th>Valor Nuevo (Response)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {fieldDiff.map((row, idx) => (
                        <tr key={idx}>
                          <td className="audit-report-diff-campo">{row.campo}</td>
                          <td className="audit-report-diff-anterior">{row.anterior}</td>
                          <td className="audit-report-diff-nuevo">{row.nuevo}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {!isError && !fieldDiff && log.requestBody && (
              <div className="audit-report-diff-section">
                <h4 className="audit-report-subtitle">Datos de la Transaccion</h4>
                <div className="audit-report-json-columns">
                  <div className="audit-report-json-col">
                    <div className="audit-report-json-header">
                      <span>Payload Enviado (Request)</span>
                      <button
                        type="button"
                        className="audit-report-toggle-btn"
                        onClick={() => setShowRequestBody(!showRequestBody)}
                      >
                        {showRequestBody ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                        {showRequestBody ? 'Ocultar' : 'Ver JSON'}
                      </button>
                    </div>
                    {showRequestBody && (
                      <pre className="audit-report-code-block">
                        {JSON.stringify(requestBodyJson || log.requestBody, null, 2)}
                      </pre>
                    )}
                  </div>
                  <div className="audit-report-json-col">
                    <div className="audit-report-json-header">
                      <span>Respuesta del Servidor (Response)</span>
                      <button
                        type="button"
                        className="audit-report-toggle-btn"
                        onClick={() => setShowResponseBody(!showResponseBody)}
                      >
                        {showResponseBody ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                        {showResponseBody ? 'Ocultar' : 'Ver JSON'}
                      </button>
                    </div>
                    {showResponseBody && (
                      <pre className="audit-report-code-block audit-report-code-block--response">
                        {JSON.stringify(respuestaBodyJson || log.respuestaBody || 'Sin respuesta capturada', null, 2)}
                      </pre>
                    )}
                  </div>
                </div>
              </div>
            )}

            {!isError && !log.requestBody && (
              <div className="audit-report-no-data">
                <Info size={18} />
                <span>No se capturaron datos de payload para esta operacion (posiblemente una consulta GET).</span>
              </div>
            )}
          </section>

          {/* ───────────────────────────────────────────── */}
          {/* 3. DIAGNOSTICO DE CAUSA RAIZ                  */}
          {/* ───────────────────────────────────────────── */}
          <section className="audit-report-section">
            <div className="audit-report-section-header">
              <div className="audit-report-section-icon audit-report-section-icon--warning">
                <AlertTriangle size={16} />
              </div>
              <div>
                <h3 className="audit-report-section-title">3. Diagnostico de Causa Raiz</h3>
                <p className="audit-report-section-subtitle">
                  {isError
                    ? 'Analisis detallado de por que ocurrio el fallo'
                    : 'Motivo del cambio de estado en la transaccion'}
                </p>
              </div>
            </div>

            {isError && errorDetails && (
              <div className="audit-report-diagnosis">
                <div className="audit-report-diagnosis-card audit-report-diagnosis-card--cause">
                  <h4>Causa Identificada</h4>
                  {errorDetails.rootCause ? (
                    <p>{errorDetails.rootCause}</p>
                  ) : (
                    <p>
                      La excepcion <strong>{errorDetails.exceptionClass || 'desconocida'}</strong> fue
                      disparada durante la ejecucion de la operacion. El mensaje del error indica:
                      &quot;{errorDetails.message?.split('\n')[0]}&quot;.
                    </p>
                  )}
                </div>

                {errorDetails.sqlQuery && (
                  <div className="audit-report-diagnosis-card">
                    <h4>Operacion SQL Afectada</h4>
                    <p>
                      La consulta que fallo opera sobre la tabla asociada a la entidad
                      <strong> {entidadLabel}</strong>. La consulta completa es:
                    </p>
                    <pre className="audit-report-code-block audit-report-code-block--mini">
                      {errorDetails.sqlQuery}
                    </pre>
                  </div>
                )}

                {errorDetails.sqlstate && (
                  <div className="audit-report-diagnosis-card">
                    <h4>Clasificacion del Error SQL</h4>
                    <p>
                      <strong>SQLSTATE {errorDetails.sqlstate}</strong> -{' '}
                      {errorDetails.sqlstate?.startsWith('23')
                        ? 'Viola restriccion de integridad de datos (unique, foreign key, not null, check).'
                        : errorDetails.sqlstate?.startsWith('42')
                          ? 'Error de sintaxis o nombre de objeto inexistente en la base de datos.'
                          : errorDetails.sqlstate?.startsWith('08')
                            ? 'Error de conexion con la base de datos.'
                            : errorDetails.sqlstate?.startsWith('57')
                              ? 'Interrupcion de la operacion por timeout o cancelacion.'
                              : 'Consulte la documentacion de PostgreSQL para esta clasificacion SQLSTATE.'}
                    </p>
                  </div>
                )}

                {errorDetails?.stackTrace?.length > 0 && (
                  <div className="audit-report-diagnosis-card">
                    <h4>Traza de Ejecucion (Stack Trace)</h4>
                    <button
                      type="button"
                      className="audit-report-toggle-btn"
                      onClick={() => setShowStack(!showStack)}
                    >
                      {showStack ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      {showStack ? 'Ocultar Stack Trace' : `Ver Stack Trace (${errorDetails.stackTrace.length} frames)`}
                    </button>
                    {showStack && (
                      <pre className="audit-report-code-block audit-report-code-block--stacktrace">
                        {errorDetails.stackTrace.map((line, i) => (
                          <div key={i} className="audit-stack-line">
                            <span className="audit-stack-number">#{i}</span>
                            <span className="audit-stack-content">{line}</span>
                          </div>
                        ))}
                      </pre>
                    )}
                  </div>
                )}
              </div>
            )}

            {isError && !errorDetails && (
              <div className="audit-report-diagnosis">
                <div className="audit-report-diagnosis-card audit-report-diagnosis-card--cause">
                  <h4>Causa Identificada</h4>
                  <p>
                    La operacion fallo con codigo HTTP {code} ({statusLabel(code)}).
                    No se captaron detalles tecnicos adicionales de la excepcion.
                  </p>
                </div>
              </div>
            )}

            {!isError && (
              <div className="audit-report-diagnosis">
                <div className="audit-report-diagnosis-card audit-report-diagnosis-card--success">
                  <h4>Resultado de la Operacion</h4>
                  {log.accion === 'CREACION' && (
                    <p>
                      La operacion de creacion se ejecuto correctamente. El servidor接受 los datos
                      del payload enviado y genero un nuevo registro en la base de datos con
                      codigo de respuesta HTTP {code} ({statusLabel(code)}).
                    </p>
                  )}
                  {log.accion === 'ACTUALIZACION' && (
                    <p>
                      La operacion de actualizacion se ejecuto correctamente. Los campos
                      modificados en el payload fueron persistidos en la base de datos con
                      codigo de respuesta HTTP {code} ({statusLabel(code)}).
                      {fieldDiff && ` Se detectaron ${fieldDiff.length} campo(s) modificado(s).`}
                    </p>
                  )}
                  {log.accion === 'ELIMINACION' && (
                    <p>
                      La operacion de eliminacion se ejecuto correctamente. El registro fue
                      eliminado (o marcado como eliminado) con codigo HTTP {code} ({statusLabel(code)}).
                    </p>
                  )}
                  {log.accion === 'CONSULTA' && (
                    <p>
                      La consulta se ejecuto correctamente. El servidor devolvio los datos
                      solicitados con codigo HTTP {code} ({statusLabel(code)}).
                    </p>
                  )}
                  {log.accion === 'LOGIN' && (
                    <p>
                      La autenticacion del usuario fue exitosa. Se establecio la sesion
                      correctamente con codigo HTTP {code} ({statusLabel(code)}).
                    </p>
                  )}
                  {['CREACION', 'ACTUALIZACION', 'ELIMINACION', 'CONSULTA', 'LOGIN'].indexOf(log.accion) === -1 && (
                    <p>
                      La operacion de tipo {accion} se ejecuto con resultado exitoso
                      (HTTP {code} - {statusLabel(code)}).
                    </p>
                  )}
                </div>
              </div>
            )}
          </section>

          {/* ───────────────────────────────────────────── */}
          {/* 4. RECOMENDACION / PLAN DE ACCION             */}
          {/* ───────────────────────────────────────────── */}
          <section className="audit-report-section">
            <div className="audit-report-section-header">
              <div className="audit-report-section-icon audit-report-section-icon--action">
                <CheckCircle2 size={16} />
              </div>
              <div>
                <h3 className="audit-report-section-title">4. Recomendacion / Plan de Accion</h3>
                <p className="audit-report-section-subtitle">
                  Pasos concretos para resolver o prevenir que vuelva a ocurrir
                </p>
              </div>
            </div>

            <div className="audit-report-recommendations">
              {recomendacones.map((rec, idx) => (
                <div key={idx} className={`audit-report-rec-card audit-report-rec-card--${rec.prioridad}`}>
                  <div className="audit-report-rec-header">
                    <span className="audit-report-rec-number">{idx + 1}</span>
                    <div>
                      <h4 className="audit-report-rec-titulo">{rec.titulo}</h4>
                      <span className={`audit-report-rec-prioridad audit-report-rec-prioridad--${rec.prioridad}`}>
                        {rec.prioridad}
                      </span>
                    </div>
                  </div>
                  <p className="audit-report-rec-descripcion">{rec.descripcion}</p>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
};

export default AuditLogDetailDrawer;
