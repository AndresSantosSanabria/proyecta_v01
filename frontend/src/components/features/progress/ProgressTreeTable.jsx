import React from 'react';
import {
  Lock,
  FileText,
  Upload,
  Check,
  ChevronDown,
  ChevronRight,
  Layers,
  Target,
  Package,
  Download
} from 'lucide-react';
import EvidenceUpload from '../../common/EvidenceUpload';

const getEvidenciaFile = (ent) => ent.evidenciaPdf || ent.archivo || ent.evidencia || null;
const getEvidenciaUrl = (ent) => ent.evidenciaUrl || ent.descargaUrl || null;
const getEntregableId = (ent) => ent.entregableId || ent.id;
const getFechaEntrega = (ent) => ent.fechaEntrega || ent.fechaEntregaReal || null;

const TreeTableRow = ({
  fase,
  isExpanded,
  toggleNode,
  onOpenEvidence,
  onDownloadEvidencia
}) => {
  const avanceFase = typeof fase.avance === 'object' ? parseFloat(fase.avance) || 0 : fase.avance || 0;
  const ponderacionFase = typeof fase.ponderacion === 'object' ? parseFloat(fase.ponderacion) || 0 : fase.ponderacion || 0;

  return (
    <React.Fragment>
      <tr
        className="row-phase clickable-row"
        onClick={() => toggleNode(`fase-${fase.id}`)}
      >
        <td className="col-hito indent-1">
          <div className="node-title">
            {isExpanded(`fase-${fase.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            <span className="node-badge fase-badge"><Layers size={12} /> FASE</span>
            <strong>{fase.nombre}</strong>
          </div>
        </td>
        <td>{ponderacionFase}%</td>
        <td>—</td>
        <td>
          <div className="progress-cell">
            <div className="progress-bar-bg small">
              <div className={`progress-bar-fill ${avanceFase === 100 ? 'success-bg' : ''}`} style={{ width: `${avanceFase}%` }}></div>
            </div>
            <span>{avanceFase}% <Lock size={10} className="text-muted" /></span>
          </div>
        </td>
        <td>—</td>
        <td>—</td>
      </tr>

      {isExpanded(`fase-${fase.id}`) && fase.hitos.map((hito) => {
        const avanceHito = typeof hito.avance === 'object' ? parseFloat(hito.avance) || 0 : hito.avance || 0;
        const ponderacionHito = typeof hito.ponderacion === 'object' ? parseFloat(hito.ponderacion) || 0 : hito.ponderacion || 0;

        return (
          <React.Fragment key={hito.id}>
            <tr
              className="row-milestone clickable-row"
              onClick={() => toggleNode(`hito-${hito.id}`)}
            >
              <td className="col-hito indent-2">
                <div className="node-title">
                  {isExpanded(`hito-${hito.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                  <span className="node-badge hito-badge"><Target size={12} /> HITO</span>
                  <span>{hito.nombre}</span>
                </div>
              </td>
              <td>{ponderacionHito}%</td>
              <td>—</td>
              <td>
                <div className="progress-cell">
                  <div className="progress-bar-bg small">
                    <div className={`progress-bar-fill ${avanceHito === 100 ? 'success-bg' : ''}`} style={{ width: `${avanceHito}%` }}></div>
                  </div>
                  <span>{avanceHito}% <Lock size={10} className="text-muted" /></span>
                </div>
              </td>
              <td>—</td>
              <td>—</td>
            </tr>

            {isExpanded(`hito-${hito.id}`) && hito.entregables.map((ent) => {
              const evidenciaFile = getEvidenciaFile(ent);
              const evidenciaUrl = getEvidenciaUrl(ent);
              const fechaEntrega = getFechaEntrega(ent);
              const entregableId = getEntregableId(ent);
              const tieneDocumento = evidenciaUrl != null;
              const avanceEnt = typeof ent.avance === 'object' ? parseFloat(ent.avance) || 0 : ent.avance || 0;
              const ponderacionEnt = typeof ent.ponderacion === 'object' ? parseFloat(ent.ponderacion) || 0 : ent.ponderacion || 0;

              return (
                <tr key={entregableId} className="row-deliverable">
                  <td className="col-hito indent-3">
                    <div className="deliverable-content">
                      <div className="node-title">
                        <span className="node-badge entregable-badge"><Package size={12} /> ENTREGABLE</span>
                        <span>{ent.nombre}</span>
                      </div>
                      {tieneDocumento && (
                        <button className="file-link-btn" onClick={(e) => { e.stopPropagation(); onDownloadEvidencia(ent); }}>
                          <FileText size={14} /> {evidenciaFile} <Download size={12} />
                        </button>
                      )}
                    </div>
                  </td>
                  <td>{ponderacionEnt}%</td>
                  <td>{ent.fechaLimite || '—'}</td>
                  <td>
                    {tieneDocumento ? (
                      <span className="status-badge success"><Check size={12} /> A conformidad</span>
                    ) : (
                      <div className="progress-cell">
                        <span>{avanceEnt}% <Lock size={10} className="text-muted" /></span>
                      </div>
                    )}
                  </td>
                  <td>
                    {tieneDocumento ? (
                      <span className="text-success small">✓ Entregado {fechaEntrega || ''}</span>
                    ) : (
                      <span>—</span>
                    )}
                  </td>
                  <td className={ent.atraso ? 'text-danger fw-bold' : ''}>
                    {ent.atraso ? `+${ent.atraso}d` : '—'}
                  </td>
                  {!tieneDocumento && (
                    <td className="action-col-absolute">
                      <button className="btn-upload" onClick={(e) => { e.stopPropagation(); onOpenEvidence(entregableId); }}>
                        <Upload size={14} /> Marcar OK + subir PDF
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </React.Fragment>
        );
      })}
    </React.Fragment>
  );
};

const ProgressTreeTable = ({ progressData, isExpanded, toggleNode, proyectoId, onEvidenceUploaded }) => {
  const [showEvidenceModal, setShowEvidenceModal] = React.useState(null);

  const handleOpenEvidence = (entregableId) => {
    setShowEvidenceModal(entregableId);
  };

  const handleCloseEvidence = () => {
    setShowEvidenceModal(null);
  };

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

  const avanceTotal = typeof progressData.avanceTotal === 'object' ? parseFloat(progressData.avanceTotal) || 0 : progressData.avanceTotal || 0;

  return (
    <div className="detailed-table-container">
      <div className="table-header">
        <h2>Avance Detallado — {progressData.nombre}</h2>
        <div className="header-actions">
          <span className="corte-date">Corte: {new Date().toLocaleDateString('es-CO')}</span>
          <button className="btn-report"><FileText size={16} /> Generar Reporte</button>
        </div>
      </div>

      <div className="tree-table-wrapper">
        <table className="tree-table">
          <thead>
            <tr>
              <th className="col-hito">HITO / ENTREGABLE</th>
              <th className="col-ponderacion">PONDERACIÓN <Lock size={12} /></th>
              <th className="col-fecha">FECHA LÍMITE</th>
              <th className="col-avance">AVANCE <Lock size={12} /></th>
              <th className="col-estado">ESTADO <Lock size={12} /></th>
              <th className="col-atraso">ATRASO <Lock size={12} /></th>
            </tr>
          </thead>
          <tbody>
            <tr className="row-root">
              <td className="col-hito"><strong>▼ {progressData.codigo} — {progressData.nombre}</strong></td>
              <td><strong>100%</strong></td>
              <td>—</td>
              <td>
                <div className="progress-cell">
                  <div className="progress-bar-bg small">
                    <div className="progress-bar-fill" style={{ width: `${avanceTotal}%` }}></div>
                  </div>
                  <span>{avanceTotal}%</span>
                </div>
              </td>
              <td>—</td>
              <td>—</td>
            </tr>

            {progressData.fases.map((fase) => (
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
