import React from 'react';
import {
  Lock,
  FileText,
  Upload,
  Eye,
  ChevronDown,
  ChevronRight,
  Layers,
  Target,
  Package,
  Download,
  X,
  Loader2,
} from 'lucide-react';
import EvidenceUpload from '../../common/EvidenceUpload';
import apiClient from '../../../api/axiosConfig';
import { usePermission } from '../../../hooks/usePermission';

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const toDisplayPercent = (value) => {
  const numeric = toNumber(value);
  if (Math.abs(numeric) <= 1) {
    return numeric * 100;
  }
  return numeric;
};

const getEvidenciaFile = (ent) => ent.evidenciaPdf || ent.archivo || ent.evidencia || null;
const getEvidenciaUrl = (ent) => ent.evidenciaUrl || ent.descargaUrl || null;
const getEntregableId = (ent) => ent.entregableId || ent.id;
const getFechaEntrega = (ent) => ent.fechaEntrega || ent.fechaEntregaReal || null;
const getDiasAtraso = (ent) => ent.diasAtraso ?? ent.atraso ?? null;

const normalizeEvidencePath = (url) => {
  if (!url) return '';
  return String(url)
    .trim()
    .replace(/^https?:\/\/[^/]+\/api\/v1/i, '')
    .replace(/^\/api\/v1/i, '')
    .replace(/^\//, '');
};

const isPdfBlob = async (blob) => {
  if (!(blob instanceof Blob) || blob.size === 0) {
    return false;
  }
  const signature = new Uint8Array(await blob.slice(0, 5).arrayBuffer());
  const text = new TextDecoder('ascii').decode(signature);
  return text.startsWith('%PDF-');
};

const getEvidenceBlob = async (url) => {
  const path = normalizeEvidencePath(url);
  if (!path) {
    throw new Error('No se encontró la URL de la evidencia.');
  }

  const response = await apiClient.get(path, { responseType: 'blob' });
  const blob = response.data;

  if (!(await isPdfBlob(blob))) {
    throw new Error('El archivo recibido no es un PDF válido.');
  }

  return blob;
};

const semaforoDias = (dias, tieneEvidencia) => {
  if (dias == null) return 'neutral';
  if (dias < 0) return 'danger';
  if (!tieneEvidencia && dias <= 8) return 'warning';
  return 'success';
};

const NodeMetric = ({ value, suffix = '%' }) => (
  <span className="metric-pill">
    {Number.isFinite(value) ? value.toFixed(2) : '0.00'}
    {suffix}
  </span>
);

const TreeTableRow = ({ fase, isExpanded, toggleNode, onOpenEvidence, onPreviewEvidence }) => {
  const canUploadEvidence = usePermission('EVIDENCIA:CARGAR');
  const ponderacionFase = toNumber(fase.ponderacion);
  const programadoFase = toNumber(fase.progresoProgramado ?? fase.avanceProgramado ?? fase.avance ?? 0);
  const ejecutadoFase = toNumber(fase.progresoEjecutado ?? fase.avance ?? 0);
  const diferenciaFase = toNumber(fase.diferencia ?? (programadoFase - ejecutadoFase));
  const eficaciaFase = toNumber(fase.eficacia ?? (programadoFase === 0 ? 1 : ejecutadoFase / programadoFase));

  return (
    <>
      <tr className="row-phase clickable-row" onClick={() => toggleNode(`fase-${fase.id}`)}>
        <td className="col-node indent-1">
          <div className="node-title">
            {isExpanded(`fase-${fase.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            <span className="node-badge fase-badge">
              <Layers size={12} /> FASE
            </span>
            <strong>{fase.nombre}</strong>
          </div>
        </td>
        <td>{ponderacionFase.toFixed(0)}%</td>
        <td><NodeMetric value={programadoFase} /></td>
        <td><NodeMetric value={ejecutadoFase} /></td>
        <td><NodeMetric value={diferenciaFase} /></td>
        <td>
          <span className={`status-badge ${fase.estado === 'ATRASO' ? 'danger' : 'success'}`}>
            {fase.estado || 'EN_TIEMPO'}
          </span>
        </td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
        <td>{(eficaciaFase * 100).toFixed(1)}%</td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
      </tr>

      {isExpanded(`fase-${fase.id}`) && (fase.hitos || []).map((hito) => {
        const ponderacionHito = toNumber(hito.ponderacion);
        const programadoHito = toNumber(hito.progresoProgramado ?? hito.avanceProgramado ?? hito.avance ?? 0);
        const ejecutadoHito = toNumber(hito.progresoEjecutado ?? hito.avance ?? 0);
        const diferenciaHito = toNumber(hito.diferencia ?? (programadoHito - ejecutadoHito));
        const eficaciaHito = toNumber(hito.eficacia ?? (programadoHito === 0 ? 1 : ejecutadoHito / programadoHito));

        return (
          <React.Fragment key={hito.id}>
            <tr className="row-milestone clickable-row" onClick={() => toggleNode(`hito-${hito.id}`)}>
              <td className="col-node indent-2">
                <div className="node-title">
                  {isExpanded(`hito-${hito.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                  <span className="node-badge hito-badge">
                    <Target size={12} /> HITO
                  </span>
                  <span>{hito.nombre}</span>
                </div>
              </td>
              <td>{ponderacionHito.toFixed(0)}%</td>
              <td><NodeMetric value={programadoHito} /></td>
              <td><NodeMetric value={ejecutadoHito} /></td>
              <td><NodeMetric value={diferenciaHito} /></td>
              <td>
                <span className={`status-badge ${hito.estado === 'ATRASO' ? 'danger' : 'success'}`}>
                  {hito.estado || 'EN_TIEMPO'}
                </span>
              </td>
              <td>—</td>
              <td>—</td>
              <td>—</td>
              <td>{(eficaciaHito * 100).toFixed(1)}%</td>
              <td>—</td>
              <td>—</td>
              <td>—</td>
            </tr>

            {isExpanded(`hito-${hito.id}`) && (hito.entregables || []).map((ent) => {
              const evidenciaFile = getEvidenciaFile(ent);
              const evidenciaUrl = getEvidenciaUrl(ent);
              const fechaEntrega = getFechaEntrega(ent);
              const entregableId = getEntregableId(ent);
              const tieneDocumento = Boolean(evidenciaUrl);
              const programadoEnt = toNumber(ent.progresoProgramado ?? ent.avanceProgramado ?? ent.avance ?? 0);
              const ejecutadoEnt = toNumber(ent.progresoEjecutado ?? ent.avance ?? 0);
              const diferenciaEnt = toNumber(ent.diferencia ?? (programadoEnt - ejecutadoEnt));
              const eficaciaEnt = toDisplayPercent(ent.eficacia ?? (programadoEnt === 0 ? 1 : ejecutadoEnt / programadoEnt));
              const diasAtraso = getDiasAtraso(ent);
              const semaforo = semaforoDias(diasAtraso, tieneDocumento);
              const fechaEntregaValida = fechaEntrega && ent.fechaLimite
                ? new Date(fechaEntrega) <= new Date(ent.fechaLimite)
                : false;
              const eficienciaEnt = tieneDocumento && fechaEntregaValida ? 100 : 0;

              return (
                <tr key={entregableId} className="row-deliverable">
                  <td className="col-node indent-3">
                    <div className="deliverable-content">
                      <div className="node-title">
                        <span className="node-badge entregable-badge">
                          <Package size={12} /> ENTREGABLE
                        </span>
                        <span>{ent.nombre}</span>
                      </div>
                      {tieneDocumento && (
                        <button
                          className="file-link-btn"
                          onClick={(e) => {
                            e.stopPropagation();
                            onPreviewEvidence(ent);
                          }}
                          title="Ver evidencia"
                        >
                          <Eye size={14} /> {evidenciaFile}
                        </button>
                      )}
                    </div>
                  </td>
                  <td>{toNumber(ent.ponderacion).toFixed(0)}%</td>
                  <td>{ent.fechaLimite || '—'}</td>
                  <td><NodeMetric value={programadoEnt} /></td>
                  <td><NodeMetric value={ejecutadoEnt} /></td>
                  <td><NodeMetric value={diferenciaEnt} /></td>
                  <td>
                    <span className={`status-badge ${semaforo}`}>
                      {ent.estado || (tieneDocumento ? 'A_CONFORMIDAD' : 'PENDIENTE')}
                    </span>
                  </td>
                  <td>{fechaEntrega || '—'}</td>
                  <td>{diasAtraso == null ? '—' : `${diasAtraso >= 0 ? '+' : ''}${diasAtraso}d`}</td>
                  <td>{eficaciaEnt.toFixed(1)}%</td>
                  <td>{eficienciaEnt.toFixed(1)}%</td>
                  <td className="action-col">
                    {!tieneDocumento && canUploadEvidence ? (
                      <button
                        className="btn-upload"
                        onClick={(e) => {
                          e.stopPropagation();
                          onOpenEvidence(entregableId);
                        }}
                      >
                        <Upload size={14} /> Marcar OK + subir PDF
                      </button>
                    ) : '—'}
                  </td>
                </tr>
              );
            })}
          </React.Fragment>
        );
      })}
    </>
  );
};

const ProgressTreeTable = ({ progressData, excelSummary, isExpanded, toggleNode, proyectoId, onEvidenceUploaded }) => {
  const [showEvidenceModal, setShowEvidenceModal] = React.useState(null);
  const [previewEvidence, setPreviewEvidence] = React.useState({ open: false, loading: false, error: '', name: '', url: '', objectUrl: '' });

  React.useEffect(() => {
    return () => {
      if (previewEvidence.objectUrl) {
        window.URL.revokeObjectURL(previewEvidence.objectUrl);
      }
    };
  }, [previewEvidence.objectUrl]);

  const handleOpenEvidence = (entregableId) => setShowEvidenceModal(entregableId);
  const handleCloseEvidence = () => setShowEvidenceModal(null);
  const handleEvidenceSuccess = () => {
    setShowEvidenceModal(null);
    if (onEvidenceUploaded) onEvidenceUploaded();
  };

  const setPreviewError = (message) => {
    setPreviewEvidence((current) => ({
      ...current,
      loading: false,
      error: message,
      objectUrl: '',
    }));
  };

  const handleDownloadEvidencia = async (ent) => {
    const url = getEvidenciaUrl(ent);
    const file = getEvidenciaFile(ent);
    if (!url) return;
    try {
      const blob = await getEvidenceBlob(url);
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', file || 'evidencia.pdf');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch {
      setPreviewError('No fue posible descargar la evidencia. El archivo no es un PDF válido o no está disponible.');
    }
  };

  const closePreviewEvidence = () => {
    setPreviewEvidence((current) => {
      if (current.objectUrl) {
        window.URL.revokeObjectURL(current.objectUrl);
      }
      return { open: false, loading: false, error: '', name: '', url: '', objectUrl: '' };
    });
  };

  const handlePreviewEvidencia = async (ent) => {
    const url = getEvidenciaUrl(ent);
    const file = getEvidenciaFile(ent);
    if (!url) return;

    setPreviewEvidence((current) => {
      if (current.objectUrl) {
        window.URL.revokeObjectURL(current.objectUrl);
      }
      return {
        open: true,
        loading: true,
        error: '',
        name: file || 'evidencia.pdf',
        url,
        objectUrl: '',
      };
    });

    try {
      const blob = await getEvidenceBlob(url);
      const objectUrl = window.URL.createObjectURL(blob);
      setPreviewEvidence((current) => ({
        ...current,
        loading: false,
        objectUrl,
      }));
    } catch (error) {
      console.error('Preview error:', error);
      setPreviewError('No fue posible previsualizar la evidencia. El archivo no es un PDF válido o no está disponible.');
    }
  };

  React.useEffect(() => {
    if (!previewEvidence.open) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        closePreviewEvidence();
      }
    };

    document.body.classList.add('modal-open');
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.classList.remove('modal-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [previewEvidence.open]);

  const avanceTotal = toNumber(progressData.progresoEjecutado ?? progressData.avanceTotal ?? 0);
  const corte = progressData.corte ? new Date(progressData.corte).toLocaleDateString('es-CO') : new Date().toLocaleDateString('es-CO');
  const dependencia = excelSummary?.dependencia || progressData.dependencia || '—';
  const entregablesProgramados = toNumber(
    progressData.entregablesProgramadosAlCorte
    ?? excelSummary?.programadosAlCorte
    ?? progressData.entregablesProgramados
    ?? 0,
  );
  const entregablesConformes = toNumber(
    progressData.entregablesEntregadosAlCorte
    ?? excelSummary?.entregadosAlCorte
    ?? progressData.entregablesConformes
    ?? progressData.entregablesConformidad
    ?? 0,
  );
  const entregablesATiempo = toNumber(
    progressData.entregablesEntregadosATiempo
    ?? excelSummary?.entregadosATiempo
    ?? 0,
  );
  const eficacia = toDisplayPercent(
    progressData.eficacia
    ?? excelSummary?.eficacia
    ?? (entregablesProgramados === 0 ? 0 : entregablesConformes / entregablesProgramados),
  );
  const eficiencia = toDisplayPercent(
    progressData.eficiencia
    ?? excelSummary?.eficiencia
    ?? (entregablesConformes === 0 ? 0 : entregablesATiempo / entregablesConformes),
  );

  return (
    <div className="detailed-table-container">
      <div className="table-header">
        <div>
          <h2>Avance detallado - {progressData.nombre}</h2>
          <div className="table-subtitle">
            <span className={`status-badge ${progressData.estado === 'ATRASO' ? 'danger' : 'success'}`}>
              {progressData.estado || 'EN_TIEMPO'}
            </span>
            <span className="corte-date">Corte: {corte}</span>
          </div>
        </div>
        <div className="header-actions">
          <button className="btn-report" type="button">
            <FileText size={16} /> Generar reporte
          </button>
        </div>
      </div>

      <div className="tree-table-wrapper">
        <table className="tree-table excel-table">
          <thead>
            <tr>
              <th className="col-node">Meta / Proyecto</th>
              <th className="col-ponderacion">Ponderación <Lock size={12} /></th>
              <th className="col-fecha">Programado <Lock size={12} /></th>
              <th className="col-avance">Avance <Lock size={12} /></th>
              <th className="col-avance">Diferencia <Lock size={12} /></th>
              <th className="col-estado">Estado <Lock size={12} /></th>
              <th className="col-avance">Total entregables <Lock size={12} /></th>
              <th className="col-fecha">Programados al corte <Lock size={12} /></th>
              <th className="col-fecha">Entregados al corte <Lock size={12} /></th>
              <th className="col-avance">Eficacia <Lock size={12} /></th>
              <th className="col-avance">Eficiencia <Lock size={12} /></th>
              <th className="col-fecha">Dependencia <Lock size={12} /></th>
            </tr>
          </thead>
          <tbody>
            <tr className="row-root">
              <td className="col-node">
                <strong>{progressData.codigo} — {progressData.nombre}</strong>
              </td>
              <td>100%</td>
              <td><NodeMetric value={toNumber(progressData.progresoProgramado)} /></td>
              <td>
                <div className="progress-cell">
                  <div className="progress-bar-bg small">
                    <div className="progress-bar-fill" style={{ width: `${avanceTotal}%` }} />
                  </div>
                  <span>{avanceTotal.toFixed(2)}%</span>
                </div>
              </td>
              <td><NodeMetric value={toNumber(progressData.diferencia)} /></td>
              <td>
                <span className={`status-badge ${progressData.estado === 'ATRASO' ? 'danger' : 'success'}`}>
                  {progressData.estado || 'EN_TIEMPO'}
                </span>
              </td>
              <td>{toNumber(progressData.entregablesTotal).toFixed(0)}</td>
              <td>{entregablesProgramados.toFixed(0)}</td>
              <td>{entregablesConformes.toFixed(0)}</td>
              <td>{eficacia.toFixed(1)}%</td>
              <td>{eficiencia.toFixed(1)}%</td>
              <td>{dependencia}</td>
            </tr>

            {(progressData.fases || []).map((fase) => (
              <TreeTableRow
                key={fase.id}
                fase={fase}
                isExpanded={isExpanded}
                toggleNode={toggleNode}
                onOpenEvidence={handleOpenEvidence}
                onPreviewEvidence={handlePreviewEvidencia}
              />
            ))}
          </tbody>
        </table>
      </div>

      {showEvidenceModal && (
        <EvidenceUpload
          proyectoId={proyectoId}
          entregableId={showEvidenceModal}
          onClose={handleCloseEvidence}
          onSuccess={handleEvidenceSuccess}
        />
      )}

      {previewEvidence.open && (
        <div className="evidence-preview-overlay" onClick={closePreviewEvidence}>
          <div className="evidence-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="evidence-preview-header">
              <div>
                <span className="evidence-preview-kicker">Evidencia PDF</span>
                <h3>{previewEvidence.name}</h3>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={closePreviewEvidence}
                aria-label="Cerrar vista previa"
              >
                <X size={18} />
              </button>
            </div>

            <div className="evidence-preview-body">
              {previewEvidence.loading && (
                <div className="evidence-preview-loading">
                  <Loader2 size={28} className="animate-spin" />
                  <p>Preparando vista previa...</p>
                </div>
              )}

              {!previewEvidence.loading && previewEvidence.error && (
                <div className="evidence-preview-error">
                  <p>{previewEvidence.error}</p>
                  <button
                    type="button"
                    className="evidence-preview-download"
                    onClick={() => handleDownloadEvidencia({ evidenciaUrl: previewEvidence.url, evidenciaPdf: previewEvidence.name })}
                  >
                    <Download size={14} />
                    Descargar PDF
                  </button>
                </div>
              )}

              {!previewEvidence.loading && !previewEvidence.error && previewEvidence.objectUrl && (
                <iframe
                  className="evidence-preview-frame"
                  src={previewEvidence.objectUrl}
                  title={previewEvidence.name}
                />
              )}
            </div>

            <div className="evidence-preview-footer">
              <button type="button" className="evidence-preview-secondary" onClick={closePreviewEvidence}>
                Cerrar
              </button>
              {previewEvidence.objectUrl ? (
                <a
                  className="evidence-preview-download"
                  href={previewEvidence.objectUrl}
                  download={previewEvidence.name}
                >
                  <Download size={14} />
                  Descargar PDF
                </a>
              ) : previewEvidence.url ? (
                <button
                  type="button"
                  className="evidence-preview-download"
                  onClick={() => handleDownloadEvidencia({ evidenciaUrl: previewEvidence.url, evidenciaPdf: previewEvidence.name })}
                >
                  <Download size={14} />
                  Descargar PDF
                </button>
              ) : null}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProgressTreeTable;
