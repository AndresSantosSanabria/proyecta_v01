import React from 'react';
import {
  Lock,
  FileText,
  Upload,
  ChevronDown,
  ChevronRight,
  Layers,
  Target,
  Package,
  Download,
} from 'lucide-react';
import EvidenceUpload from '../../common/EvidenceUpload';
import { usePermission } from '../../../hooks/usePermission';

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const getEvidenciaFile = (ent) => ent.evidenciaPdf || ent.archivo || ent.evidencia || null;
const getEvidenciaUrl = (ent) => ent.evidenciaUrl || ent.descargaUrl || null;
const getEntregableId = (ent) => ent.entregableId || ent.id;
const getFechaEntrega = (ent) => ent.fechaEntrega || ent.fechaEntregaReal || null;
const getDiasAtraso = (ent) => ent.diasAtraso ?? ent.atraso ?? null;

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

const TreeTableRow = ({ fase, isExpanded, toggleNode, onOpenEvidence, onDownloadEvidencia }) => {
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
              const eficaciaEnt = toNumber(ent.eficacia ?? (programadoEnt === 0 ? 1 : ejecutadoEnt / programadoEnt));
              const diasAtraso = getDiasAtraso(ent);
              const semaforo = semaforoDias(diasAtraso, tieneDocumento);

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
                            onDownloadEvidencia(ent);
                          }}
                        >
                          <FileText size={14} /> {evidenciaFile} <Download size={12} />
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
                  <td>{(eficaciaEnt * 100).toFixed(1)}%</td>
                  <td>{tieneDocumento ? '100.0%' : '0.0%'}</td>
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

  const handleOpenEvidence = (entregableId) => setShowEvidenceModal(entregableId);
  const handleCloseEvidence = () => setShowEvidenceModal(null);
  const handleEvidenceSuccess = () => {
    setShowEvidenceModal(null);
    if (onEvidenceUploaded) onEvidenceUploaded();
  };

  const handleDownloadEvidencia = async (ent) => {
    const url = getEvidenciaUrl(ent);
    const file = getEvidenciaFile(ent);
    if (!url) return;
    try {
      const response = await fetch(url);
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', file || 'evidencia.pdf');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch {
      window.open(url, '_blank');
    }
  };

  const avanceTotal = toNumber(progressData.progresoEjecutado ?? progressData.avanceTotal ?? 0);
  const corte = progressData.corte ? new Date(progressData.corte).toLocaleDateString('es-CO') : new Date().toLocaleDateString('es-CO');
  const dependencia = excelSummary?.dependencia || progressData.dependencia || '—';
  const entregablesProgramados = toNumber(excelSummary?.programadosAlCorte ?? progressData.entregablesProgramadosAlCorte ?? progressData.entregablesProgramados ?? 0);
  const entregablesConformes = toNumber(excelSummary?.entregadosAlCorte ?? progressData.entregablesConformes ?? progressData.entregablesConformidad ?? 0);
  const eficacia = toNumber(excelSummary?.eficacia ?? progressData.eficacia ?? (entregablesProgramados === 0 ? 1 : entregablesConformes / entregablesProgramados));
  const eficiencia = toNumber(excelSummary?.eficiencia ?? progressData.eficiencia ?? (entregablesConformes === 0 ? 1 : entregablesConformes / Math.max(1, progressData.entregablesTotal || 1)));

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
              <td>{(eficacia * 100).toFixed(1)}%</td>
              <td>{(eficiencia * 100).toFixed(1)}%</td>
              <td>{dependencia}</td>
            </tr>

            {(progressData.fases || []).map((fase) => (
              <TreeTableRow
                key={fase.id}
                fase={fase}
                isExpanded={isExpanded}
                toggleNode={toggleNode}
                onOpenEvidence={handleOpenEvidence}
                onDownloadEvidencia={handleDownloadEvidencia}
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
    </div>
  );
};

export default ProgressTreeTable;
