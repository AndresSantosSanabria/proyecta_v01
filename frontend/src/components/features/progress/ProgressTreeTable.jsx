import React from 'react';
import {
  Lock,
  FileText,
  Upload,
  CheckCircle2,
  RefreshCcw,
  Eye,
  ChevronDown,
  ChevronRight,
  Layers,
  Target,
  Package,
  Download,
  X,
  Loader2,
  MessageSquare,
  Pencil,
  History,
  RotateCcw,
  Calendar,
  Clock,
} from 'lucide-react';
import EvidenceUpload from '../../common/EvidenceUpload';
import Paso3FasesHitosEntregables from '../wizard/steps/Paso3FasesHitosEntregables';
import apiClient from '../../../api/axiosConfig';
import projectService from '../../../services/projectService';
import { usePermission } from '../../../hooks/usePermission';

import ModificarFechaModal from './ModificarFechaModal';
import HistorialCambiosFecha from './HistorialCambiosFecha';

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
const getEstadoRevision = (ent) => String(ent.estadoCodigo || ent.estado || '').toUpperCase();
const getObservacionRevision = (ent) => ent.observacionRevision || ent.observacion_revision || ent.observacion || '';

const unwrapApiData = (value) => value?.data?.data ?? value?.data ?? value ?? null;
const hasPersistentId = (item) => Boolean(item?.id || item?.faseId || item?.hitoId || item?.entregableId);
const getFaseId = (fase) => fase?.faseId || fase?.id;
const getHitoId = (hito) => hito?.hitoId || hito?.id;
const trimOrNull = (value) => {
  const text = String(value || '').trim();
  return text || null;
};

const dateSortValue = (value) => {
  if (!value) return Number.POSITIVE_INFINITY;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? Number.POSITIVE_INFINITY : parsed;
};

const toDateOnly = (value) => {
  if (!value) return null;
  const parsed = new Date(`${value}T00:00:00`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const isBeforeDate = (value, minValue) => {
  const date = toDateOnly(value);
  const minDate = toDateOnly(minValue);
  return Boolean(date && minDate && date < minDate);
};

const compareEntregablesBySchedule = (left, right) => {
  const startDiff = dateSortValue(left?.fechaInicio || left?.fecha_inicio)
    - dateSortValue(right?.fechaInicio || right?.fecha_inicio);
  if (startDiff !== 0) return startDiff;

  const endDiff = dateSortValue(left?.fechaLimite || left?.fechaEntrega)
    - dateSortValue(right?.fechaLimite || right?.fechaEntrega);
  if (endDiff !== 0) return endDiff;

  const leftId = getEntregableId(left);
  const rightId = getEntregableId(right);
  if (leftId && rightId && leftId !== rightId) {
    const leftNumericId = Number(leftId);
    const rightNumericId = Number(rightId);
    if (Number.isFinite(leftNumericId) && Number.isFinite(rightNumericId)) {
      return leftNumericId - rightNumericId;
    }
  }

  return String(left?.nombre || '').localeCompare(String(right?.nombre || ''), 'es', { sensitivity: 'base' });
};

const sortEntregablesBySchedule = (entregables = []) => [...entregables].sort(compareEntregablesBySchedule);

const compareHitosBySequence = (left, right) => {
  const leftId = Number(getHitoId(left));
  const rightId = Number(getHitoId(right));
  const leftHasId = Number.isFinite(leftId);
  const rightHasId = Number.isFinite(rightId);

  if (leftHasId && rightHasId && leftId !== rightId) {
    return leftId - rightId;
  }
  if (leftHasId && !rightHasId) return -1;
  if (!leftHasId && rightHasId) return 1;
  return 0;
};

const sortHitosBySequence = (hitos = []) => [...hitos].sort(compareHitosBySequence);

const normalizeHierarchyForEditor = (source = []) => source.map((fase) => ({
  id: getFaseId(fase),
  nombre: fase.nombre || '',
  descripcion: fase.descripcion || '',
  ponderacion: fase.ponderacion ?? '',
  hitos: sortHitosBySequence(fase.hitos || []).map((hito) => ({
    id: getHitoId(hito),
    nombre: hito.nombre || '',
    descripcion: hito.descripcion || '',
    ponderacion: hito.ponderacion ?? '',
    entregables: sortEntregablesBySchedule(hito.entregables || []).map((entregable) => ({
      id: getEntregableId(entregable),
      nombre: entregable.nombre || '',
      ponderacion: entregable.ponderacion ?? '',
      fechaInicio: entregable.fechaInicio || entregable.fecha_inicio || '',
      fechaLimite: entregable.fechaLimite || entregable.fechaEntrega || '',
    })),
  })),
}));

const toHierarchyNumber = (value) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const buildHierarchyValidation = (fases = [], fechaInicioProyecto = '') => {
  const errors = {};

  if (fases.length === 0) {
    return { errors, message: '' };
  }

  const sumaFases = fases.reduce((sum, fase) => sum + toHierarchyNumber(fase.ponderacion), 0);
  if (Math.abs(sumaFases - 100) > 0.01) {
    errors.fases = 'La suma total de fases debe ser exactamente 100%.';
  }

  fases.forEach((fase, fIndex) => {
    if (toHierarchyNumber(fase.ponderacion) <= 0) {
      errors[`fase_${fIndex}_ponderacion`] = 'La ponderacion de la fase debe ser mayor a 0.';
    }
    if (!(fase.hitos || []).length) {
      errors[`fase_${fIndex}_hitos`] = `La fase ${fIndex + 1} debe tener al menos un hito.`;
    }

    const sumaHitos = (fase.hitos || []).reduce((sum, hito) => sum + toHierarchyNumber(hito.ponderacion), 0);
    if ((fase.hitos || []).length && Math.abs(sumaHitos - 100) > 0.01) {
      errors[`fase_${fIndex}_ponderacion_suma`] = `Los hitos de la fase ${fIndex + 1} deben sumar 100%.`;
    }

    (fase.hitos || []).forEach((hito, hIndex) => {
      if (toHierarchyNumber(hito.ponderacion) <= 0) {
        errors[`hito_${fIndex}_${hIndex}_ponderacion`] = 'La ponderacion del hito debe ser mayor a 0.';
      }
      if (!(hito.entregables || []).length) {
        errors[`hito_${fIndex}_${hIndex}_entregables`] = `El hito ${fIndex + 1}.${hIndex + 1} debe tener al menos un entregable.`;
      }

      const sumaEntregables = (hito.entregables || []).reduce((sum, entregable) => sum + toHierarchyNumber(entregable.ponderacion), 0);
      if ((hito.entregables || []).length && Math.abs(sumaEntregables - 100) > 0.01) {
        errors[`hito_${fIndex}_${hIndex}_ponderacion_suma`] = `Los entregables del hito ${fIndex + 1}.${hIndex + 1} deben sumar 100%.`;
      }

      (hito.entregables || []).forEach((entregable, eIndex) => {
        if (toHierarchyNumber(entregable.ponderacion) <= 0) {
          errors[`ent_${fIndex}_${hIndex}_${eIndex}_ponderacion`] = 'La ponderacion del entregable debe ser mayor a 0.';
        }
        if (!hasPersistentId(entregable)) {
          if (!entregable.fechaInicio) {
            errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaInicio`] = 'La fecha de inicio del nuevo entregable es obligatoria.';
          }
          if (!entregable.fechaLimite) {
            errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaLimite`] = 'La fecha limite del nuevo entregable es obligatoria.';
          }
          if (entregable.fechaInicio && fechaInicioProyecto && isBeforeDate(entregable.fechaInicio, fechaInicioProyecto)) {
            errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaInicio`] = `La fecha de inicio del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (${fechaInicioProyecto}).`;
          }
          if (entregable.fechaLimite && fechaInicioProyecto && isBeforeDate(entregable.fechaLimite, fechaInicioProyecto)) {
            errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaLimite`] = `La fecha limite del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (${fechaInicioProyecto}).`;
          }
          if (entregable.fechaInicio && entregable.fechaLimite && new Date(entregable.fechaLimite) < new Date(entregable.fechaInicio)) {
            errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaLimite`] = 'La fecha limite debe ser mayor o igual a la fecha de inicio.';
          }
        }
      });
    });
  });

  const firstKey = Object.keys(errors)[0];
  return { errors, message: firstKey ? errors[firstKey] : '' };
};

const statusClassByState = (estadoRevision, fallback) => {
  if (estadoRevision === 'RECHAZADO') return 'danger';
  if (estadoRevision === 'EN_PROCESO' || estadoRevision === 'EN_REVISION' || estadoRevision === 'COMPLETADO') return 'warning';
  if (estadoRevision === 'A_CONFORMIDAD') return 'success';
  return fallback;
};

const statusLabelByState = (estadoRevision, fallback) => {
  if (estadoRevision === 'RECHAZADO') return 'OBSERVADO';
  if (estadoRevision === 'EN_PROCESO' || estadoRevision === 'EN_REVISION' || estadoRevision === 'COMPLETADO') return 'EN REVISION';
  if (estadoRevision === 'A_CONFORMIDAD') return 'A CONFORMIDAD';
  return fallback || 'PENDIENTE';
};

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
    throw new Error('No se encontro la URL de la evidencia.');
  }

  const response = await apiClient.get(path, {
    responseType: 'blob',
    headers: {
      Accept: 'application/pdf',
    },
  });
  const blob = response.data;

  if (!(await isPdfBlob(blob))) {
    throw new Error('El archivo recibido no es un PDF valido.');
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



const TreeTableRow = ({
  fase,
  isExpanded,
  toggleNode,
  onOpenEvidence,
  onPreviewEvidence,
  onApproveEvidence,
  onRejectEvidence,
  onOpenHistory,
  approvingEntregableId,
  onCambiarFecha,
  onVerHistorialFechas,
}) => {
  const canUploadEvidence = usePermission('EVIDENCIA:CARGAR');
  const canReviewEvidence = usePermission('ENTREGABLE:APROBAR');
  const canViewDocumentHistory = usePermission('DOCUMENTO:HISTORIAL');
  const canModificarFecha = usePermission('ENTREGABLE:CAMBIAR_FECHA');
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
        <td>--</td>
        <td>--</td>
        <td>--</td>
        <td>{(eficaciaFase * 100).toFixed(1)}%</td>
        <td>--</td>
        <td>--</td>
        <td>--</td>
      </tr>

      {isExpanded(`fase-${fase.id}`) && sortHitosBySequence(fase.hitos || []).map((hito) => {
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
              <td>--</td>
              <td>--</td>
              <td>--</td>
              <td>{(eficaciaHito * 100).toFixed(1)}%</td>
              <td>--</td>
              <td>--</td>
              <td>--</td>
            </tr>

            {isExpanded(`hito-${hito.id}`) && (hito.entregables || []).map((ent) => {
              const evidenciaFile = getEvidenciaFile(ent);
              const evidenciaUrl = getEvidenciaUrl(ent);
              const fechaEntrega = getFechaEntrega(ent);
              const entregableId = getEntregableId(ent);
              const tieneDocumento = Boolean(evidenciaUrl);
              const estadoRevision = getEstadoRevision(ent);
              const observacionRevision = getObservacionRevision(ent);
              const estaAprobado = estadoRevision === 'A_CONFORMIDAD' || ent.conforme === true;
              const estaRechazado = estadoRevision === 'RECHAZADO';
              const enRevision = !estaRechazado && (estadoRevision === 'EN_PROCESO' || estadoRevision === 'EN_REVISION' || estadoRevision === 'COMPLETADO' || (tieneDocumento && !estaAprobado));
              const programadoEnt = toNumber(ent.progresoProgramado ?? ent.avanceProgramado ?? ent.avance ?? 0);
              const ejecutadoEnt = toNumber(ent.progresoEjecutado ?? ent.avance ?? 0);
              const diferenciaEnt = toNumber(ent.diferencia ?? (programadoEnt - ejecutadoEnt));
              const eficaciaEnt = toDisplayPercent(ent.eficacia ?? (programadoEnt === 0 ? 1 : ejecutadoEnt / programadoEnt));
              const diasAtraso = getDiasAtraso(ent);
              const semaforo = semaforoDias(diasAtraso, tieneDocumento);
              const statusClass = statusClassByState(estadoRevision, semaforo);
              const statusLabel = statusLabelByState(estadoRevision, ent.estado || (tieneDocumento ? 'EN_PROCESO' : 'PENDIENTE'));
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
                          type="button"
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
                  <td>
                    <div className="date-stack">
                      {ent.fechaInicio && <span>Inicio: {ent.fechaInicio}</span>}
                      <span>Vence: {ent.fechaLimite || '--'}</span>
                    </div>
                  </td>
                  <td><NodeMetric value={programadoEnt} /></td>
                  <td><NodeMetric value={ejecutadoEnt} /></td>
                  <td><NodeMetric value={diferenciaEnt} /></td>
                  <td>
                    <span className={`status-badge ${statusClass}`}>
                      {statusLabel}
                    </span>
                    {estaRechazado && observacionRevision && (
                      <div className="review-observation">
                        <strong>Observacion del gestor</strong>
                        <span>{observacionRevision}</span>
                      </div>
                    )}
                  </td>
                  <td>{fechaEntrega || '--'}</td>
                  <td>{diasAtraso == null ? '--' : `${diasAtraso >= 0 ? '+' : ''}${diasAtraso}d`}</td>
                  <td>{eficaciaEnt.toFixed(1)}%</td>
                  <td>{eficienciaEnt.toFixed(1)}%</td>
                  <td className="action-col">
                    <div className="action-icon-group" onClick={(e) => e.stopPropagation()}>
                      {!tieneDocumento && canUploadEvidence && (
                        <button
                          type="button"
                          className="btn-action-icon"
                          title="Subir evidencia"
                          aria-label="Subir evidencia"
                          onClick={() => onOpenEvidence(entregableId)}
                        >
                          <Upload size={15} />
                        </button>
                      )}

                      {estaRechazado && canUploadEvidence && (
                        <button
                          type="button"
                          className="btn-action-text warning"
                          title="Cargar evidencia subsanada"
                          aria-label="Cargar evidencia subsanada"
                          onClick={() => onOpenEvidence(entregableId, 'subsanar')}
                        >
                          <CheckCircle2 size={14} />
                          Subsanar
                        </button>
                      )}

                      {tieneDocumento && !estaAprobado && !estaRechazado && canUploadEvidence && (
                        <button
                          type="button"
                          className="btn-action-icon"
                          title="Reemplazar evidencia"
                          aria-label="Reemplazar evidencia"
                          onClick={() => onOpenEvidence(entregableId)}
                        >
                          <RefreshCcw size={15} />
                        </button>
                      )}

                      {tieneDocumento && canViewDocumentHistory && (
                        <button
                          type="button"
                          className="btn-action-icon info"
                          title="Ver historico documental"
                          aria-label="Ver historico documental"
                          onClick={() => onOpenHistory(ent)}
                        >
                          <History size={15} />
                        </button>
                      )}

                      {enRevision && canReviewEvidence && (
                        <>
                          <button
                            type="button"
                            className="btn-action-icon success"
                            title="Aprobar entregable"
                            aria-label="Aprobar entregable"
                            disabled={approvingEntregableId === entregableId}
                            onClick={() => onApproveEvidence?.(entregableId)}
                          >
                            <CheckCircle2 size={15} />
                          </button>
                          <button
                            type="button"
                            className="btn-action-icon danger"
                            title="Observar evidencia y solicitar subsanacion"
                            aria-label="Observar evidencia y solicitar subsanacion"
                            disabled={approvingEntregableId === entregableId}
                            onClick={() => onRejectEvidence?.(entregableId)}
                          >
                            <MessageSquare size={15} />
                          </button>
                        </>
                      )}

                      {!tieneDocumento && !canUploadEvidence && <span className="action-placeholder">--</span>}

                      {canModificarFecha && (
                        <button
                          type="button"
                          className="btn-action-icon"
                          title="Modificar fecha limite"
                          aria-label="Modificar fecha limite"
                          onClick={() => onCambiarFecha(ent)}
                        >
                          <Calendar size={15} />
                        </button>
                      )}

                      {canModificarFecha && ent.tieneHistorialCambiosFecha && (
                        <button
                          type="button"
                          className="btn-action-icon info"
                          title="Ver historial de cambios de fecha"
                          aria-label="Ver historial de cambios de fecha"
                          onClick={() => onVerHistorialFechas(ent)}
                        >
                          <Clock size={15} />
                        </button>
                      )}
                    </div>
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

const ProgressTreeTable = ({ progressData, projectInfo, excelSummary, isExpanded, toggleNode, proyectoId, onEvidenceUploaded }) => {
  const [showEvidenceModal, setShowEvidenceModal] = React.useState(null);
  const canEditProject = usePermission('PROYECTO:EDITAR');
  const canManageHierarchy = canEditProject || usePermission('PROYECTO:EDITAR_ESTRUCTURA');
  const canGenerateReport = usePermission('REPORTE:VER');
  const [reviewModal, setReviewModal] = React.useState({
    open: false,
    entregableId: null,
    observacion: '',
    error: '',
  });
  const [hierarchyModal, setHierarchyModal] = React.useState({
    open: false,
    fases: [],
    errors: {},
    error: '',
    loading: false,
    saving: false,
    fechaInicioProyecto: '',
  });
  const [approvalModal, setApprovalModal] = React.useState({
    open: false,
    entregableId: null,
    error: '',
  });
  const [historyModal, setHistoryModal] = React.useState({
    open: false,
    loading: false,
    error: '',
    entregable: null,
    versions: [],
  });
  const [revertModal, setRevertModal] = React.useState({
    open: false,
    version: null,
    motivo: '',
    error: '',
    saving: false,
  });
  const [previewEvidence, setPreviewEvidence] = React.useState({
    open: false,
    loading: false,
    error: '',
    name: '',
    url: '',
    objectUrl: '',
  });
  const [approvingEntregableId, setApprovingEntregableId] = React.useState(null);
  const [showCambiarFechaModal, setShowCambiarFechaModal] = React.useState(null);
  const [showHistorialFechasModal, setShowHistorialFechasModal] = React.useState(null);

  React.useEffect(() => {
    return () => {
      if (previewEvidence.objectUrl) {
        window.URL.revokeObjectURL(previewEvidence.objectUrl);
      }
    };
  }, [previewEvidence.objectUrl]);

  const handleOpenEvidence = (entregableId, mode = 'cargar') => setShowEvidenceModal({ entregableId, mode });
  const handleCloseEvidence = () => setShowEvidenceModal(null);
  const handleEvidenceSuccess = () => {
    setShowEvidenceModal(null);
    if (onEvidenceUploaded) onEvidenceUploaded();
  };

  const handleOpenHierarchyModal = async () => {
    const initialFases = normalizeHierarchyForEditor(projectInfo?.fases || progressData.fases || []);
    setHierarchyModal({
      open: true,
      fases: initialFases,
      errors: {},
      error: '',
      loading: !projectInfo,
      saving: false,
      fechaInicioProyecto: projectInfo?.fechaInicio || progressData?.fechaInicio || '',
    });

    if (projectInfo) return;

    try {
      const response = await projectService.getById(proyectoId);
      const project = unwrapApiData(response);
      setHierarchyModal((current) => ({
        ...current,
        fases: normalizeHierarchyForEditor(project?.fases || progressData.fases || []),
        fechaInicioProyecto: project?.fechaInicio || current.fechaInicioProyecto || '',
        loading: false,
      }));
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible cargar la jerarquia actual.';
      setHierarchyModal((current) => ({
        ...current,
        loading: false,
        error: msg,
      }));
      console.error('Hierarchy load error:', error.response?.data || error);
    }
  };

  const handleCloseHierarchyModal = () => {
    if (hierarchyModal.saving) return;
    setHierarchyModal({
      open: false,
      fases: [],
      errors: {},
      error: '',
      loading: false,
      saving: false,
      fechaInicioProyecto: '',
    });
  };

  const handleHierarchyTreeChange = (changes) => {
    setHierarchyModal((current) => ({
      ...current,
      fases: changes.fases || [],
      errors: {},
      error: '',
    }));
  };

  const buildFasePayload = (fase, includeChildren = false) => {
    const payload = {
      nombre: String(fase.nombre || '').trim(),
      descripcion: trimOrNull(fase.descripcion),
      ponderacion: Math.round(toHierarchyNumber(fase.ponderacion)),
    };

    if (includeChildren) {
      payload.hitos = sortHitosBySequence(fase.hitos || []).map((hito) => buildHitoPayload(hito, true));
    }

    return payload;
  };

  const buildHitoPayload = (hito, includeChildren = false) => {
    const payload = {
      nombre: String(hito.nombre || '').trim(),
      descripcion: trimOrNull(hito.descripcion),
      ponderacion: Math.round(toHierarchyNumber(hito.ponderacion)),
    };

    if (includeChildren) {
      payload.entregables = sortEntregablesBySchedule(hito.entregables || [])
        .map((entregable) => buildEntregablePayload(entregable, true));
    }

    return payload;
  };

  const buildEntregablePayload = (entregable, includeDate = false) => {
    const payload = {
      nombre: String(entregable.nombre || '').trim(),
      ponderacion: Math.round(toHierarchyNumber(entregable.ponderacion)),
    };

    if (includeDate) {
      payload.fechaInicio = entregable.fechaInicio;
      payload.fechaLimite = entregable.fechaLimite;
    }

    return payload;
  };

  const persistHierarchyTree = async (fases) => {
    for (const fase of fases) {
      const faseId = getFaseId(fase);

      if (!faseId) {
        await projectService.createFase(proyectoId, buildFasePayload(fase, true));
        continue;
      }

      await projectService.updateFase(proyectoId, faseId, buildFasePayload(fase));

      for (const hito of sortHitosBySequence(fase.hitos || [])) {
        const hitoId = getHitoId(hito);

        if (!hitoId) {
          await projectService.createHito(proyectoId, faseId, buildHitoPayload(hito, true));
          continue;
        }

        await projectService.updateHito(proyectoId, faseId, hitoId, buildHitoPayload(hito));

        for (const entregable of sortEntregablesBySchedule(hito.entregables || [])) {
          const entregableId = getEntregableId(entregable);

          if (!entregableId) {
            await projectService.createEntregable(proyectoId, faseId, hitoId, buildEntregablePayload(entregable, true));
          } else {
            await projectService.updateEntregable(proyectoId, entregableId, buildEntregablePayload(entregable));
          }
        }
      }
    }
  };

  const handleSaveHierarchy = async (event) => {
    event.preventDefault();

    const validation = buildHierarchyValidation(hierarchyModal.fases, hierarchyModal.fechaInicioProyecto);
    if (validation.message) {
      setHierarchyModal((current) => ({
        ...current,
        errors: validation.errors,
        error: validation.message,
      }));
      return;
    }

    try {
      setHierarchyModal((current) => ({ ...current, saving: true, error: '' }));
      await persistHierarchyTree(hierarchyModal.fases);
      handleCloseHierarchyModal();
      if (onEvidenceUploaded) onEvidenceUploaded();
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible guardar la jerarquia.';
      setHierarchyModal((current) => ({
        ...current,
        saving: false,
        error: msg,
      }));
      console.error('Hierarchy save error:', error.response?.data || error);
    }
  };

  const handleOpenHistory = async (entregable) => {
    const entregableId = getEntregableId(entregable);
    setHistoryModal({
      open: true,
      loading: true,
      error: '',
      entregable,
      versions: [],
    });

    try {
      const response = await projectService.getEntregableVersions(proyectoId, entregableId);
      const payload = response?.data ?? response;
      setHistoryModal((current) => ({
        ...current,
        loading: false,
        versions: Array.isArray(payload) ? payload : [],
      }));
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible cargar el historico documental.';
      setHistoryModal((current) => ({
        ...current,
        loading: false,
        error: msg,
      }));
      console.error('History load error:', error.response?.data || error);
    }
  };

  const handleCloseHistory = () => {
    if (revertModal.saving) return;
    setHistoryModal({
      open: false,
      loading: false,
      error: '',
      entregable: null,
      versions: [],
    });
  };

  const handlePreviewVersion = async (version) => {
    if (!version?.descargaUrl) return;

    setPreviewEvidence((current) => {
      if (current.objectUrl) {
        window.URL.revokeObjectURL(current.objectUrl);
      }
      return {
        open: true,
        loading: true,
        error: '',
        name: version.nombreArchivo || `version-${version.numeroVersion}.pdf`,
        url: version.descargaUrl,
        objectUrl: '',
      };
    });

    try {
      const blob = await getEvidenceBlob(version.descargaUrl);
      const objectUrl = window.URL.createObjectURL(blob);
      setPreviewEvidence((current) => ({
        ...current,
        loading: false,
        objectUrl,
      }));
    } catch (error) {
      console.error('Version preview error:', error);
      setPreviewError('No fue posible previsualizar esta version. El archivo no es un PDF valido o no esta disponible.');
    }
  };

  const handleOpenRevertModal = (version) => {
    setRevertModal({
      open: true,
      version,
      motivo: '',
      error: '',
      saving: false,
    });
  };

  const handleCloseRevertModal = () => {
    if (revertModal.saving) return;
    setRevertModal({
      open: false,
      version: null,
      motivo: '',
      error: '',
      saving: false,
    });
  };

  const handleConfirmRevert = async (event) => {
    event.preventDefault();
    const motivo = revertModal.motivo.trim();

    if (!motivo) {
      setRevertModal((current) => ({ ...current, error: 'Debes ingresar el motivo de la reversion.' }));
      return;
    }

    try {
      setRevertModal((current) => ({ ...current, saving: true, error: '' }));
      const entregableId = getEntregableId(historyModal.entregable);
      await projectService.revertEntregableVersion(proyectoId, entregableId, revertModal.version.id, motivo);
      const response = await projectService.getEntregableVersions(proyectoId, entregableId);
      const payload = response?.data ?? response;
      setHistoryModal((current) => ({
        ...current,
        versions: Array.isArray(payload) ? payload : current.versions,
      }));
      handleCloseRevertModal();
      if (onEvidenceUploaded) onEvidenceUploaded();
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible revertir la version documental.';
      setRevertModal((current) => ({
        ...current,
        saving: false,
        error: msg,
      }));
      console.error('Revert error:', error.response?.data || error);
    }
  };

  const handleOpenReviewModal = (entregableId) => {
    setReviewModal({
      open: true,
      entregableId,
      observacion: '',
      error: '',
    });
  };

  const handleCloseReviewModal = () => {
    if (approvingEntregableId) return;
    setReviewModal({
      open: false,
      entregableId: null,
      observacion: '',
      error: '',
    });
  };

  const handleOpenApprovalModal = (entregableId) => {
    setApprovalModal({
      open: true,
      entregableId,
      error: '',
    });
  };

  const handleCloseApprovalModal = () => {
    if (approvingEntregableId) return;
    setApprovalModal({
      open: false,
      entregableId: null,
      error: '',
    });
  };

  const handleConfirmApproveEvidence = async (event) => {
    event.preventDefault();

    if (!approvalModal.entregableId) {
      setApprovalModal((current) => ({
        ...current,
        error: 'No se encontro el entregable a aprobar.',
      }));
      return;
    }

    try {
      setApprovingEntregableId(approvalModal.entregableId);
      await projectService.approveEntregable(proyectoId, approvalModal.entregableId);
      setApprovalModal({
        open: false,
        entregableId: null,
        error: '',
      });
      if (onEvidenceUploaded) onEvidenceUploaded();
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible aprobar el entregable.';
      setApprovalModal((current) => ({
        ...current,
        error: msg,
      }));
      console.error('Approval error:', error.response?.data || error);
    } finally {
      setApprovingEntregableId(null);
    }
  };

  const handleRejectEvidence = async (event) => {
    event.preventDefault();

    const observacion = reviewModal.observacion.trim();
    if (!reviewModal.entregableId) {
      setReviewModal((current) => ({
        ...current,
        error: 'No se encontro el entregable a observar.',
      }));
      return;
    }

    if (!observacion) {
      setReviewModal((current) => ({
        ...current,
        error: 'Debes registrar una observacion para que el lider pueda subsanar.',
      }));
      return;
    }

    try {
      setApprovingEntregableId(reviewModal.entregableId);
      await projectService.rejectEntregable(proyectoId, reviewModal.entregableId, observacion);
      setReviewModal({
        open: false,
        entregableId: null,
        observacion: '',
        error: '',
      });
      if (onEvidenceUploaded) onEvidenceUploaded();
    } catch (error) {
      const data = error.response?.data;
      const msg = data?.detail || data?.message || data?.title || error.message || 'No fue posible rechazar el entregable.';
      setReviewModal((current) => ({
        ...current,
        error: msg,
      }));
      console.error('Reject error:', error.response?.data || error);
    } finally {
      setApprovingEntregableId(null);
    }
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
      setPreviewError('No fue posible descargar la evidencia. El archivo no es un PDF valido o no esta disponible.');
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
      setPreviewError('No fue posible previsualizar la evidencia. El archivo no es un PDF valido o no esta disponible.');
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
  const corte = progressData.corte
    ? new Date(progressData.corte).toLocaleDateString('es-CO')
    : new Date().toLocaleDateString('es-CO');
  const dependencia = excelSummary?.dependencia || progressData.dependencia || '--';
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
            <span className="corte-date">Fecha límite: {corte}</span>
          </div>
        </div>
        <div className="header-actions">
          {canManageHierarchy && (
            <button
              className="btn-hierarchy-add"
              type="button"
              onClick={handleOpenHierarchyModal}
            >
              <Pencil size={16} /> Editar estructura
            </button>
          )}
          {canGenerateReport && (
            <button className="btn-report" type="button">
              <FileText size={16} /> Generar reporte
            </button>
          )}
        </div>
      </div>

      <div className="tree-table-wrapper">
        <table className="tree-table excel-table">
          <thead>
            <tr>
              <th className="col-node">Meta / Proyecto</th>
              <th className="col-ponderacion">Ponderacion <Lock size={12} /></th>
              <th className="col-fecha">Programado <Lock size={12} /></th>
              <th className="col-avance">Avance <Lock size={12} /></th>
              <th className="col-avance">Diferencia <Lock size={12} /></th>
              <th className="col-estado">Estado <Lock size={12} /></th>
              <th className="col-avance">Total entregables <Lock size={12} /></th>
              <th className="col-fecha">Programados a fecha límite <Lock size={12} /></th>
              <th className="col-fecha">Entregados a fecha límite <Lock size={12} /></th>
              <th className="col-avance">Eficacia <Lock size={12} /></th>
              <th className="col-avance">Eficiencia <Lock size={12} /></th>
              <th className="col-fecha">Dependencia <Lock size={12} /></th>
            </tr>
          </thead>
          <tbody>
            <tr className="row-root">
              <td className="col-node">
                <strong>{progressData.codigo} -- {progressData.nombre}</strong>
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
                onApproveEvidence={handleOpenApprovalModal}
                onRejectEvidence={handleOpenReviewModal}
                onOpenHistory={handleOpenHistory}
                approvingEntregableId={approvingEntregableId}
                onCambiarFecha={(ent) => setShowCambiarFechaModal(ent)}
                onVerHistorialFechas={(ent) => setShowHistorialFechasModal(ent)}
              />
            ))}
          </tbody>
        </table>
      </div>

      {showEvidenceModal && (
        <EvidenceUpload
          proyectoId={proyectoId}
          entregableId={showEvidenceModal.entregableId}
          mode={showEvidenceModal.mode}
          onClose={handleCloseEvidence}
          onSuccess={handleEvidenceSuccess}
        />
      )}

      {hierarchyModal.open && (
        <div className="review-modal-overlay" role="presentation" onClick={handleCloseHierarchyModal}>
          <form className="review-modal hierarchy-modal hierarchy-structure-modal" onSubmit={handleSaveHierarchy} onClick={(event) => event.stopPropagation()}>
            <div className="review-modal-header">
              <div>
                <span className="review-modal-kicker hierarchy">Jerarquia del proyecto</span>
                <h3>Editar fases, hitos y entregables</h3>
                <p>Agrega nuevos elementos o edita textos y pesos. Las fechas de entregables existentes quedan bloqueadas.</p>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={handleCloseHierarchyModal}
                aria-label="Cerrar editor de jerarquia"
                disabled={hierarchyModal.saving}
              >
                <X size={18} />
              </button>
            </div>

            {hierarchyModal.loading ? (
              <div className="history-empty">
                <Loader2 size={22} className="animate-spin" />
                Cargando jerarquia...
              </div>
            ) : (
              <div className="hierarchy-editor-scroll">
                <Paso3FasesHitosEntregables
                  data={{
                    fases: hierarchyModal.fases,
                    fechaInicioProyecto: hierarchyModal.fechaInicioProyecto,
                  }}
                  onChange={handleHierarchyTreeChange}
                  errors={hierarchyModal.errors}
                  lockExistingDates
                  protectExistingItems
                  allowEmpty
                />
              </div>
            )}

            {hierarchyModal.error && (
              <div className="review-modal-error">
                {hierarchyModal.error}
              </div>
            )}

            <div className="review-modal-actions">
              <button type="button" className="evidence-preview-secondary" onClick={handleCloseHierarchyModal} disabled={hierarchyModal.saving}>
                Cancelar
              </button>
              <button type="submit" className="hierarchy-submit" disabled={hierarchyModal.saving || hierarchyModal.loading}>
                <Pencil size={15} />
                {hierarchyModal.saving ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </form>
        </div>
      )}

      {historyModal.open && (
        <div className="review-modal-overlay" role="presentation" onClick={handleCloseHistory}>
          <div className="review-modal history-modal" onClick={(event) => event.stopPropagation()}>
            <div className="review-modal-header">
              <div>
                <span className="review-modal-kicker history">Historico documental</span>
                <h3>{historyModal.entregable?.nombre || 'Entregable'}</h3>
                <p>Versiones conservadas del documento. Solo Administrador y Gestor de Proyectos pueden ver o revertir.</p>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={handleCloseHistory}
                aria-label="Cerrar historico documental"
              >
                <X size={18} />
              </button>
            </div>

            {historyModal.loading && (
              <div className="history-empty">
                <Loader2 size={22} className="animate-spin" />
                Cargando historico...
              </div>
            )}

            {!historyModal.loading && historyModal.error && (
              <div className="review-modal-error">
                {historyModal.error}
              </div>
            )}

            {!historyModal.loading && !historyModal.error && historyModal.versions.length === 0 && (
              <div className="history-empty">
                No hay versiones registradas para este documento.
              </div>
            )}

            {!historyModal.loading && !historyModal.error && historyModal.versions.length > 0 && (
              <div className="document-history-list">
                {historyModal.versions.map((version) => {
                  const isCurrent = version.actual || version.estado === 'ACTUAL';
                  const versionDate = version.subidoEn
                    ? new Date(version.subidoEn).toLocaleString('es-CO')
                    : '--';

                  return (
                    <article key={version.id} className={`document-history-item ${isCurrent ? 'current' : ''}`}>
                      <div>
                        <div className="document-history-title">
                          <strong>Version {version.numeroVersion}</strong>
                          <span className={`document-history-state ${isCurrent ? 'current' : ''}`}>
                            {isCurrent ? 'Actual' : 'Historica'}
                          </span>
                        </div>
                        <p>{version.nombreArchivo || 'evidencia.pdf'}</p>
                        <span className="document-history-meta">
                          {versionDate} · {version.subidoPor || 'sistema'}
                        </span>
                      </div>

                      <div className="document-history-actions">
                        <button type="button" className="evidence-preview-secondary" onClick={() => handlePreviewVersion(version)}>
                          <Eye size={14} /> Ver PDF
                        </button>
                        {!isCurrent && (
                          <button type="button" className="history-revert-btn" onClick={() => handleOpenRevertModal(version)}>
                            <RotateCcw size={14} /> Revertir
                          </button>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {revertModal.open && (
        <div className="review-modal-overlay" role="presentation" onClick={handleCloseRevertModal}>
          <form className="review-modal revert-modal" onSubmit={handleConfirmRevert} onClick={(event) => event.stopPropagation()}>
            <div className="review-modal-header">
              <div>
                <span className="review-modal-kicker history">Reversion documental</span>
                <h3>Revertir a version {revertModal.version?.numeroVersion}</h3>
                <p>Esta accion cambiara el documento actual y dejara la evidencia nuevamente en revision.</p>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={handleCloseRevertModal}
                aria-label="Cerrar reversion"
                disabled={revertModal.saving}
              >
                <X size={18} />
              </button>
            </div>

            <label className="review-field">
              <span>Motivo obligatorio</span>
              <textarea
                value={revertModal.motivo}
                onChange={(event) => setRevertModal((current) => ({ ...current, motivo: event.target.value, error: '' }))}
                placeholder="Explica por que se restaura esta version."
                rows={4}
                maxLength={1000}
                disabled={revertModal.saving}
              />
            </label>

            {revertModal.error && (
              <div className="review-modal-error">
                {revertModal.error}
              </div>
            )}

            <div className="review-modal-actions">
              <button type="button" className="evidence-preview-secondary" onClick={handleCloseRevertModal} disabled={revertModal.saving}>
                Cancelar
              </button>
              <button type="submit" className="history-revert-submit" disabled={revertModal.saving}>
                <RotateCcw size={15} />
                {revertModal.saving ? 'Revirtiendo...' : 'Confirmar reversion'}
              </button>
            </div>
          </form>
        </div>
      )}

      {approvalModal.open && (
        <div className="review-modal-overlay" role="presentation" onClick={handleCloseApprovalModal}>
          <form className="review-modal approval-modal" onSubmit={handleConfirmApproveEvidence} onClick={(event) => event.stopPropagation()}>
            <div className="review-modal-header">
              <div>
                <span className="review-modal-kicker approval">Aprobacion final</span>
                <h3>Confirmar aprobacion</h3>
                <p>Esta seguro de que desea aprobar este documento?</p>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={handleCloseApprovalModal}
                aria-label="Cerrar aprobacion"
                disabled={Boolean(approvingEntregableId)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="approval-warning">
              Recuerda que esta accion no es reversible y no se va a poder cambiar el documento.
            </div>

            {approvalModal.error && (
              <div className="review-modal-error">
                {approvalModal.error}
              </div>
            )}

            <div className="review-modal-actions">
              <button type="button" className="evidence-preview-secondary" onClick={handleCloseApprovalModal} disabled={Boolean(approvingEntregableId)}>
                Cancelar
              </button>
              <button type="submit" className="approval-submit" disabled={Boolean(approvingEntregableId)}>
                <CheckCircle2 size={15} />
                {approvingEntregableId ? 'Aprobando...' : 'Si, aprobar'}
              </button>
            </div>
          </form>
        </div>
      )}

      {reviewModal.open && (
        <div className="review-modal-overlay" role="presentation" onClick={handleCloseReviewModal}>
          <form className="review-modal" onSubmit={handleRejectEvidence} onClick={(event) => event.stopPropagation()}>
            <div className="review-modal-header">
              <div>
                <span className="review-modal-kicker">Observacion del gestor</span>
                <h3>Solicitar subsanacion</h3>
                <p>Describe exactamente que debe corregir el lider antes de volver a cargar la evidencia.</p>
              </div>
              <button
                type="button"
                className="evidence-preview-close"
                onClick={handleCloseReviewModal}
                aria-label="Cerrar observacion"
                disabled={Boolean(approvingEntregableId)}
              >
                <X size={18} />
              </button>
            </div>

            <label className="review-field">
              <span>Observacion obligatoria</span>
              <textarea
                value={reviewModal.observacion}
                onChange={(event) => setReviewModal((current) => ({ ...current, observacion: event.target.value, error: '' }))}
                placeholder="Ej: El documento no incluye firmas, falta anexo técnico o la evidencia no corresponde al entregable."
                rows={5}
                maxLength={1000}
                disabled={Boolean(approvingEntregableId)}
              />
            </label>

            <div className="review-modal-count">
              {reviewModal.observacion.length}/1000 caracteres
            </div>

            {reviewModal.error && (
              <div className="review-modal-error">
                {reviewModal.error}
              </div>
            )}

            <div className="review-modal-actions">
              <button type="button" className="evidence-preview-secondary" onClick={handleCloseReviewModal} disabled={Boolean(approvingEntregableId)}>
                Cancelar
              </button>
              <button type="submit" className="review-submit" disabled={Boolean(approvingEntregableId)}>
                <MessageSquare size={15} />
                {approvingEntregableId ? 'Guardando...' : 'Guardar observacion'}
              </button>
            </div>
          </form>
        </div>
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

      {showCambiarFechaModal && (
        <ModificarFechaModal
          entregable={showCambiarFechaModal}
          proyectoId={proyectoId}
          onClose={() => setShowCambiarFechaModal(null)}
          onSaved={() => {}}
        />
      )}

      {showHistorialFechasModal && (
        <HistorialCambiosFecha
          entregable={showHistorialFechasModal}
          proyectoId={proyectoId}
          onClose={() => setShowHistorialFechasModal(null)}
        />
      )}
    </div>
  );
};

export default ProgressTreeTable;
