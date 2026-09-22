import { useState, useCallback, useRef, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  FileText, Upload, CheckCircle2, RotateCcw, Clock, AlertTriangle,
  X, LoaderCircle, Eye
} from 'lucide-react';
import advanceReportService from '../../../services/advanceReportService';
import { emitToast } from '../../../utils/feedback';
import { useAuthContext } from '../../../context/AuthContext';
import './AdvanceReportActions.css';

const ALLOWED_EXTENSIONS = ['pdf', 'pptx'];
const MAX_SIZE_MB = 20;

const AdvanceReportActions = ({ projectId }) => {
  const { hasRole } = useAuthContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [panelOpen, setPanelOpen] = useState(() => searchParams.get('openReportUpload') === 'true');

  useEffect(() => {
    if (searchParams.get('openReportUpload') === 'true') {
      const newParams = new URLSearchParams(searchParams);
      newParams.delete('openReportUpload');
      setSearchParams(newParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);
  const [uploading, setUploading] = useState(false);
  const [returning, setReturning] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [returnText, setReturnText] = useState('');
  const [showReturnForm, setShowReturnForm] = useState(false);
  const fileInputRef = useRef(null);
  const panelRef = useRef(null);

  const isDirector = hasRole('DIRECTOR_PROYECTO');
  const isGestor = hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');

  const loadStatus = useCallback(async () => {
    if (!projectId) { setLoading(false); return; }
    try {
      setLoading(true);
      const data = await advanceReportService.getStatus(projectId);
      setStatus(data);
    } catch {
      setStatus(null);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  useEffect(() => {
    if (!panelOpen) return;
    const handleClickOutside = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setPanelOpen(false);
        setShowReturnForm(false);
        setReturnText('');
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [panelOpen]);

  const validateFile = (file) => {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      emitToast({ title: 'Extension no permitida', message: `Solo se permiten: ${ALLOWED_EXTENSIONS.join(', ')}`, tone: 'error' });
      return false;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      emitToast({ title: 'Archivo demasiado grande', message: `Maximo ${MAX_SIZE_MB} MB.`, tone: 'error' });
      return false;
    }
    return true;
  };

  const handleUpload = async (file) => {
    if (!file || !projectId) return;
    if (!validateFile(file)) return;
    try {
      setUploading(true);
      await advanceReportService.uploadReport(projectId, file);
      await loadStatus();
      emitToast({ title: 'Informe cargado', message: `${file.name} subido exitosamente.`, tone: 'success' });
    } catch (err) {
      const msg = err?.response?.data?.message || 'No fue posible cargar el informe.';
      emitToast({ title: 'Error al cargar', message: msg, tone: 'error' });
    } finally {
      setUploading(false);
    }
  };

  const handleVerify = async () => {
    if (!projectId) return;
    try {
      setVerifying(true);
      await advanceReportService.verifyReport(projectId);
      await loadStatus();
      setPanelOpen(false);
      emitToast({ title: 'Informe verificado', message: 'El informe de avance fue verificado.', tone: 'success' });
    } catch (err) {
      emitToast({ title: 'Error', message: err?.response?.data?.message || 'No fue posible verificar.', tone: 'error' });
    } finally {
      setVerifying(false);
    }
  };

  const handleReturn = async () => {
    if (!projectId) return;
    try {
      setReturning(true);
      await advanceReportService.returnReport(projectId, returnText);
      await loadStatus();
      setPanelOpen(false);
      setShowReturnForm(false);
      setReturnText('');
      emitToast({ title: 'Informe devuelto', message: 'El informe fue devuelto al director.', tone: 'success' });
    } catch (err) {
      emitToast({ title: 'Error', message: err?.response?.data?.message || 'No fue posible devolver.', tone: 'error' });
    } finally {
      setReturning(false);
    }
  };

  const handleFileInput = (e) => {
    const file = e.target.files?.[0];
    if (file) handleUpload(file);
    e.target.value = '';
  };

  const handleDownload = async () => {
    if (!projectId || !status?.fileName) return;
    try {
      const blob = await advanceReportService.downloadReport(projectId);
      const url = window.URL.createObjectURL(blob);
      if (status.fileName.toLowerCase().endsWith('.pdf')) {
        window.open(url, '_blank');
      } else {
        const link = document.createElement('a');
        link.href = url;
        link.download = status.fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
      }
      setTimeout(() => window.URL.revokeObjectURL(url), 10000);
    } catch (err) {
      emitToast({ title: 'Error al descargar', message: 'No fue posible descargar el archivo.', tone: 'error' });
    }
  };

  const isUploaded = status?.isUploaded && !status?.isPending;
  const isVerified = status?.estado === 'VERIFICADO';
  const isReturned = status?.estado === 'DEVUELTO';
  const isPendingNoUpload = status?.isPending && !status?.isUploaded;
  const isOverdue = status?.isOverdue;
  const daysUntilDue = status?.daysUntilDue;
  const dueDate = status?.dueDate;
  const hasDueDate = dueDate && dueDate !== null;

  if (!status || (!isUploaded && !isVerified && !isReturned && !hasDueDate)) return null;

  const getButtonLabel = () => {
    if (isVerified) return 'Informe verificado';
    if (isReturned) return 'Informe devuelto';
    if (isUploaded) return 'Informe de avance';
    if (isOverdue) return 'Informe vencido';
    if (isPendingNoUpload && daysUntilDue != null) {
      return daysUntilDue < 0
        ? `Informe vencido (${Math.abs(daysUntilDue)}d)`
        : `Informe (${daysUntilDue}d)`;
    }
    return 'Informe de avance';
  };

  const getButtonClass = () => {
    if (isVerified) return 'ara-btn ara-btn--success';
    if (isReturned) return 'ara-btn ara-btn--warning';
    if (isUploaded) return 'ara-btn ara-btn--info';
    if (isOverdue) return 'ara-btn ara-btn--danger';
    return 'ara-btn ara-btn--pending';
  };

  const getIcon = () => {
    if (isVerified) return <CheckCircle2 size={15} />;
    if (isReturned) return <RotateCcw size={15} />;
    if (isUploaded) return <FileText size={15} />;
    if (isOverdue) return <AlertTriangle size={15} />;
    return <Clock size={15} />;
  };

  return (
    <div className="ara-wrapper" ref={panelRef}>
      <button
        type="button"
        className={getButtonClass()}
        onClick={() => setPanelOpen((prev) => !prev)}
      >
        {getIcon()}
        {getButtonLabel()}
      </button>

      {panelOpen && (
        <div className="ara-panel">
          <div className="ara-panel-header">
            <strong>Informe de avance</strong>
            <button className="ara-panel-close" onClick={() => { setPanelOpen(false); setShowReturnForm(false); }}>
              <X size={14} />
            </button>
          </div>

          <div className="ara-panel-body">
            {dueDate && (
              <div className={`ara-due ${isOverdue ? 'ara-due--overdue' : ''}`}>
                <Clock size={13} />
                {isOverdue
                  ? `Vencido hace ${Math.abs(daysUntilDue)} dia(s) — ${dueDate}`
                  : `Faltan ${daysUntilDue} dia(s) — ${dueDate}`}
              </div>
            )}

            {isUploaded && (
              <div className="ara-file-info">
                <FileText size={16} />
                <div className="ara-file-meta">
                  <strong>{status.fileName}</strong>
                  <span>{status.uploadedAt} — por {status.uploadedBy || 'desconocido'}</span>
                  {isVerified && <span className="ara-badge ara-badge--success">Verificado</span>}
                  {isReturned && <span className="ara-badge ara-badge--warning">Devuelto</span>}
                </div>
                <button 
                  className="ara-action-btn ara-action-btn--download" 
                  onClick={handleDownload}
                  title="Ver / Descargar informe"
                  style={{ marginLeft: 'auto', background: 'transparent', border: 'none', cursor: 'pointer', color: '#475569', padding: '4px', borderRadius: '4px' }}
                >
                  <Eye size={18} />
                </button>
              </div>
            )}

            {isReturned && status?.observaciones && (
              <div className="ara-return-note">
                <strong>Observaciones:</strong>
                <p>{status.observaciones}</p>
              </div>
            )}

            {isPendingNoUpload && (
              <div className="ara-upload-hint">
                <AlertTriangle size={14} />
                <span>Sin informe cargado para este periodo.</span>
              </div>
            )}
          </div>

          <div className="ara-panel-footer">
            {isDirector && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,.pptx"
                  onChange={handleFileInput}
                  style={{ display: 'none' }}
                />
                <button
                  className="ara-action-btn ara-action-btn--upload"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                >
                  {uploading ? <LoaderCircle size={13} className="ara-spin" /> : <Upload size={13} />}
                  {isUploaded ? 'Reemplazar' : 'Cargar informe'}
                </button>
              </>
            )}

            {isGestor && isUploaded && !isVerified && (
              <>
                {!showReturnForm ? (
                  <>
                    <button
                      className="ara-action-btn ara-action-btn--verify"
                      onClick={handleVerify}
                      disabled={verifying}
                    >
                      {verifying ? <LoaderCircle size={13} className="ara-spin" /> : <CheckCircle2 size={13} />}
                      Verificar
                    </button>
                    <button
                      className="ara-action-btn ara-action-btn--return"
                      onClick={() => setShowReturnForm(true)}
                    >
                      <RotateCcw size={13} />
                      Devolver
                    </button>
                  </>
                ) : (
                  <div className="ara-return-form">
                    <textarea
                      placeholder="Observaciones para el director..."
                      value={returnText}
                      onChange={(e) => setReturnText(e.target.value)}
                      rows={3}
                    />
                    <div className="ara-return-form-actions">
                      <button
                        className="ara-action-btn ara-action-btn--return"
                        onClick={handleReturn}
                        disabled={returning}
                      >
                        {returning ? <LoaderCircle size={13} className="ara-spin" /> : <RotateCcw size={13} />}
                        Confirmar devolucion
                      </button>
                      <button
                        className="ara-action-btn ara-action-btn--cancel"
                        onClick={() => { setShowReturnForm(false); setReturnText(''); }}
                      >
                        Cancelar
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdvanceReportActions;
